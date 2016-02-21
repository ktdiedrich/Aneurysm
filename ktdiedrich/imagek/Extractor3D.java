/*=========================================================================
 *
 *  Copyright (c) Karl T. Diedrich 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *=========================================================================*/

package ktdiedrich.imagek;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** Extract 3-D data starting with 2-D clustered Z-Buffer MIP data 
 * @author Karl Diedrich <ktdiedrich@gmail.com> 
 * */
public class Extractor3D
{
	public static final String SEG_NAME = "Seg";
    public static final double MEDIAN_FILTER_STD_DEV_ABOVE = 1.0;
    public static final int MIN_2D_SEED_SIZE = 30;
    public static final int MIN_3D_CLUSTER_SIZE = 10000;
    public static final short VOXEL_Z_DIFF = 2;
    public static final float SEED_HIST_THRES = 0.25f;
    public static final int SCALP_SKULL = 0;
    public static final int HOLE_FILL_ITERATIONS = 3; 
    public static final int HOLE_FILL_DIRECTIONS = 24;
    public static final int HOLE_FILL_RADIUS = 8;
    public static final boolean BUBBLE_FILL_BY_SLICE = false;
    public static final int MEDIAN_FILTER_SIZE = 2;
    public static final double CHI_SQ_SMOOTHNESS = 1.0;
    public static final boolean MEDIAN_FILTER = false;
    
    private RegionGrow3D _regionGrow3D;
    private ImageProcessor _seedProc;
    private Map<Short, Cluster> _clusters;
    private int _clusterSizeThreshold;
    private double _seedHistogramThreshold;
    private boolean _showSteps;
    private int _fillHolesTimes;
    private Message _messageWindow;
    private int _bubbleFillAlgorithm;
    private double _maxChisq;
    private short _zDiff;
    private int _seedClusterMin;
    private int _medianFilterSize;
    private double _medFilterStdDevAbove;
    private int _scalpDist;
    private String _segDir;
    private String _segBaseName;
    public Extractor3D()
    {
        _clusterSizeThreshold = MIN_3D_CLUSTER_SIZE;
        _fillHolesTimes = HOLE_FILL_ITERATIONS;
        _medFilterStdDevAbove = MEDIAN_FILTER_STD_DEV_ABOVE;
        _medianFilterSize = 0;
        _regionGrow3D = new RegionGrow3D();
        _holeFill = new HoleFill();
    }
    public void message(String m)
    {
    	if (_messageWindow != null)
    		_messageWindow.message(m);
    	else
    		System.out.println(m);
    }
    public void medianFilter(ImagePlus image)
    {
    	
    	MedianFilter medianFilter = new MedianFilter();
        medianFilter.setShowSteps(_showSteps);
        medianFilter.setMedianFilterSize(_medianFilterSize);
        medianFilter.setStdDevFactor(_medFilterStdDevAbove);
        long start = System.currentTimeMillis();
        medianFilter.medianFilter(image);
        long duration = System.currentTimeMillis() - start;
        IJ.log("Median filter: " + duration/60000 + " minutes");
    }
    /** Segment the arteries. */
    public ImagePlus segment(ImagePlus inputImage )
    {
    	long start = 0; // for timing 
    	long duration = 0; // for timing 
    	if (_medianFilterSize > 0 && _medFilterStdDevAbove > 0)
    	{
    		this.medianFilter(inputImage);
    	}
    	
    	ImageStack stack = inputImage.getStack();
        String title = inputImage.getShortTitle();
        ImagePlus mip = MIP.createShortMIP(inputImage, MIP.Z_AXIS);
        if (_showSteps)
        {
            mip.show();
            mip.updateAndDraw();
        }
        List<Edge> scalpEdges = null;
        if (_scalpDist > 0)
        {
        	Scalper knife = new Scalper();
        	knife.setInnerDist(_scalpDist);
        	knife.setShowSteps(_showSteps);
        	scalpEdges = knife.scalp(mip);
        	Scalper.scalpStack(stack, scalpEdges);
        }
        
        mip = null;
    	inputImage.updateAndDraw();
       
        ImagePlus mipz = MIPZUtil.createMIPZImage(inputImage);
        ImageProcessor mipzScalpProc = null;
        if (scalpEdges != null)
        {
        	mipzScalpProc = Scalper.scalpEdges(mipz.getProcessor(), scalpEdges);
        	
        }
        else
        {
        	mipzScalpProc = mipz.getProcessor();
        }
        ImagePlus scalpedMipZ = new ImagePlus(title+"Scalped_MIP_Z", mipzScalpProc);
    	if (_showSteps)
    	{
    		scalpedMipZ.show();
    		scalpedMipZ.updateAndDraw();
    	}
    	
    	ZBufferPolySmooth smoother = new ZBufferPolySmooth();
        smoother.setShowSteps(_showSteps);
        smoother.setMaxChisq(_maxChisq);
        smoother.setZDiff(_zDiff);
        smoother.setClusterSize(_seedClusterMin);
        
        start = System.currentTimeMillis();
        ImagePlus smooth = smoother.smooth(scalpedMipZ);
        duration  = System.currentTimeMillis() - start;
        short[][] zBufferSqr = smoother.getZSqr();
        short[][] clusteredSqr = smoother.getClusterSizes();
        
        IJ.log("Z Buffer polynomial smoothing: "+duration/60000+" minutes");
        if (_showSteps)
        {
            smooth.show();
            smooth.updateAndDraw();
        }
    	
    	ImageStack inputStack = inputImage.getStack(); 
        int height = clusteredSqr.length;
        int width = clusteredSqr[0].length;
        assert height == inputStack.getHeight();
        assert width == inputStack.getWidth();
        assert height == zBufferSqr.length;
        assert width == zBufferSqr[0].length;
        int zSize = inputStack.getSize();
        _regionGrow3D.setWidth(width);
        _regionGrow3D.setHeight(height);
        _regionGrow3D.setZSize(zSize);
        ImagePlus pcImage = _regionGrow3D.makePCmagImage(inputImage.getShortTitle());
        if (pcImage != null)
        {
        	pcImage.show();
        	pcImage.updateAndDraw();
        }
        _seedProc = new ShortProcessor(width, height);
        short[] seedPixels = (short[])_seedProc.getPixels();
        
        short[][] reconVoxels = new short[zSize][];
        _clusters = new HashMap<Short, Cluster>();
        ImageStack segStack = new ImageStack(width, height);
        short[][] inputVoxels = new short[zSize][];
        for (int i=0; i<zSize; i++)
        {
            ImageProcessor reconSliceProc = new ShortProcessor(width, height);
            segStack.addSlice("segmentation "+(i), reconSliceProc);
            // direct pixel access is faster than methods 
            inputVoxels[i] = (short[])inputStack.getProcessor(i+1).getPixels();
            reconVoxels[i] = (short[])reconSliceProc.getPixels();
        }

        boolean [][][] clustered = new boolean[zSize][height][width];
        short currentClusterNum = 0;
        Cluster currentCluster = new Cluster(currentClusterNum);
        SortedMap<Short, Integer> histogram = new TreeMap<Short, Integer>();
        long seedSum = 0;
        List<Short> seedValues = new LinkedList<Short>();
        int allHistCount = 0;
        for (int r=0; r < height; r++)
        {
            for (int c=0; c < width; c++)
            {
                short clusterSize = clusteredSqr[r][c];
                if (clusterSize > 0)
                {
                    short mipZ = zBufferSqr[r][c];
                    
                    if (mipZ > 0)
                    {
                    	// 3-D location of seed voxel
                        short mipVal = inputVoxels[mipZ-1][r*width+c];
                        seedValues.add(mipVal);
                        if (histogram.containsKey(mipVal))
                        {
                            Integer countVal = histogram.get(mipVal);
                            countVal++;
                            histogram.put(mipVal, countVal);
                        }
                        else
                        {
                            histogram.put(mipVal, 1);
                        }
                        allHistCount++;
                        seedSum += (long)mipVal;
                        seedPixels[r*width + c] = mipVal;
                    }
                }
            }
        }
        
        Set<Short> histVals = histogram.keySet();
        int cumHistCount = 0;
        
        for (Short hv: histVals)
        {
            Integer histCount = histogram.get(hv);
            cumHistCount += histCount;
            double cumFract = (double)cumHistCount / (double)allHistCount;
            if (cumFract > _seedHistogramThreshold)
            {
                _regionGrow3D.setLowerThreshold(hv);
                break;
            }
        }
        
        if (_showSteps)
        {
            IJ.log("3-D region growing threshold: "+_regionGrow3D.getLowerThreshold());
        }
        start =  System.currentTimeMillis();
        
        for (int r=0; r < height; r++)
        {
            for (int c=0; c < width; c++)
            {
                short clusterSize = clusteredSqr[r][c];
            
                if (clusterSize > 0)
                {
                    short mipZ = zBufferSqr[r][c];
                    if (mipZ > 0)
                    {
                        short mipVal = inputVoxels[mipZ-1][r*width+c];
                        
                        if (clustered[mipZ-1][r][c] == false)
                        {
                            currentClusterNum++;
                            currentCluster = new Cluster(currentClusterNum);
                            _clusters.put(currentClusterNum, currentCluster);
                        }
                        ImageProcessor reconSliceProc = segStack.getProcessor(mipZ);
                        reconSliceProc.putPixel(c, r, mipVal );
                        currentCluster.addPosition(new Position(c, r, mipZ));
                        
                        clustered[mipZ-1][r][c] = true;
                        
                        _regionGrow3D.growRegion(inputVoxels, reconVoxels, c, r, mipZ, mipVal, clustered, currentCluster);
                    }
                }
            }
        }
        duration = System.currentTimeMillis() - start;
        IJ.log("Segmentation region grow: "+duration+" ms");
        for (int i=1; i <= segStack.getSize(); i++)
        {
            segStack.getProcessor(i).resetMinAndMax();
        }
        
        BubbleFill bubbleFill = new BubbleFill();
        bubbleFill.fillBubbles(inputStack, segStack, _bubbleFillAlgorithm);
        
        for (int i=0; i< _fillHolesTimes; i++)
        {
        	start = System.currentTimeMillis();
            _holeFill.fillHoles(inputStack, segStack);
            duration = System.currentTimeMillis() - start;
            IJ.log("Fill holes: "+duration+" ms");
        }
        bubbleFill.fillBubbles(inputStack, segStack, _bubbleFillAlgorithm);
        
        ImagePlus segIm = new ImagePlus();
        long seed = Math.round(_seedHistogramThreshold*100);
        String filterS = "";
        if (_medianFilterSize > 0) filterS = "M"+_medianFilterSize;
        segIm.setStack(title+"S"+seed+filterS+SEG_NAME, segStack);
        _segDir = IJ.getDirectory("image");
        _segBaseName = segIm.getShortTitle();
        if (_segBaseName.contains(File.separator))
		{
			_segBaseName = SegmentationCMD.parseDirectoryFileName(_segBaseName)[1];
		}
        segIm.setTitle(_segBaseName);
        FileInfo segInfo = segIm.getFileInfo();
        segInfo.directory = _segDir;
        return segIm;
    }
    private HoleFill _holeFill;
    /** Removes voxels from clusters below the size threshold in the reconstructing image stack */
    protected void thresholdCluster(Cluster currentCluster, ImageStack reconStack)
    {
        if (currentCluster.getSize() < _clusterSizeThreshold)
        {
            for (Position pos: currentCluster.getPositions())
            {
                ImageProcessor proc = reconStack.getProcessor(pos.getZ());
                proc.putPixel(pos.getColumn(), pos.getRow(), 0);
            }
        }
    }
    
    public short getBackground()
    {
        return _regionGrow3D.getLowerThreshold();
    }
    public void setBackground(short background)
    {
        _regionGrow3D.setLowerThreshold(background);
    }
    public ImagePlus getSeedImage()
    {
        _seedProc.resetMinAndMax();
        return new ImagePlus("seed2Dfor3Drecon", _seedProc);
    }
    public int getClusterSizeThreshold()
    {
        return _clusterSizeThreshold;
    }
    public void setClusterSizeThreshold(int clusterSizeThreshold)
    {
        _clusterSizeThreshold = clusterSizeThreshold;
    }
    
    public double getSeedHistogramThreshold()
    {
        return _seedHistogramThreshold;
    }
    public void setSeedHistogramThreshold(double seedHistogramThreshold)
    {
        _seedHistogramThreshold = seedHistogramThreshold;
        IJ.log("Seed histogram threshold: "+_seedHistogramThreshold);
    }
    public boolean isShowSteps()
    {
        return _showSteps;
    }
    public void setShowSteps(boolean showSteps)
    {
        _showSteps = showSteps;
    }
    public int getFillHolesTimes()
    {
        return _fillHolesTimes;
    }
    public void setFillHolesTimes(int fillHolesTimes)
    {
        _fillHolesTimes = fillHolesTimes;
    }
    /** If more than or equal to this number of directions is artery fill the hole 
     * voxel as an artery voxel. */
    public void setHoleFillDirections(int thres)
   	{
    	_holeFill.setDirections(thres);
   	}
    public int getHoleFillDirections()
    {
    	return _holeFill.getDirections();
    }
   	/** How far in direction to search for artery voxels when filling holes. */
   	public void setHoleFillRadius(int n)
   	{
   		_holeFill.setRadius(n);
   	}
	public int getHoleFillRadius()
	{
		return _holeFill.getRadius();
	}
	public void setMessageWindow(Message messageWindow) 
	{
		_messageWindow = messageWindow;
		_regionGrow3D.setMessageWindow(messageWindow);
	}
	public void setXPCimage(ImagePlus image)
	{
		_regionGrow3D.setXPC(ImageProcess.getShortStackVoxels(image.getImageStack()));
	}
	public void setYPCimage(ImagePlus image)
	{
		_regionGrow3D.setYPC(ImageProcess.getShortStackVoxels(image.getImageStack()));
	}
	public void setZPCimage(ImagePlus image)
	{
		_regionGrow3D.setZPC(ImageProcess.getShortStackVoxels(image.getImageStack()));
	}
	public int getBubbleFillAlgorithm() {
		return _bubbleFillAlgorithm;
	}
	public void setBubbleFillAlgorithm(int bubbleFillAlgorithm) {
		_bubbleFillAlgorithm = bubbleFillAlgorithm;
	}
	public double getMaxChisq() {
		return _maxChisq;
	}
	public void setMaxChisq(double maxChisq) {
		_maxChisq = maxChisq;
	}
	public short getZDiff() {
		return _zDiff;
	}
	public void setZDiff(short diff) {
		_zDiff = diff;
	}
	public int getSeedClusterMin() {
		return _seedClusterMin;
	}
	public void setSeedClusterMin(int seedClusterMin) {
		_seedClusterMin = seedClusterMin;
	}
	public int getMedianFilterSize() {
		return _medianFilterSize;
	}
	public void setMedianFilterSize(int medianFilterSize) {
		_medianFilterSize = medianFilterSize;
	}
	public double getMedFilterStdDevAbove() {
		return _medFilterStdDevAbove;
	}
	public void setMedFilterStdDevAbove(double medFilterStdDevAbove) {
		_medFilterStdDevAbove = medFilterStdDevAbove;
	}
	public int getScalpDist() {
		return _scalpDist;
	}
	public void setScalpDist(int scalpDist) {
		_scalpDist = scalpDist;
	}
	public String getSegDir() {
		return _segDir;
	}
	public String getSegBaseName() {
		return _segBaseName;
	}
	
}

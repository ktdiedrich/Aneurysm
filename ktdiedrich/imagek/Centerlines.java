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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;

import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Inserts;

import ij.*;
import ij.io.FileInfo;
import ij.io.FileSaver;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** Find centerlines from 3-D image stack 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 */
public class Centerlines implements Message
{
    public static final short N_MAX = 20;
    public static final float A = 2.0F;
    public static final float B = 0.5F; 
    public static final float DFE_THRESHOLD = 3.0f;
    public static final float FIXED_COST = 0.01F;  //Prevents path costs from being zero 
    public static final int MIN_LINE_LENGTH = 10; 
    public static final float LOWER_PORTION = 0.15F;
    public static final int UP_FROM_LOWER_END = 2; 
    public static final int PATH_LEN_LIMIT = Integer.MAX_VALUE-10000;
    public static final int RETRACE = 10;
    public static final long MS_2_MIN = 60000;
    public static final long MS_2_SEC = 1000;
    
    private Message _messageWindow;
	private short _nMax;
    private float _a;
    private float _b;
    private float _dfeThreshold;
    private int _minLineLength;
    private float _xRes;
    private float _yRes;
    private float _zRes;
    private boolean _showSteps;
    private ImagePlus _segmentationImage;
    private boolean _displaySurround;
    private boolean _centerlineOverInput;
    private boolean _thickenOutput;
    private boolean _centerlinesFromAllEnds;
    private boolean _fixBadEnds;
    private boolean _measureCenterlines;
    private RecordDFMTortuosityPanel _recordPanel;
    private JFrame _recordFrame;
    private List<CenterlineGraph> _centerlineGraphs;
    private int _recenterTimes;
    private int _windowSize;
    private double _enhancementIntensityThreshold;
    private double _enhancementSizeThreshold;
    private double _dfcDfeRatioThreshold;
    private int _extendEnhancement;
    private List<Graph> _segGraphs;
    private int _width, _height, _zSize;
    private String _title;
    private double _lineDFEratio;
    private int _lowClusterThreshold;
    private FileInfo _segmentationFileInfo;
    private ShortestPaths _sp;
    private Map<GraphNode, Integer> _centerlineAnchorLower;
    
    private ImageStack _mipZcentStack;
    private String _fixLabel="";
    private String _algLabel;
    private VoxelDistance _vd;
    private short _mipAxis;
    private int _minRecenter;
    private ImagePlus _centerlinePositive;
    private double _massWeightPower;
    private int _centerlineAlgorithm;
    private String _trueCenterlineFileName;
    private boolean _dfeWeightedCOM;
    private int _tortuosityAlg;
    private ImagePlus _xPCimage;
    private ImagePlus _yPCimage;
    private ImagePlus _zPCimage;
    private double _pcWeight;
    private double _velocityDotSigma;
    private double _velocityPower;
    public Centerlines()
    {
		_enhancementIntensityThreshold = DistanceFromCenterline.THRESHOLD;
        _nMax = N_MAX;
        _a = A;
        _b = B;
        _dfeThreshold = DFE_THRESHOLD;
        _minLineLength = MIN_LINE_LENGTH;
        _showSteps = false;
        _sp = new ShortestPaths();
        _fixBadEnds=false;
        _recenterTimes = 0;
        _minRecenter = CenterOfMass.MIN_MOVES;
        //_sp.setPathLenLimit(PATH_LEN_LIMIT);
        _xRes=1; _yRes=1; _zRes=1;
        _massWeightPower = CenterOfMass.WEIGHT_POWER;
        _algLabel = new String("ALG");
    }
    
	public void setMessageWindow(Message messageWindow) 
	{
		_messageWindow = messageWindow;
	}
	public void message(String m)
	{
		if (_messageWindow != null)
			_messageWindow.message(m);
		else
			System.out.println(m);
	}
	public void clear()
	{
		if (_messageWindow != null)
			_messageWindow.clear();
	}
	public boolean isCenterlineOverInput() 
    {
		return _centerlineOverInput;
	}
	public void setCenterlineOverInput(boolean centerlineOverInput) 
	{
		_centerlineOverInput = centerlineOverInput;
	}
	
	public void recordPanel()
	{
		_recordPanel = new RecordDFMTortuosityPanel(DbConn.PROPERTIES);
        _recordPanel.setCenterlines(this);
        this.setMessageWindow(_recordPanel);
        _recordFrame = RecordDFMTortuosityPanel.makeSaveTortuosityFrame(_recordPanel);
        _recordFrame.setVisible(true);
	}
    public short sameDFEcount(GraphNode dfeNode)
    {
        short count = 1;
        short dfeValue = dfeNode.dfe;
        for (GraphNode adjNode: dfeNode.adjacents)
        {
            if (dfeValue == adjNode.dfe)
            {
                count++;
            }
        }
        return count;
    }
    /** @return */
    protected short checkSameDFE(short val, short sameDFE, int col, int row, int z, int width, short[][] dfes)
    {
        
        if (val == dfes[z][(row*width)+col] )
        {
            sameDFE++;
        }
        
        return sameDFE;
    }
    /* Sets same and maximum DFE for the neighbors of the input position */
    public short sameDFEcount(int col, int row, int z, int width, int height, int zSize, short[][] dfes)
    {
        short sameDFE = 1;
        short val = dfes[z][row*width+col];
        sameDFE = checkSameDFE(val, sameDFE, col, row+1, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row-1, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row+1, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row-1, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row+1, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row-1, z, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row+1, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row-1, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row+1, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row-1, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row+1, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row-1, z+1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row+1, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col, row-1, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row+1, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col+1, row-1, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row+1, z-1, width, dfes);
        sameDFE = checkSameDFE(val, sameDFE, col-1, row-1, z-1, width, dfes);
        return sameDFE;
    }
    public float maxNeighborMDFE(GraphNode mdfeNode)
    {
        float maxMDFE = mdfeNode.mDFE;
        for (GraphNode adjNode: mdfeNode.adjacents )
        {
            float adjValue = adjNode.mDFE;
            if (adjValue > maxMDFE)
                maxMDFE = adjValue;
        }
        return maxMDFE;
    }
    public float getA() {
		return _a;
	}
	public void setA(float a) {
		_a = a;
	}
	public float getB() {
		return _b;
	}
	public void setB(float b) {
		_b = b;
	}
    public float[][] modifyDFEs(short[][] dfes, int width, int height, int zSize)
    {
        float[][] mdfes = new float[zSize][width*height];
        // MDFEs 1 voxel in from edge so all have 26 neighbors
        for (int z=1; z<(zSize-1); z++)
        {
            for (int r=1; r<height-1; r++)
            {
                for (int c=1; c<width-1; c++)
                {
                    if (dfes[z][r*width+c] != 0)
                    {
                        short sameDFE = sameDFEcount(c, r, z, width, height, zSize, dfes);
                        mdfes[z][r*width+c] = (float)dfes[z][r*width+c] + ((float)sameDFE)/((float)_nMax);
                    }
                }
            }
        }
        return mdfes;
    }
    public void modifyDFEs(List<Graph> graphs)
    {
        for (Graph graph: graphs)
        {
            for (GraphNode node: graph.getNodes())
            {
                node.graphed = false; // reset for use in later algorithms
                short dfeValue = node.dfe;
                if (dfeValue > 0) // always true 
                {
                    short sameDFE = sameDFEcount(node);
                    // Arbitrary _nMax = 20 
                    float mdfeValue = (float)dfeValue + ( (float)sameDFE ) / ((float)_nMax);
                    node.mDFE = (mdfeValue);
                }
            }
        }
    }
    
    /** Assigns the weight value for finding the lowest cost path to each node. The weight is based 
     * on the modified Distance From Edge (mdfe). */
    public void weight(List<Graph> graphs)
    {   
        for (Graph graph: graphs)
        {
            for (GraphNode node: graph.getNodes())
            {
                float mdfeValue = node.mDFE;
                if (mdfeValue > 0) // always true 
                {
                    float maxMDFE = maxNeighborMDFE(node);  
                    float sub1 = 1-(mdfeValue/maxMDFE);
                    double sub2 = Math.pow(sub1, _b);
                    float weightValue = (_a * (float)(sub2)) + FIXED_COST;
                    node.weight = (weightValue);
                }
            }
        }
    }
    
    private final void save(ImagePlus image)
    {
    	this.save(image, "Save");
    }
    private final void save(ImagePlus image, String note)
    {
    	String st = image.getShortTitle();
    	IJ.log("Saving file directory: "+_segmentationFileInfo.directory+" short title: "+st);
    	if (st.contains("/"))
    	{
    		int i = st.indexOf("/");
    		st = st.substring(i+1);
    	}
    	if (st.contains("\\"))
    	{
    		int i = st.indexOf("\\");
    		st = st.substring(i+1);
    	}
    	String p = _segmentationFileInfo.directory+st+".zip";
    	
    	
		FileSaver fs = new FileSaver(image);
		IJ.log(note+": "+p);
		fs.saveAsZip(p);
    }
    private final void savePng(ImagePlus image)
    {
    	savePng(image, "Save");
    }
    private final void savePng(ImagePlus image, String note)
    {
    	String st = image.getShortTitle();
    	IJ.log("Saving file directory: "+_segmentationFileInfo.directory+" short title: "+st);
    	if (st.contains("/"))
    	{
    		int i = st.indexOf("/");
    		st = st.substring(i+1);
    	}
    	if (st.contains("\\"))
    	{
    		int i = st.indexOf("\\");
    		st = st.substring(i+1);
    	}
    	String p = _segmentationFileInfo.directory+st+".png";
    	
    	
		FileSaver fs = new FileSaver(image);
		IJ.log(note+": "+p);
		fs.saveAsPng(p);
    }
    public void findCenterlines(ImagePlus segmentationImage)
    {
    	_vd = new VoxelDistance(_xRes, _yRes, _zRes);
    	long start = 0, duration = 0;
    	start = System.currentTimeMillis();
    	_segmentationImage = segmentationImage;
    	_title = segmentationImage.getShortTitle();
    	
    	ImagePlus magImage = PhaseContrast.makePCmagImage(_title, _xPCimage, _yPCimage, _zPCimage, _segmentationImage);
    	if (magImage != null)
    	{
    		magImage.show();
    		magImage.updateAndDraw();
    	}
    	
    	
    	//  original file info only exists for the opened file not a generated segmentation. 
    	_segmentationFileInfo = segmentationImage.getOriginalFileInfo();
    	if (_segmentationFileInfo != null)
    	{
    		IJ.log("Input segmentation: "+_segmentationFileInfo.directory+_segmentationFileInfo.fileName);
    	}
    	ImageStack segmentationStack = segmentationImage.getImageStack();
    	ImageProcessor proc0 = segmentationStack.getProcessor(1);
    	
        _height = segmentationStack.getHeight();
        _width = segmentationStack.getWidth();
        _zSize = segmentationStack.getSize();
        
        if (proc0 instanceof ShortProcessor)
        {
        	IJ.log("Short image");
        	short[][] inputVoxels = ImageProcess.getShortStackVoxels(segmentationStack);
        	if (_showSteps)
            {
            	ImagePlus image = ImageProcess.makeImage(inputVoxels, _width, _height, _title+"InputArray");
            	this.save(image);
            }
        	_segGraphs = this.makeDfeGraphsFromSegIntensity(inputVoxels);
        	if (_showSteps)
        	{
        		ImagePlus dfeImage = Graph.makeDFEimage(_segGraphs, _width, _height, _zSize, _title+"DFEGraphs");
        		this.save(dfeImage);
        	}
        	_centerlineGraphs = this.makeCenterlines(_segGraphs, _vd);
        	if (_fixBadEnds)
        	{
            	ImagePlus brokeLoopIm = this.makeBrokenLoopsImage(_segGraphs, _centerlineGraphs, 
            			inputVoxels, _title);
            	this.save(brokeLoopIm);
            }
        	
        }
        else if (proc0 instanceof FloatProcessor)
        {
        	IJ.log("Float image");
        	float[][] inputVoxels = ImageProcess.getFloatStackVoxels(segmentationStack);
        	_segGraphs = this.makeGraphsFromWeightImage(inputVoxels);
        	if (_showSteps)
        	{
        		ImagePlus weightImage = Graph.makeWeightImage(_segGraphs, _width, _height, _zSize, 
        			_title+"WeightGraphs");
        		this.save(weightImage);
        	}
        	_centerlineGraphs = this.makeCenterlines(_segGraphs, _vd);
        }
        
        
    	AccumulateCenterlines accumCenterline = new AccumulateCenterlines(_segGraphs, _width, _height, _zSize, _title+_fixLabel,
    			_centerlinePositive, _vd);
    	accumCenterline.readTrueCentFile(_trueCenterlineFileName);
    	accumCenterline.addCenterline(_centerlineGraphs);
        
        duration = System.currentTimeMillis() - start;
        IJ.log("FindCenterlines: "+duration/MS_2_MIN+" minutes");
        IJ.log("_centerlineGRaphs size: "+_centerlineGraphs.size());
        if (_measureCenterlines)
        {
        	this.displayMeasureCenterlines(_centerlineGraphs);
        }
        
        if (_centerlinesFromAllEnds)
        {
        	// recalculate centerlines from different sources 
            start = System.currentTimeMillis();
            this.findEndCenterlines(_centerlineGraphs, accumCenterline);
            duration = System.currentTimeMillis()-start;
            IJ.log("findEndCenterlines minutes: "+duration/MS_2_MIN+"\n");
        }
    }
    private static List<Graph> onlyLargestGraph(final List<Graph> graphs)
    {
    	IJ.log("Calling onlyLargestGraph()");
    	Graph largest = null;
    	int large = 0;
    	for (Graph g: graphs)
    	{
    		int sz = g.getNodes().size();
    		if (sz > large)
    		{
    			large = sz;
    			largest = g;
    		}
    	}
    	List<Graph> list = new LinkedList<Graph>();
    	list.add(largest);
    	return list;
    }
    /** Make a set of graphs from the input intensities of the segmentation. */
    public List<Graph> makeDfeGraphsFromSegIntensity(short[][] segmentationVoxels)
    {
        List<GraphNode> dfeNodes = null;
        {
        	DistanceFromEdge dfer = new DistanceFromEdge(_xRes, _yRes, _zRes);
        	long dfeStart = System.currentTimeMillis();
            short[][] dfes = dfer.distanceFromEdge(segmentationVoxels, _width, _height);
            long dfeDuration = System.currentTimeMillis()-dfeStart;
            IJ.log("Calculate segmentation DFE: "+dfeDuration/MS_2_SEC+" seconds.");
            if (_showSteps)
            {
            	ImagePlus image = ImageProcess.makeImage(dfes, _width, _height, _title+"DFEArray");
            	this.save(image);
            }
            System.gc();
            if (_dfeThreshold > 0)
                Threshold.thresholdUnder(dfes, (short)Math.round(_dfeThreshold*VoxelDistance.DISTANCE_PRECISION));
            long s = System.currentTimeMillis();
            dfeNodes = ConnectedGraph.makeNodeListDFE(dfes, _width, _height);
            long dur = System.currentTimeMillis()-s;
            IJ.log("Make DFE nodes from DFE arrays: "+dur/MS_2_SEC+" seconds.");
        }
        System.gc();
        long s = System.currentTimeMillis();
        ConnectedGraph conGraph = new ConnectedGraph();
        List<Graph> segDfeGraphs = conGraph.makeGraphs(dfeNodes, _lowClusterThreshold);
        this.modifyDFEs(segDfeGraphs);
        if (_centerlinesFromAllEnds)
        {
        	segDfeGraphs = onlyLargestGraph(segDfeGraphs);
        }
        long dur = System.currentTimeMillis()-s;
        IJ.log("DFE graphs: "+dur/MS_2_SEC+" seconds, number of graphs: "+segDfeGraphs.size());
        
        // need PC images to do a PC centerline algorithm, change to DFE weighted COM if PC image is missing  
        if ((_centerlineAlgorithm == Inserts.DFEWTCOM_MULT_PCCROSSNORM || 
        		_centerlineAlgorithm == Inserts.VELOC_DFECOM) && 
        		(_xPCimage == null || _yPCimage == null || _zPCimage == null))
        {
        	
        	_centerlineAlgorithm = Inserts.DFE_WEIGHTED_COM;
        	_sp.setCenterlineAlgorithm(_centerlineAlgorithm);
        	IJ.log("Missing phase contrast image change to centerline algorithm ID: "+_centerlineAlgorithm);
        }
        s = System.currentTimeMillis();
        if (_centerlineAlgorithm == Inserts.DFE_CENTERLINE_ALGORITHM)
        {
        	IJ.log("DFE cost function.");
        	this.weightDfeGraphs(segDfeGraphs);
        }
        else if (_centerlineAlgorithm == Inserts.DFE_WEIGHTED_COM || 
        		_centerlineAlgorithm == Inserts.DFEWTCOM_MULT_PCCROSSNORM || 
        		_centerlineAlgorithm == Inserts.VELOC_COST ||
        		_centerlineAlgorithm == Inserts.COM_CENTERLINE_ALGORITHM)
        {
        	IJ.log("DFE weighted Center of Mass cost function");
        	CenterOfMass.findCenterOfMass(segDfeGraphs, _recenterTimes, _vd, _minRecenter, _massWeightPower, _dfeWeightedCOM );
        }
        else if (_centerlineAlgorithm == Inserts.VELOC_DFECOM)
        {
        	IJ.log("Velocity Center of Mass cost function");
        	VelocityCenterOfMass.findCenterOfMass(segDfeGraphs, _recenterTimes, _vd, _minRecenter, _massWeightPower,
        		_xPCimage, _yPCimage, _zPCimage);
        	// display weight image
        	ImagePlus velocCOMimage = Graph.makeWeightImage(segDfeGraphs, _width, _height, _zSize, _title+"VelocCOM");
        	velocCOMimage.show();
        	velocCOMimage.updateAndDraw();
        }
        dur = System.currentTimeMillis()-s;
        IJ.log("Cost function time: "+dur/MS_2_SEC+" seconds. ");
        if (_showSteps)
        {
        	ImagePlus comIm = CenterOfMass.makeCenterOfMassImage(segDfeGraphs, _width, _height, _zSize, _title+"COM");
        	this.save(comIm);
        } 
    	return segDfeGraphs;
    }
    
    /** Make a set of graphs from the input intensities of the segmentation. */
    public List<Graph> makeGraphsFromWeightImage(float[][] segmentationVoxels)
    {
        List<GraphNode> imageNodes = null;
        {
        	long s = System.currentTimeMillis();
            imageNodes = ConnectedGraph.makeNodeListWeight(segmentationVoxels, _width, _height);
            long dur = System.currentTimeMillis()-s;
            IJ.log("Make segmentation nodes from Weight arrays: "+dur/MS_2_SEC+" seconds.");
        }
        System.gc();
        long s = System.currentTimeMillis();
        ConnectedGraph conGraph = new ConnectedGraph();
        List<Graph> segWeightGraphs = conGraph.makeGraphs(imageNodes, _lowClusterThreshold);
        if (_centerlinesFromAllEnds)
        {
        	segWeightGraphs = onlyLargestGraph(segWeightGraphs);
        }
        long dur = System.currentTimeMillis()-s;
        IJ.log("Weight graphs: "+dur/MS_2_MIN+" minutes, number of graphs: "+segWeightGraphs.size());
        
        this.weightDfeGraphs(segWeightGraphs);
    	return segWeightGraphs;
    }
    
    /** Create weights based on the DFE for use as costs to Dijkstra's shortest paths algorithm. */
    public void weightDfeGraphs(List<Graph> segDfeGraphs)
    {
    	long s = System.currentTimeMillis();
    	
        long dur = System.currentTimeMillis()-s;
        IJ.log("Modify DFE graphs: "+dur/MS_2_SEC+" seconds. ");
        if (_showSteps)
        {
        	ImagePlus image = Graph.makeMDFEImage(segDfeGraphs, _width, _height, _zSize, _title+"MDFE");
    		this.save(image);
        }        
        s = System.currentTimeMillis();
        weight(segDfeGraphs);
        dur = System.currentTimeMillis()-s;
        IJ.log("Weight DFE graphs: "+dur/MS_2_SEC+" seconds. ");
    }
    
    /** Make centers using Dijkstra's shortest path algorithm using the weights as cost function.  */
    public List<CenterlineGraph> makeCenterlines(List<Graph> segDfeGraphs, VoxelDistance vd)
    {
    	IJ.log("SegDfeGraphs size: "+segDfeGraphs.size());
    	// find ends and reuse these as source nodes.
    	PhaseContrast pc = null;
    	if (_xPCimage != null && _yPCimage != null && _zPCimage != null)
    	{
    		// set centerline algorithm 
    		pc = new PhaseContrast(_xPCimage, _yPCimage, _zPCimage);
    		pc.setVoxelDistance(_vd);
    		pc.setVelocityDotSigma(_velocityDotSigma);
    		pc.setVelocityPower(_velocityPower);
    		_sp.setPhaseContrast(pc);
    		_sp.setPcWeight(_pcWeight);
    	}
    	IJ.log("MakeCenterlines() Centerline algorithm ID: "+_centerlineAlgorithm); 
        if (_centerlineAlgorithm == Inserts.DFE_CENTERLINE_ALGORITHM)
        {
        	_algLabel = "DFE";
        }
        else if (_centerlineAlgorithm == Inserts.COM_CENTERLINE_ALGORITHM)
        {
        	_algLabel = "COM";
        }
        else if (_centerlineAlgorithm == Inserts.DFE_WEIGHTED_COM)
        {
        	_algLabel = "DFEWTCOM";
        }
        else if (_centerlineAlgorithm == Inserts.DFEWTCOM_MULT_PCCROSSNORM)
        {
        	_algLabel ="DFEWTCOM_MPCNorm";
        }
        else if (_centerlineAlgorithm == Inserts.DFEWTCOM_PLUS_WEIGHTEDPCCROSSNORM)
        {
        	_algLabel ="DFECOM_PCNorm"+_pcWeight;
        	Pattern pub = Pattern.compile("\\.");
        	Matcher mat = pub.matcher(_algLabel);
        	_algLabel = mat.replaceAll("\\_");
        }
        else if (_centerlineAlgorithm == Inserts.VELOC_DFECOM)
        {
        	_algLabel = "VelocCOM";
        }
        else if (_centerlineAlgorithm == Inserts.VELOC_COST)
        {
        	_algLabel = "VelocCost";
        }
        List<Graph> shortestPaths = _sp.dijkstraLowestCostPaths(segDfeGraphs, _zSize, vd);
        
        //String pcLabel = "";
        /*
        if (pc != null)
        {
        	if (_pcWeight > 0)
        	{
        		pcLabel = PhaseContrast.PC_ALG_NAME+_pcWeight;
        	}
        	else
        	{
        		pcLabel = "M"+PhaseContrast.PC_ALG_NAME;
        	}
            Pattern pub = Pattern.compile("\\.");
        	Matcher mat = pub.matcher(pcLabel);
        	pcLabel = mat.replaceAll("\\_");
        	
        	
        }
        */
        // _sp.writeCosts(_segmentationFileInfo.directory+_title+"Costs.txt");
        // IJ.log("Wrote: "+_segmentationFileInfo.directory+_title+"Costs.txt");
        
        // IJ.log("Shortest paths graphs: "+shortestPaths.size());
        String srcName = "SRC";
        if (shortestPaths.size() > 0)
        {
        	GraphNode src = shortestPaths.get(0).getSourceNode();
        	srcName = src.col+"_"+src.row+"_"+src.z;
        }
        if (_showSteps)
        {
        	ImagePlus pathCostIm = Graph.pathCostImage(shortestPaths, _width, _height, _zSize, 
        			_title+srcName+"ShortestPaths");
        	this.save(pathCostIm);
        }
        if (_showSteps)
        {
        	ImagePlus pathLenIm = Graph.pathLengthImage(shortestPaths, _width, _height, _zSize, _title+srcName+"PathLen");
        	this.save(pathLenIm);
        }
        
        {
        	ImagePlus weightImage = Graph.makeWeightImage(segDfeGraphs, _width, _height, _zSize, _title+"CostWeight");
        	this.assign2Dweights(weightImage, _title); 
        }
        
        List<CenterlineGraph> centerlineGraphs =  backTraceCenterlines(shortestPaths);
        IJ.log("centerlineGraphs size: "+centerlineGraphs.size());
        // IJ.log("Centerline graph count: "+centerlineGraphs.size());
        if (_fixBadEnds)
        {
        	this.fixBadEnds(centerlineGraphs, _width, _height, _zSize, _title);
        	
        }
        if (_showSteps)
        {
        	Overlay overlay = new Overlay();
        	ImagePlus centerlineWeights = overlay.overlayCenterlinesOnWeights(centerlineGraphs, segDfeGraphs, 
        			_width, _height, _zSize, _title+_fixLabel+"CentCostWeight");
        	this.save(centerlineWeights);
        }
        _mipZcentStack = new ImageStack(_width, _height);
        
        
    	for (CenterlineGraph cg: centerlineGraphs)
        {
    		GraphNode sn = cg.getSourceNode();
    		Centerline cl = sn.centerline;
        	Graph g = cl.getGraph();
        	CenterlineNodeData.assignClosestBelongNodes(g.getNodes(), cg.getCenterlineNodes(), 
        			_xRes, _yRes, _zRes);
        }
        
        
    	GraphNode src0 = centerlineGraphs.get(0).getSourceNode();
    	String endName = src0.col+"_"+src0.row+"_"+src0.z;
    	
    	ImagePlus centerlineIm = Graph.makeColorImage(centerlineGraphs, _width, _height, _zSize, 
    			_title+endName+_algLabel+_fixLabel+"Cent", Integer.MAX_VALUE, false);
    	// TODO commented out saving images. 
    	// this.save(centerlineIm, "Save 1");
    	
    	ImagePlus zImMIP = MIP.createColorMIP(centerlineIm, _mipAxis);;
		_mipZcentStack.addSlice(zImMIP.getShortTitle(), zImMIP.getProcessor());
        
		{
			
        	// ImagePlus enhancedImage = this.enhanceAneurysms(centerlineGraphs, shortestPaths, EnhanceAneurysm.WRITE_FILE);
        }
		
        return centerlineGraphs;
    }
    
    public ImagePlus makeBrokenLoopsImage(List<Graph> segGraphs, List<CenterlineGraph> centerlineGraphs, 
    		short[][] segmentationVoxels, String title)
    {
    	ImageStack stack = new ImageStack(_width, _height);
    	short[][] voxels = new short[_zSize][];
    	for (int i=0; i < _zSize; i++)
    	{
    		ImageProcessor ip = new ShortProcessor(_width, _height);
    		stack.addSlice(""+i, ip);
    		voxels[i] = (short[])ip.getPixels();
    	}
    	ImagePlus image = new ImagePlus(title+"BrokeLoop", stack);
    	
    	for (Graph g: segGraphs)
    	{
    		for (GraphNode n: g.getNodes())
    		{
    			// get intensity 
    			voxels[n.z][n.row*_width+n.col] = segmentationVoxels[n.z][n.row*_width+n.col];
    		}
    	}
    	// TODO not deleting kissing point 
    	/* 
    	for (CenterlineGraph cg: centerlineGraphs)
    	{
    		for (CenterlineCycle cc: cg.getCycles())
    		{
    			List<GraphNode> narrowAdjs = cc.getNarrowNodeAdjs();
    			GraphNode narrow = narrowAdjs.get(0);
    			int narrowRad =  cc.narrowRadius();
    			IJ.log("Delete narrow: "+narrow.coordinateString()+" radius: "+narrowRad);
    			short[][][] structure = Shapes.makeSolidSphereStructure(narrowRad, (short)1);
    			
    			Morphology.deleteStructure(voxels, _width, structure, narrow.col, narrow.row, narrow.z);
	    					
	    		
    			//else
    			//{
    				//Morphology.deleteStructure(voxels, _width, structure, narrow.col, narrow.row, narrow.z);
    			//}
    			//for (GraphNode n: cc.erodeNarrow())
    			//{
    			//	voxels[n.z][n.row*_width+n.col] = 0;
    			//}
    		}
    	}
    	*/
    	return image;
    }
    private HashMap<GraphNode, Integer> _centerlineAnchorUpper;
    /** Assign 2-D weights to the bottom end of the stack so centerlines are in the middle of arteries running off the end.
     * Call before backtracing. */
    private void assign2Dweights(ImagePlus weightImage, String title)
    {
    	// anchor on top slices too 
    	_centerlineAnchorLower = new HashMap<GraphNode, Integer>();
    	_centerlineAnchorUpper = new HashMap<GraphNode, Integer>();
    	int arteryEndZlower = weightImage.getStackSize()-3;
    	int arteryEndZupper = 1; 
    	
        Clusters2D weightClusters2Dlower = new Clusters2D(weightImage.getImageStack().getProcessor(arteryEndZlower+1), arteryEndZlower);
        Clusters2D weightClusters2Dupper = new Clusters2D(weightImage.getImageStack().getProcessor(arteryEndZupper+1), arteryEndZupper);
        List<Position> lowerWeightCenterPoss = weightClusters2Dlower.getClusterMiddle();
        List<Position> upperWeightCenterPoss = weightClusters2Dupper.getClusterMiddle();
        IJ.log("Lower End weight centers: "+lowerWeightCenterPoss.size());
        IJ.log("Upper End weight centers: "+upperWeightCenterPoss.size());
        
        if (_showSteps)
        {
	        ImagePlus arteryWeightLowerEndClusters = weightClusters2Dlower.getClusterImage(title+"LowerEndWeightClusters");
	        
	        FileSaver fs = new FileSaver(arteryWeightLowerEndClusters);
	        String p = _segmentationFileInfo.directory+arteryWeightLowerEndClusters.getShortTitle()+".tif";
	        IJ.log("Save: "+p);
	        fs.saveAsTiff(p);
	        
	        ImagePlus inputWeight = weightClusters2Dlower.getInputImage(title+"LowerEndWeight");
	        fs = new FileSaver(inputWeight);
	        p = _segmentationFileInfo.directory+inputWeight.getShortTitle()+".tif";
	        IJ.log("Save: "+p);
	        fs.saveAsTiff(p);
	        
	        Overlay.overlayPositionOnFloatImage(weightImage, lowerWeightCenterPoss, 3.0f);
	        this.save(weightImage);
	        
	        // Upper 
	        ImagePlus arteryWeightUpperEndClusters = weightClusters2Dupper.getClusterImage(title+"UpperEndWeightClusters");
	        
	        fs = new FileSaver(arteryWeightUpperEndClusters);
	        p = _segmentationFileInfo.directory+arteryWeightUpperEndClusters.getShortTitle()+".tif";
	        IJ.log("Save: "+p);
	        fs.saveAsTiff(p);
	        
	        ImagePlus inputWeightUpper = weightClusters2Dupper.getInputImage(title+"UpperEndWeight");
	        fs = new FileSaver(inputWeightUpper);
	        p = _segmentationFileInfo.directory+inputWeightUpper.getShortTitle()+".tif";
	        IJ.log("Save: "+p);
	        fs.saveAsTiff(p);
	        
        }
        // assign high pathLen to centers of arteries running out of image so they are used to backtrace centerlines 
        for (Graph g: _segGraphs)
        {
        	for (GraphNode n: g.getNodes())
        	{
        		// break ties for sorting 
        		int var = 0;
        		for (Position lowEnd: lowerWeightCenterPoss)
        		{
        			if (n.col == lowEnd.getColumn() && n.row == lowEnd.getRow() && n.z == lowEnd.getZ())
        			{
        				int pl = Integer.MAX_VALUE-var;
        				n.pathLen = pl;
        				_centerlineAnchorLower.put(n, pl);
        				//IJ.log("Centerline anchor: "+n.coordinateString()+" pathLen: "+n.pathLen);
        			}
        			var++;
        		}
        		
        		for (Position upEnd: upperWeightCenterPoss)
        		{
        			if (n.col == upEnd.getColumn() && n.row == upEnd.getRow() && n.z == upEnd.getZ())
        			{
        				int pl = Integer.MAX_VALUE-var;
        				n.pathLen = pl;
        				_centerlineAnchorUpper.put(n, pl);
        				//IJ.log("Centerline anchor: "+n.coordinateString()+" pathLen: "+n.pathLen);
        			}
        			var++;
        		}
        	}
        }
    }
    /** Rests the previously calculated pathLEn for graph nodes to fix the end points of the artery 
     * centerlines running off the bottom of the image. */
    private void resetCenterlineAnchorPathLen()
    {
    	for (GraphNode n: _centerlineAnchorLower.keySet())
    	{
    		int pl = _centerlineAnchorLower.get(n);
    		n.pathLen = pl;
    	}
    	
    	for (GraphNode n: _centerlineAnchorUpper.keySet())
    	{
    		int pl = _centerlineAnchorUpper.get(n);
    		n.pathLen = pl;
    	}
    }
    
    /** Find bad centerline end pairs and draw a centerline between closest loose ends with a straight line in the segmentation between them. 
     * Adds new CenterlineGraph's to centerlineGraphs parameter. */
    private void fixBadEnds(List<CenterlineGraph> centerlineGraphs, int width, int height, int zSize, String title)
    {
        Colors colors = Colors.getColors();
        for (CenterlineGraph cg: centerlineGraphs)
        {
        	BadCenterlineEnds bad = new BadCenterlineEnds(cg.getTreeEnds());
        	
        	Map<GraphNode, GraphNode> movedEndPairs = bad.getMovedEndPairs();
        	Map<GraphNode, List<GraphNode> > endRemoved = bad.getEndRemoved();
        	for (GraphNode goalSourceEnd: movedEndPairs.keySet())
        	{
        		GraphNode backtraceFromEnd = movedEndPairs.get(goalSourceEnd);
        		// IJ.log("Moved end pair: "+goalSourceEnd.coordinateString()+" "+backtraceFromEnd.coordinateString());
        		
        		if (goalSourceEnd.centerline != null)
        		{
        			Graph graph = goalSourceEnd.centerline.getGraph();
        			if (graph != null)
        			{
        				LinkedList<GraphNode> nodes = graph.getNodes();
        				Graph pairedShortestPaths = _sp.dijkstraLowestCostPathTarget(nodes, goalSourceEnd, zSize,  backtraceFromEnd);
        				// write out shortest paths 
        				{
        					List<Graph> pspl = new LinkedList<Graph>();
        					pspl.add(pairedShortestPaths);
        		        	//ImagePlus pcIm = Graph.pathCostImage(pspl, width, height, zSize, title+goalSourceEnd.col+"_"+goalSourceEnd.row+"_"+goalSourceEnd.z+"PairedShortest");
        		        	//this.save(pcIm);
        		        }
        				
        				Centerline patchLine = Centerlines.backtrace(goalSourceEnd, backtraceFromEnd);
        				Set<GraphNode> treeEnds = cg.getTreeEnds();
        				if (patchLine != null)
        				{
        					treeEnds.remove(goalSourceEnd);
        					treeEnds.remove(backtraceFromEnd);
        					patchLine.setRgb(colors.magenta);
        					IJ.log("Connect centerline length: "+patchLine.getCenterlineNodes().size());
        					cg.addCenterline(patchLine);
        					List<GraphNode> patchNodes = patchLine.getCenterlineNodes();
        					// brake cycle
        					CenterlineCycle cycle = CenterlineCycles.findCycle(patchNodes, _width, _height, _zSize,
        							_xRes, _yRes, _zRes);
        					cg.addCycle(cycle);
        					List<GraphNode> shortCut = cycle.narrowBifurcationsPath();
        					// remove shortcut from centerline list 
        					for (GraphNode sn: shortCut)
        					{
        						sn.centerline.getCenterlineNodes().remove(sn);
        					}
        					
        					// IJ.log("Fix ends: ");
        					// Map<GraphNode, GraphNode> brokenEnds = cycle.pairConnect();
        					// connect broken graphs in centerline graph 
        			    	// CenterlineCycle.connectPairs(brokenEnds, zSize, cg);
        				}
        				else
        				{
        					// restore centerline when the new backtraced centerline retraces an existing centerline  
        					List<GraphNode> srcRemoved = endRemoved.get(goalSourceEnd);
        					List<GraphNode> backRemoved = endRemoved.get(backtraceFromEnd);
        					for (GraphNode n: srcRemoved)
        					{
        						n.isCenterline = true;
        						n.centerline.addNode(n);
        					}
        					for (GraphNode n: backRemoved)
        					{
        						n.isCenterline = true;
        						n.centerline.addNode(n);
        					}
        					
        					treeEnds.remove(goalSourceEnd);
        					treeEnds.remove(backtraceFromEnd);
        					treeEnds.add(srcRemoved.get(0));
        					treeEnds.add(backRemoved.get(0));
        				}
        			}
        			
        		}	
        	}	
        }
    }
    
    /** Find centerlines with source nodes from the ends of the first centerline. Must be called after the findCenterlines() 
     * to create _dfeGraphs. */
    public void findEndCenterlines(List<CenterlineGraph> centGraphs, AccumulateCenterlines accumCenterline)
    {
    	int countCent=1;
    	if (_segGraphs != null && _segGraphs.size() > 0)
    	{
	    	for (CenterlineGraph cg: centGraphs)
	    	{
	    		Set<GraphNode> allEnds = cg.getTreeEnds();
	    		IJ.log("All Ends: "+allEnds.size());
	    		if (allEnds != null)
	    		{
		    		for (GraphNode endSource: allEnds)
		    		{
		    			long ts = System.currentTimeMillis();
		    			countCent++;
		    			String endName = ""+endSource.col+"_"+endSource.row+"_"+endSource.z;
		    			// recalculate centerline with new source nodes 
		    			List<CenterlineGraph> newSourceCentGraphs = new LinkedList<CenterlineGraph>();
		    			
		    			List<Graph> newShortestPaths = new LinkedList<Graph>();
		    			for (Graph dfeg: _segGraphs)
		    			{
		    				LinkedList<GraphNode> nodes = dfeg.getNodes();
		    				Graph shortestPath = _sp.dijkstraLowestCostPath(nodes, endSource, _zSize);
		    				this.resetCenterlineAnchorPathLen();
		    				CenterlineGraph newCg = this.backTraceCenterline(shortestPath);
		    				
		    				List<CenterlineGraph> newCgList = new LinkedList<CenterlineGraph>();
		    				newCgList.add(newCg);
		    				if (_fixBadEnds)
		    				{
		    					this.fixBadEnds(newCgList, _width, _height, _zSize, _title+"NewSrc");
		    				}
		    				
		    				// IJ.log(endName+" new centerline length: "+newCg.getNodes().size());
		    				newShortestPaths.add(shortestPath);
		    				for (CenterlineGraph ncg: newCgList)
		    				{
		    					newSourceCentGraphs.add(ncg);
		    				}
		    			}
		    			// create image of centerlines and write
		    			int nct = centerlineNodeCount(newSourceCentGraphs);
		    			{
		    				
		    				IJ.log(endName+": "+nct);
		    				if (nct > 0)
		    				{
		    					ImagePlus centerlineIm = Graph.makeColorImage(newSourceCentGraphs, _width, _height, _zSize, 
		    						_title+endName+_fixLabel+"SrcCent", Integer.MAX_VALUE, false);
		    					ImagePlus zIm = MIP.createColorMIP(centerlineIm, _mipAxis); // TODO select axis 
		    					_mipZcentStack.addSlice(endName, zIm.getProcessor());
		    					if (_showSteps)
		    					{
		    						this.save(centerlineIm, "Save "+countCent);
		    					}
		    				}
		    			}
		    			if (_showSteps)
		    			{
		    				ImagePlus pathCostIm = Graph.pathCostImage(newShortestPaths, _width, _height, _zSize, 
		    					_title+endName+"NewSrcShortestPaths");
		    				this.save(pathCostIm);
		    			
		    				ImagePlus pathLenIm = Graph.pathLengthImage(newShortestPaths, _width, _height, _zSize, _title+endName+"NewPathLen");
		    				this.save(pathLenIm);
		    			}
		    			// write all newSourceCentGraphs on top of each other
		    			if (nct > 0)
		    			{
		    				accumCenterline.addCenterline(newSourceCentGraphs);
		    			}
			    		long td = System.currentTimeMillis()-ts;
			    		IJ.log(countCent+" One good tree end time minutes: "+td/MS_2_MIN);
		    		}
	    		}
	    		// if (allEnds != null) IJ.log("Total different centerlines for graph: "+allEnds.size());
	    	}
    	}
    	
    	ImagePlus centZmip = new ImagePlus(_title+_fixLabel+"CentMip", _mipZcentStack);
    	this.save(centZmip);
    	StringBuffer centHistFname = new StringBuffer(_segmentationFileInfo.directory);
    	centHistFname.append(_title);
    	centHistFname.append(_fixLabel);
    	
    	accumCenterline.setFileBaseName(centHistFname.toString());
    	centHistFname.append("CentStabilityHist.txt");
    	
    	//StringBuffer bifHistFname = new StringBuffer(_segmentationFileInfo.directory);
    	//bifHistFname.append(_title);
    	//bifHistFname.append(_fixLabel);
    	//bifHistFname.append("BifStabilityHist.txt");
    	
    	double centStability = accumCenterline.saveCentHistogram(centHistFname.toString());
    	
    	// double bifStability = accumCenterline.saveBifHistogram(bifHistFname.toString());
    	IJ.log("Stability centerline: "+centStability); // +", Bifurcations: "+bifStability);
    	ImagePlus accuracyIm = accumCenterline.getAccuracyIm();
    	if (accuracyIm != null)
    	{
    		this.save(accuracyIm);
    	}
    	
    	//ImagePlus accumBifIm = accumCenterline.getBifImage();
    	//this.save(accumBifIm);
    	
    	ImagePlus accummCentIm = accumCenterline.getCentImage();
    	this.save(accummCentIm);
    	ImagePlus accumCentMipZim = MIP.createShortMIP(accummCentIm, MIP.Z_AXIS);
    	this.savePng(accumCentMipZim);	
    }
    public static int centerlineNodeCount(List<CenterlineGraph> cgs)
    {
    	int c = 0;
    	for (CenterlineGraph cg: cgs)
    	{
    		c += cg.getCenterlineNodes().size();
    	}
    	return c;
    }
    /** Makes and displays the centerline image, weight image and record panel for marking ends
     * of the Distance Factor Metric tortuosity measurement if turned on. 
     * Call after findCenterlines */
    public void displayMeasureCenterlines(List<CenterlineGraph> centerlineGraphs)
    {
    	Overlay overlay = new Overlay();
        
        if (_segmentationImage != null && _recordPanel != null)
        {
        	ImagePlus colorCenterlineArtery = overlay.overlayColorCenterlines(_segmentationImage, centerlineGraphs, 
        		_title+"Center");
        	ImageStack stack = colorCenterlineArtery.getImageStack();
        	//ContrastEnhancer ce = new ContrastEnhancer();
        	for (int i=1; i <= stack.getSize(); i++)
        	{
        		ImageProcessor ip = stack.getProcessor(i);
        		//ce.equalize(ip);
        	}
        	colorCenterlineArtery.show();
        	colorCenterlineArtery.updateAndDraw();
        	Point2PointDFM p2pdfm = new Point2PointDFM(colorCenterlineArtery, centerlineGraphs, _xRes, _yRes, _zRes);
        	p2pdfm.setTortAlg(this._tortuosityAlg);
            Point2PointDFMListener p2pdfmLis = new Point2PointDFMListener(colorCenterlineArtery, centerlineGraphs, p2pdfm);
            p2pdfm.setMessageWindow(_recordPanel);
            p2pdfmLis.setMessageWindow(_recordPanel);
            
            _recordPanel.setPoint2PointDFM(p2pdfm);
        }
    }
    
    /** Makes and displays a image enhanced for aneurysms in a separate thread. Call after find centerlines. */
    public ImagePlus enhanceAneurysms(List<CenterlineGraph> centerlineGraphs, List<Graph> shortestPaths, short displayType)
    {
    	if (centerlineGraphs != null && centerlineGraphs.size() > 0 && shortestPaths != null && shortestPaths.size()>0)
    	{
    		CenterlineGraph g0 = centerlineGraphs.get(0);
    		GraphNode src = g0.getSourceNode();
    		String title = _title+src.col+"_"+src.row+"_"+src.z;
    		EnhanceAneurysm enAn = new EnhanceAneurysm(centerlineGraphs, _segGraphs, title,
    				shortestPaths, _xRes, _yRes, _zRes, _width, _height, _zSize, _windowSize);
    		enAn.setEnhancementIntensityThreshold(_enhancementIntensityThreshold);
    		enAn.setDfcDfeRatioThreshold(_dfcDfeRatioThreshold);
    		enAn.setEnhancementSizeThreshold(_enhancementSizeThreshold);
    		enAn.setExtendEnhancement(_extendEnhancement);
    		enAn.setDirectory(_segmentationFileInfo.directory);
    		enAn.setDisplayType(displayType);
    		// TODO thread 
    		// enAn.start();
    		return enAn.enhance();
    	}
    	return null;
    }
    
    public List<CenterlineGraph> backTraceCenterlines(List<Graph> shortestPathsGraphs)
    {
    	
        List<CenterlineGraph> centerGraphs = new LinkedList<CenterlineGraph>();
        for (Graph g: shortestPathsGraphs)
        {
        	CenterlineGraph cents = backTraceCenterline(g);
        	centerGraphs.add(cents);
        }
        return centerGraphs;
    }
    
    /** Backtrace centerline in graph from all points starting at the farthest out path. */
    public CenterlineGraph backTraceCenterline(Graph shortestPath)
    {
    	List<GraphNode> shortestPathNodes = shortestPath.getNodes();
        // sort longest to shortest path lengths 
        Collections.sort(shortestPathNodes, new PathLenComparator());
       
        return backTraceCenterline(shortestPath, shortestPathNodes, shortestPath.getSourceNode());
    }
    /** Backtrace centerlines to the source node of the graph from a list of start points. */
    public CenterlineGraph backTraceCenterline(Graph shortestPath, List<GraphNode> backtraceStarts, GraphNode source)
    {
    	IJ.log("Backtracing graph from source node: "+source.coordinateString()+" backtrace starts size: "+backtraceStarts.size()+ 
    			" first backtrace: "+backtraceStarts.get(0).coordinateString());
    	
    	// find the far ends for use as second level source nodes 
    	CenterlineGraph centGraph = new CenterlineGraph();
        List<Centerline> centerlines = new LinkedList<Centerline>();
        // add source node to one centerline 
        
        Centerline centerline = new Centerline();
        centerline.setGraph(shortestPath);
        source.graphed = false;
        source.centerline = centerline;
        centerlines.add(centerline);
        centGraph.setSourceNode(source);
        List<List<GraphNode> > allSurroundLines = new LinkedList<List<GraphNode> >();
        LinkedList<GraphNode> line = new LinkedList<GraphNode>();
        //line.add(source);
        // TODO only measure line length L once, repeated in L and AccuracyLineMeasure 
        for (GraphNode backtraceNode: backtraceStarts)
        {
            boolean end = false;
            if (backtraceNode.graphed == false)
            {
            	end = true;
            }
            searchPath(source, backtraceNode, line);
        	
            int lineSize = line.size();
            if (lineSize >= _minLineLength)
            {
            	// find centerline tree ends 
            	if (end) 
            	{
            		centGraph.addTreeEnd(backtraceNode);
            	}
            	
            	centerline = new Centerline();
            	centerline.setCenterlineNodes(line);
            	centerline.setGraph(shortestPath);
            	
            	//if (backtraceNode.pathLen < PATH_LEN_LIMIT && lineCount == 0)
            	//{
            	//	IJ.log( "First backtrace start: "+backtraceNode.coordinateString() );
            	//	centerline.setRgb(colors.firstLine);
            	//	lineCount++;
            	//}
            	
            	centerlines.add(centerline);
            	GraphNode lineBegin = line.get(0);
            	GraphNode lineEnd = line.get(lineSize-1);
            	//IJ.log("pathLen: "+lineBegin.pathLen+" pathCost: "+lineBegin.pathCost+" "+lineBegin.coordinateString()+" -> "+
            	//		" pathLen: "+lineEnd.pathLen+" pathCost: "+lineEnd.pathCost+" "+lineEnd.coordinateString()+" source node: "+source.coordinateString());
            	if (lineEnd != source)
            	{
            		// mark bifurcation
            		centerline.setBifurcationNode(lineEnd);
            	}
				
            }
            else
            {
            	if (line.size() > 0)
            		allSurroundLines.add(line);
            }
            line = new LinkedList<GraphNode>();
        }
        
        for (List<GraphNode> surLine: allSurroundLines)
        {
        	GraphNode end = surLine.get(surLine.size()-1);
        	GraphNode pred = end.predecessor;
        	if (pred != null)
        	{
        		if (pred.centerline != null)
        		{
        			Centerline cent = pred.centerline;
        			cent.addSurroundNodes(surLine);
        		}
        	}
        }
        IJ.log("Centerlines size: "+centerlines.size());
        centGraph.setCenterlines(centerlines);
        return centGraph;
    }
    public void searchPath(GraphNode source, GraphNode node, List<GraphNode> line)
    {
        if (!node.graphed && !node.equals(source))
        {
        	/* 
        	int colDis = Math.abs(node.col-node.predecessor.col);
        	int rowDis = Math.abs(node.row-node.predecessor.row);
        	int zDis = Math.abs(node.z-node.predecessor.z);
        	if (colDis > 1 || rowDis > 1 || zDis > 1)
        	{
        		IJ.log(node.coordinateString()+" predecessor not neighbors: "+node.predecessor.coordinateString());
        	}
        	*/
            node.graphed = true;
            line.add(node);
            if (node.predecessor != null)
            {
                searchPath(source, node.predecessor, line);
            }
        }
         
        else if (node.equals(source))
        {
        	source.graphed = true;
        	line.add(source);
        	if (line.size() < _minLineLength)
        	{
        		source.centerline.addSurroundNodes(line);
        	}
        }
        
    }
    /** Backtrace a centerline from the looseEnd back to the source. Returns null when the new centerline backtraces over an existing centerline.
     * In this case the old centerline should be restored. */
    public static Centerline backtrace(GraphNode source, GraphNode looseEnd)
    {
    	Centerline centerline = new Centerline();
    	centerline.setGraph(source.centerline.getGraph());
    	centerline.addNode(looseEnd);
    	GraphNode next = looseEnd.predecessor;
    	int centCount = 0;
    	while (next != null && next != source)
    	{
    		if (next.isCenterline == true)
    		{
    			centCount++;
    			if (centCount > RETRACE)
    				return null;
    		}
    		
    		centerline.addNode(next);
    		next = next.predecessor;
    	}
    	centerline.addNode(source);
    	return centerline;
    }
    public float getDfeThreshold()
    {
        return _dfeThreshold;
    }
    public void setDfeThreshold(float dfeThreshold)
    {
        _dfeThreshold = dfeThreshold;
    }
    public int getMinLineLength()
    {
        return _minLineLength;
    }
    public void setMinLineLength(int minLineLength)
    {
        _minLineLength = minLineLength;
    }
	public float getXRes() {
		return _xRes;
	}
	public boolean isThickenOutput() {
		return _thickenOutput;
	}
	public void setThickenOutput(boolean thickenOutput) {
		_thickenOutput = thickenOutput;
	}
	public void setXRes(float res) {
		_xRes = res;
	}
	public float getYRes() {
		return _yRes;
	}
	public void setYRes(float res) {
		_yRes = res;
	}
	public float getZRes() {
		return _zRes;
	}
	public void setZRes(float res) {
		_zRes = res;
	}
	public boolean isShowSteps() {
		return _showSteps;
	}
	public void setShowSteps(boolean showSteps) {
		_showSteps = showSteps;
	}
	/** Display the artery surrounding the centerline */
	public void setDisplaySurround(boolean displaySurround) {
		_displaySurround = displaySurround;
	}
	/** True displays the artery surrounding the centerline. */
	public boolean isDisplaySurround() {
		return _displaySurround;
	}
	public ImagePlus getImage() {
		return _segmentationImage;
	}
	public List<CenterlineGraph> getCenterlineGraphs() {
		return _centerlineGraphs;
	}
	public int getRecenterTimes() {
		return _recenterTimes;
	}
	public void setRecenterTimes(int recenterTimes) {
		_recenterTimes = recenterTimes;
	}
	public int getWindowSize() {
		return _windowSize;
	}
	public void setWindowSize(int windowSize) {
		_windowSize = windowSize;
	}
	public double getEnhancementIntensityThreshold() {
		return _enhancementIntensityThreshold;
	}
	public void setEnhancementIntensityThreshold(double enhancementThreshold) {
		_enhancementIntensityThreshold = enhancementThreshold;
	}
	public double getEnhancementSizeThreshold() {
		return _enhancementSizeThreshold;
	}
	public void setEnhancementSizeThreshold(double enhancementSizeThreshold) {
		_enhancementSizeThreshold = enhancementSizeThreshold;
	}
	public int getExtendEnhancement() {
		return _extendEnhancement;
	}
	public void setExtendEnhancement(int extendEnhancement) {
		_extendEnhancement = extendEnhancement;
	}
	public List<Graph> getImageGraphs() {
		return _segGraphs;
	}
	public double getDfcDfeRatioThreshold() {
		return _dfcDfeRatioThreshold;
	}
	public void setDfcDfeRatioThreshold(double dfcDfeRatioThreshold) {
		_dfcDfeRatioThreshold = dfcDfeRatioThreshold;
	}
	public double getLineDFEratio() {
		return _lineDFEratio;
	}
	public void setLineDFEratio(double lineDFEratio) {
		_lineDFEratio = lineDFEratio;
	}
	public int getLowClusterThreshold() {
		return _lowClusterThreshold;
	}
	public void setLowClusterThreshold(int lowClusterThreshold) {
		_lowClusterThreshold = lowClusterThreshold;
	}

	public boolean isCenterlinesFromAllEnds() {
		return _centerlinesFromAllEnds;
	}

	public void setCenterlinesFromAllEnds(boolean centerlinesFromAllEnds) {
		_centerlinesFromAllEnds = centerlinesFromAllEnds;
		IJ.log("Centerlines from all ends: "+_centerlinesFromAllEnds);
	}

	public boolean isFixBadEnds() {
		return _fixBadEnds;
	}

	public void setFixBadEnds(boolean fixBadEnds) {
		_fixBadEnds = fixBadEnds;
		if (_fixBadEnds == false) _fixLabel = "";
		else _fixLabel = "FixedEnds";
	}

	public boolean isMeasureCenterlines() {
		return _measureCenterlines;
	}

	public void setMeasureCenterlines(boolean measureCenterlines) {
		_measureCenterlines = measureCenterlines;
	}

	public short getMipAxis() {
		return _mipAxis;
	}

	public void setMipAxis(short mipAxis) {
		_mipAxis = mipAxis;
	}

	public int getMinRecenter() {
		return _minRecenter;
	}

	public void setMinRecenter(int minRecenter) {
		_minRecenter = minRecenter;
	}

	public ImagePlus getCenterlinePositive() {
		return _centerlinePositive;
	}

	public void setCenterlinePositive(ImagePlus centerlinePositive) {
		_centerlinePositive = centerlinePositive;
	}

	public double getMassWeightPower() {
		return _massWeightPower;
	}

	public void setMassWeightPower(double massWeightPower) {
		_massWeightPower = massWeightPower;
	}

	public void setCenterlineAlgorithm(int alg)
	{
		_centerlineAlgorithm = alg;
		_sp.setCenterlineAlgorithm(_centerlineAlgorithm);
	}
	public int getCenterlineAlgorithm() {
		return _centerlineAlgorithm;
	}

	public String getTrueCenterlineFileName() {
		return _trueCenterlineFileName;
	}

	public void setTrueCenterlineFileName(String trueCenterlineFileName) {
		_trueCenterlineFileName = trueCenterlineFileName;
	}

	public boolean isDfeWeightedCOM() {
		return _dfeWeightedCOM;
	}

	public void setDfeWeightedCOM(boolean dfeWeightedCOM) {
		_dfeWeightedCOM = dfeWeightedCOM;
	}

	public int getTortuosityAlg() {
		return _tortuosityAlg;
	}

	public void setTortuosityAlg(int tortuosityAlg) {
		_tortuosityAlg = tortuosityAlg;
	}

	public ImagePlus getXPCimage() {
		return _xPCimage;
	}

	public void setXPCimage(ImagePlus cimage) {
		_xPCimage = cimage;
	}

	public ImagePlus getYPCimage() {
		return _yPCimage;
	}

	public void setYPCimage(ImagePlus cimage) {
		_yPCimage = cimage;
		
	}

	public ImagePlus getZPCimage() {
		return _zPCimage;
	}

	public void setZPCimage(ImagePlus cimage) {
		_zPCimage = cimage;
		
	}

	public double getPcWeight() {
		return _pcWeight;
	}

	public void setPcWeight(double pcWeight) {
		_pcWeight = pcWeight;
	}

	public double getVelocityDotSigma() {
		return _velocityDotSigma;
	}

	public void setVelocityDotSigma(double velocityDotSigma) {
		_velocityDotSigma = velocityDotSigma;
	}

	public double getVelocityPower() {
		return _velocityPower;
	}

	public void setVelocityPower(double velocityPower) {
		_velocityPower = velocityPower;
	}
	
}


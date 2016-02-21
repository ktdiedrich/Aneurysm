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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** 
 * Extract voxels from image stack by threshold and cluster size
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 *
 */
public class ThresholdCluster
{
	private boolean _showSteps;
	private int _clusterSizeThreshold;
	private Map<Short, Cluster> _clusters;
	private RegionGrow3D _regionGrow3D;
    
    public static final int CLUSTER_SIZE_THRESHOLD = 100;
    public static final short LOWER_GROWING_THRESHOLD = 1100;
    public ThresholdCluster()
    {
        _clusterSizeThreshold = CLUSTER_SIZE_THRESHOLD;
        _regionGrow3D = new RegionGrow3D();
        _regionGrow3D.setLowerThreshold(LOWER_GROWING_THRESHOLD);
    }
    public ImagePlus thresholdClusterStack(ImageStack inputStack, short seedLower, short seedUpper)
    {
        
        int zSize = inputStack.getSize();
        int height = inputStack.getHeight();
        int width = inputStack.getWidth();
        ImageStack seedStack = new ImageStack(width, height);
        boolean [][][] clustered = new boolean[zSize][height][width];
        short currentClusterNum = 0;
        Cluster currentCluster = new Cluster(currentClusterNum);
        _clusters = new HashMap<Short, Cluster>();
        short[][] inputStackPixels = new short[height*width][];
        short[][] seedStackPixels = new short[height*width][];
        ImageStack reconStack = new ImageStack(width, height);
        //ImageProcessor[] reconSliceProcs = new ImageProcessor[zSize];
        short[][] reconVoxels = new short[inputStack.getSize()][];
        for (int z=1; z<=inputStack.getSize(); z++)
        {
            ImageProcessor reconSliceProc = new ShortProcessor(width, height);
            reconStack.addSlice(""+z, reconSliceProc);
            //reconSliceProcs[z-1] = reconSliceProc;
            reconVoxels[z-1] = (short[])reconSliceProc.getPixels();
            ImageProcessor inputProc = inputStack.getProcessor(z);
            inputStackPixels[z-1] = (short[])inputProc.getPixels();
            ImageProcessor seedProc = inputProc.duplicate();
            seedStack.addSlice("SeedStack"+z, seedProc);
            seedStackPixels[z-1] = (short[])seedProc.getPixels();
            for (int r=0; r < height; r++)
            {
                for (int c=0; c < width; c++)
                {
                    if (seedStackPixels[z-1][r*width+c] < seedLower || seedStackPixels[z-1][r*width+c] > seedUpper)
                    {
                        seedStackPixels[z-1][r*width+c] = 0;
                    }
                }
            }
        }
        if (_showSteps)
        {
            for (int i=1; i <= zSize; i++)
            {
                seedStack.getProcessor(i).resetMinAndMax();
            }
            ImagePlus seedIm = new ImagePlus();
            seedIm.setStack("SeedStack", seedStack);
            seedIm.show();
            seedIm.updateAndDraw();
        }
        
        for (int z=1; z<=inputStack.getSize(); z++)
        { 
            ImageProcessor reconSliceProc = reconStack.getProcessor(z);
            for (int r=0; r < height; r++)
            {
                for (int c=0; c < width; c++)
                {
                    short seedInt = seedStackPixels[z-1][r*width+c]; 
                    if ( seedInt > 0)
                    {
                        if (clustered[z-1][r][c] == false)
                        {
                            clustered[z-1][r][c] = true;
                            currentClusterNum++;
                            this.thresholdCluster(currentCluster, reconStack);
                            currentCluster = new Cluster(currentClusterNum);
                            _clusters.put(currentClusterNum, currentCluster);
                        }
                        
                        reconSliceProc.putPixel(c, r, seedInt);
                        currentCluster.addPosition(new Position(c, r, z));
                        
                        _regionGrow3D.growRegion(inputStackPixels, reconVoxels, c, r, z, seedInt, clustered, currentCluster);
                    }
                }
            }
        }
       
        ImagePlus reconStackIm = new ImagePlus();
        reconStackIm.setStack("RegionGrown", reconStack);
        return reconStackIm;
    }
    
    /** Removes voxels from clusters below the size threshold in the reconstructing image stack */
    protected void thresholdCluster(Cluster currentCluster, ImageStack reconStack)
    {
        // int clusSize = currentCluster.getMembers();
        // System.out.println("Cluster: "+currentCluster.getNumber()+" Size: "+clusSize+"");
        if (currentCluster.getSize() < _clusterSizeThreshold)
        {
            for (Position pos: currentCluster.getPositions())
            {
                ImageProcessor proc = reconStack.getProcessor(pos.getZ());
                proc.putPixel(pos.getColumn(), pos.getRow(), 0);
            }
        }
    }
    public int getClusterSizeThreshold()
    {
        return _clusterSizeThreshold;
    }
    public void setClusterSizeThreshold(int clusterSizeThreshold)
    {
        _clusterSizeThreshold = clusterSizeThreshold;
    }
    public short get3Dlower()
    {
        return _regionGrow3D.getLowerThreshold();
    }
    public void set3Dlower(short dlower)
    {
        _regionGrow3D.setLowerThreshold(dlower);
    }
    public void set3Dupper(short dUpper)
    {
        _regionGrow3D.setUpperThreshold(dUpper);
    }
    public short get3Dupper()
    {
        return _regionGrow3D.getUpperThreshold();
    }
    public boolean isShowSteps()
    {
        return _showSteps;
    }
    public void setShowSteps(boolean showSteps)
    {
        _showSteps = showSteps;
    }
}

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

import ij.IJ;
import ij.ImagePlus;

import java.util.LinkedList;
import java.util.Queue;

/* Grow 3-D regions
 * @author Karl Diedrich <ktdiedrich@gmail.com> */
public class RegionGrow3D
{
    private Queue<Position> _checkPos;
    private short _lowerThreshold;
    private short _upperThreshold;
    private short _lowPixel;
    private short _highPixel;
    private Message _messageWindow;
    private short[][] _xPC;
    private short[][] _yPC;
    private short[][] _zPC;
    private int _width, _height, _zSize;
    public RegionGrow3D()
    {
        _lowPixel = Short.MAX_VALUE;
        _highPixel = 0;
        _lowerThreshold = 0;
        _upperThreshold = Short.MAX_VALUE;
    }
    public void message(String m)
    {
    	if (_messageWindow != null)
    	{
    		_messageWindow.message(m);
    	}
    }
    public short getLowerThreshold()
    {
        return _lowerThreshold;
    }
    public void setLowerThreshold(short lowerThreshold)
    {
        _lowerThreshold = lowerThreshold;
    }
    /** Grow the region from the point at col, row, z 
     * @param inputVoxels The 3-D voxels of the input image in a 2-D row major order array. 
     * @param segmentationVoxels The growing segmentation. */
    public void growRegion(short[][] inputVoxels, short[][] segmentationVoxels, int col, int row, int z, 
            short pixelInt, boolean[][][] clustered, Cluster currentCluster)
    {   
        if (pixelInt < _lowPixel)
        {
            _lowPixel = pixelInt;
        }
        if (pixelInt > _highPixel)
        {
            _highPixel = pixelInt;
        }
        _checkPos = new LinkedList<Position>();
        Position.addNeighbors(_checkPos, col, row, z);
        
        while (_checkPos.isEmpty() == false)
        {
           Position nextPos = _checkPos.remove();
           grow(inputVoxels, segmentationVoxels, nextPos, pixelInt, clustered, currentCluster);
        }
        
    }
    /** Called iteratively to grow regions from seed. */
    protected void grow(short[][] inputVoxels, short[][] segmentationVoxels, Position pos, short basePixelInt,
            boolean[][][] clustered, Cluster currentCluster)
    {
    	int z = pos.getZ();
        int col = pos.getColumn();
        int row = pos.getRow();
        int zSize = clustered.length;
        int width = clustered[0][0].length;
        int height = clustered[0].length;
        
        if (z>0 && col >=0 && row>=0 && z <= zSize &&  row < height && col < width)
        {
        	//System.out.println("z-1: "+(z-1)+" row: "+row+" col:"+col);
        	if (clustered[(z-1)][row][col] == true)
        		return;
        	if (basePixelInt < _lowPixel)
        	{
        		_lowPixel = basePixelInt;
        	}
        	if (basePixelInt > _highPixel)
        	{
        		_highPixel = basePixelInt;
        	}
        	int rc = row*width + col; 
            int reconPix = segmentationVoxels[z-1][rc];
            if (reconPix == 0)
            {
                short pixelInt = inputVoxels[z-1][rc];
                // TODO add PC magnitude threshold 
                if (pixelInt > _lowerThreshold && pixelInt < _upperThreshold)
                {
                	int zm1 = z-1;
                    segmentationVoxels[zm1][rc] = pixelInt;
                    clustered[zm1][row][col] = true;
                    currentCluster.addPosition(new Position(col, row, z));
                    Position.addNeighbors(_checkPos, col, row, z);
                }
            }
        }
    }
    public short getLowPixel()
    {
        return _lowPixel;
    }
    public short getHighPixel()
    {
        return _highPixel;
    }
    public short getUpperThreshold()
    {
        return _upperThreshold;
    }
    public void setUpperThreshold(short upperThreshold)
    {
        _upperThreshold = upperThreshold;
    }
	
	public void setMessageWindow(Message messageWindow) 
	{
		_messageWindow = messageWindow;
	}
	public short[][] getXPC() {
		return _xPC;
	}
	public void setXPC(short[][] xpc) {
		_xPC = xpc;
	}
	public short[][] getYPC() {
		return _yPC;
	}
	public void setYPC(short[][] ypc) {
		_yPC = ypc;
	}
	public short[][] getZPC() {
		return _zPC;
	}
	public void setZPC(short[][] zpc) {
		_zPC = zpc;
	}
	
	public int getWidth() {
		return _width;
	}
	public void setWidth(int width) {
		_width = width;
	}
	public int getHeight() {
		return _height;
	}
	public void setHeight(int height) {
		_height = height;
	}
	public int getZSize() {
		return _zSize;
	}
	public void setZSize(int size) {
		_zSize = size;
	}
	public ImagePlus makePCmagImage(String baseTitle)
	{
		return PhaseContrast.makePCmagImage(baseTitle, _xPC, _yPC, _zPC, _width, _height, _zSize);
	}
}

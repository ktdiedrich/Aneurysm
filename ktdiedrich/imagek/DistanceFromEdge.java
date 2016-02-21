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

import java.util.List;
import java.util.Queue;

import ij.*;
import ij.process.*;

/* Distance from edge calculation
 * @author Karl T. Diedrich <ktdiedrich@gmail.com> */
public class DistanceFromEdge
{
	public static final int LOWER_3D_CUTOFF = 6;
    private boolean _showSteps;
    private short _col, _row, _z;
    private short _colRow, _colZ, _rowZ;
    private short _colRowZ;
    private int _lower3Dcutoff;
    
    /** Sets x, y and z resolutions */
    public DistanceFromEdge(float xRes, float yRes, float zRes)
    {
    	VoxelDistance voxDis = new VoxelDistance(xRes, yRes, zRes);
        _col = voxDis.getCol();
        _row = voxDis.getRow();
        _z = voxDis.getZ();
        
        _colRow = voxDis.getColRow();
        _colZ = voxDis.getColZ();
        _rowZ = voxDis.getRowZ();
        
        _colRowZ = voxDis.getColRowZ();
        _lower3Dcutoff = LOWER_3D_CUTOFF;
    }
    
    
    public  short[][] distanceFromEdge(ImagePlus image)
    {
        return distanceFromEdge(image.getImageStack());
    }
    public  short[][] distanceFromEdge(ImageStack stack)
    {
        return distanceFromEdge(ImageProcess.getShortStackVoxels(stack), stack.getWidth(), stack.getHeight());
    }
    public  short[][] distanceFromEdge(short[][] voxels, int width, int height)
    {
    	
    	return this.distanceFromEdge26ray(voxels, width, height);
    	// return this.distanceFromEdgeCity(voxels, width, height);
    }
    public short[][] distanceFromEdgeCity(short[][] voxels, int width, int height)
    {
    	int zSize = voxels.length;
    	boolean[][] visit = new boolean[zSize][width*height];
    	int low = zSize - _lower3Dcutoff;
        short[][] dfes = new short[zSize][width*height];
        // TODO 
        int total = width*height*zSize;
        int visited = 0;
        for (int x=0; x < width; x++)
        {
        	for (int y=0; y < height; y++)
        	{
        		for (int z=0; z < zSize; z++)
        		{
        			short val = voxels[z][y*width+x];
        			if (val == 0)
        			{
        				visit[z][y*width+x] = true;
        				visited++;
        			}
        		}
        	}
        }
       IJ.log("total: "+total+ " visited: "+visited); 
       while (visited < total)
       {
	        for (int x=0; x < width; x++)
	        {
	        	for (int y=0; y < height; y++)
	        	{
	        		for (int z=0; z < zSize; z++)
	        		{
	        			short val = voxels[z][y*width+x];
	        			short dfeVal = dfes[z][y*width+x];
	        			if (val > 0 && dfeVal == 0)
	        			{
	        				visited += visitDfe(x, y, z, voxels, dfes, width, height, zSize, visit);
	        			}
	        		}
	        	}
	        }
	        IJ.log("Visited: "+visited);
       }
        IJ.log("finished visits");
        return dfes;
    }
    
    private int visitDfe(int x, int y, int z, short[][] voxels, short[][] dfes, 
    		int width, int height, int zSize, boolean[][] visit)
    {
    	int visited=0;
    	Position pos = new Position(x, y, z);
		Queue<Position> neighbors = pos.getNeighbors(width, height, zSize);
		Position neigh = neighbors.poll();
		short dfe = dfes[z][y*width+x];
		int minNewDfe = Integer.MAX_VALUE;
		while (neigh != null)
		{
			int nx = neigh.getColumn();
			int ny  = neigh.getRow();
			int nz = neigh.getZ();
			if (nx >=0 && nx < width && ny > 0 && ny < height && nz > 0 && nz < zSize && 
					visit[nz][ny*width+nx] == true)
			{
				int dx = Math.abs(x-nx);
				int dy = Math.abs(y-ny);
				int dz = Math.abs(z-nz);
				short nDfe = dfes[nz][ny*width+nx];
				short nVal = voxels[nz][ny*width+nx];
				
				short addDfe = 0;
				if (dx>0 && dy > 0 && dz > 0)
					addDfe = _colRowZ;
				else if (dx > 0 && dy > 0)
					addDfe = _colRow;
				else if (dx > 0 && dz > 0)
					addDfe = _colZ;
				else if (dy > 0 && dz >0)
					addDfe = _rowZ;
				else if (dx > 0)
					addDfe = _col;
				else if (dy > 0)
					addDfe = _row;
				else if (dz > 0)
					addDfe = _z;
				
				int newDfe = nDfe + addDfe;
				if (newDfe < minNewDfe)
				{
					minNewDfe = newDfe;
				}
			}
			neigh = neighbors.poll();
		}
		if (minNewDfe < Integer.MAX_VALUE && (dfe == 0 || minNewDfe < dfe) )
		{
			dfes[z][y*width+x] = (short)minNewDfe;
			visit[z][y*width+x] = true;
			visited++;
		}
		return visited;
    }
    
    public short[][] distanceFromEdge26ray(short[][] voxels, int width, int height)
    {
    	// calculate direction costs and round to short 
    	// for the bottom planes do a 2-D distance from edge 
    	
        int zSize = voxels.length;
        int low = zSize - _lower3Dcutoff;
        
        short[][] dfes = new short[zSize][width*height];
        for (int z=0; z<zSize; z++)
        {
            for (int r=0; r<height; r++)
            {
                for (int c=0; c<width; c++)
                {
                	int v = 0xFFF & voxels[z][r*width+c]; 
                    if ( v > 0)
                    {
                    	// IJ.log("DFE input value: "+voxels[z][r*width+c]);
                    	if (z <= _lower3Dcutoff)
                    	{
                    		dfes[z][r*width+c] = minDFE2D(voxels, zSize, width, height, c, r, z);
                    	}
                    	else if (z > _lower3Dcutoff && z <= low)
                    	{
                    		dfes[z][r*width+c] = minDFE3D(voxels, zSize, width, height, c, r, z);
                    	}
                    	else
                    	{
                    		dfes[z][r*width+c] = minDFE2D(voxels, zSize, width, height, c, r, z);
                    	}
                    }
                }
            }
        }
        return dfes;
    }
    private short minDFE2D(short[][] pixels, int zSize, int width, int height, int col, int row, int z)
    {
    	short dfe = Short.MAX_VALUE;
    	short[] dfes = new short[8];
        
        int rowWidth = row*width;
        
    	// Column
        for (int c=col; c<width; c++)
        {
        	int p = 0xFFF & pixels[z][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[0]+=_col;
        }
        for (int c=col; c>=0; c--)
        {
        	int p = 0xFFF & pixels[z][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[1]+=_col;
        }
        
        // Row 
        for (int r=row; r<height; r++)
        {
        	int p = 0xFFF & pixels[z][r*width+col]; 
            if ( p == 0)
                break;
            dfes[2]+=_row;
        }
        for (int r=row; r>=0; r--)
        {
        	int p = 0xFFF & pixels[z][r*width+col]; 
            if ( p == 0)
                break;
            dfes[3]+=_row;
        }
        
        // Single diagonal
        // Column, Row
        for (int r=row, c=col; r<height && c<width; r++, c++)
        { 
        	int p = 0xFFF & pixels[z][r*width+c];
            if ( p == 0)
                break;
            dfes[4]+=_colRow;
        }
        for (int r=row, c=col; r<height && c>=0; r++, c--)
        {  
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[5]+=_colRow;
        }
        for (int r=row, c=col; r>=0 && c<width; r--, c++)
        { 
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[6]+=_colRow;
        }
        for (int r=row, c=col; r>=0 && c>=0; r--, c--)
        {
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[7]+=_colRow;
        }
        // find minimum DFE
        for (int i=0; i<dfes.length; i++)
        {
            if (dfes[i] < dfe)
            {
                dfe = dfes[i];
            }
        }
    	return dfe;
    }
    /** Find the 3-D distance from edge of the voxel at col, row, z */ 
    private short minDFE3D(short[][] pixels, int zSize, int width, int height, int col, int row, int z)
    {
        short dfe = Short.MAX_VALUE;
        short[] dfes = new short[26];
        
        int rowWidth = row*width;
        int rowCol = row*width+col;
        
        // Face step cost = resolution 
        
        // Column
        for (int c=col; c<width; c++)
        {
        	int p = 0xFFF & pixels[z][rowWidth+c];
            if (p == 0)
                break;
            dfes[0]+=_col;
        }
        for (int c=col; c>=0; c--)
        {
        	int p = 0xFFF & pixels[z][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[1]+=_col;
        }
        
        // Row 
        for (int r=row; r<height; r++)
        {
        	int p = 0xFFF & pixels[z][r*width+col]; 
            if ( p == 0)
                break;
            dfes[2]+=_row;
        }
        for (int r=row; r>=0; r--)
        {
        	int p = 0xFFF & pixels[z][r*width+col]; 
            if ( p == 0)
                break;
            dfes[3]+=_row;
        }
        
        // Z
        for (int zz=z; zz<zSize; zz++)
        {
        	int p = 0xFFF & pixels[zz][rowCol]; 
            if ( p == 0)
                break;
            dfes[4]+=_z;
        }
        for (int zz=z; zz>=0; zz--)
        {
        	int p = 0xFFF & pixels[zz][rowCol]; 
            if ( p == 0)
                break;
            dfes[5]+=_z;
        }
        
        
        // Single diagonal
        
        // Column, Row
        for (int r=row, c=col; r<height && c<width; r++, c++)
        { 
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[6]+=_colRow;
        }
        for (int r=row, c=col; r<height && c>=0; r++, c--)
        {  
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[7]+=_colRow;
        }
        for (int r=row, c=col; r>=0 && c<width; r--, c++)
        { 
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[8]+=_colRow;
        }
        for (int r=row, c=col; r>=0 && c>=0; r--, c--)
        {
        	int p = 0xFFF & pixels[z][r*width+c]; 
            if ( p == 0)
                break;
            dfes[9]+=_colRow;
        }
        
        // Column, Z
        for (int c=col, zz=z; c<width && zz<zSize; c++, zz++)
        {  
        	int p = 0xFFF & pixels[zz][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[10]+=_colZ;
        }
        for (int c=col, zz=z; c>=0 && zz<zSize; c--, zz++)
        {   
        	int p = 0xFFF & pixels[zz][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[11]+=_colZ;
        }
        for (int c=col, zz=z; c<width && zz>=0; c++, zz--)
        {
        	int p = 0xFFF & pixels[zz][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[12]+=_colZ;
        }
        for (int c=col, zz=z; c>=0 && zz>=0; c--, zz--)
        {
        	int p = 0xFFF & pixels[zz][rowWidth+c]; 
            if ( p == 0)
                break;
            dfes[13]+=_colZ;
        }
        
        // Row, Z
        for (int r=row, zz=z; r<height && zz<zSize; r++, zz++)
        {
        	int p = 0xFFF & pixels[zz][r*width+col];
            if ( p == 0)
                break;
            dfes[14]+=_rowZ;
        }
        for (int r=row, zz=z; r>=0 && zz<zSize; r--, zz++)
        {
        	int p = 0xFFFF & pixels[zz][r*width+col]; 
            if ( p == 0)
                break;
            dfes[15]+=_rowZ;
        }
        for (int r=row, zz=z; r<height && zz>=0; r++, zz--)
        {
        	int p = 0xFFFF & pixels[zz][r*width+col]; 
            if ( p == 0)
                break;
            dfes[16]+=_rowZ;
        }
        for (int r=row, zz=z; r>=0 && zz>=0; r--, zz--)
        {
        	int p = 0xFFFF & pixels[zz][r*width+col]; 
            if ( p == 0)
                break;
            dfes[17]+=_rowZ;
        }
        
        // Triple diagonal: Column, Row, Z
        for (int r=row, c=col, zz=z; r<height && c<width && zz<zSize; r++, c++, zz++)
        {
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[18]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r<height && col>=0 && zz<zSize; r++, c--, zz++)
        {
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[19]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r>=0 && c<width && zz<zSize; r--, c++, zz++)
        {
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[20]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r>=0 && c>=0 && zz<zSize; r--, c--, zz++)
        {
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[21]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r<height && c<width && zz>=0; r++, c++, zz--)
        {
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[22]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r<height && c>=0 && zz>=0; r++, c--, zz--)
        {   
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[23]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r>=0 && c<width && zz>=0; r--, c++, zz--)
        {   
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[24]+=_colRowZ;
        }
        for (int r=row, c=col, zz=z; r>=0 && c>=0 && zz>=0; r--, c--, zz--)
        {
        	int p = 0xFFFF & pixels[zz][r*width+c];
            if ( p == 0)
                break;
            dfes[25]+=_colRowZ;
        }
        
        // find minimum DFE
        for (int i=0; i<dfes.length; i++)
        {
            if (dfes[i] < dfe)
            {
                dfe = dfes[i];
            }
        }
        
        return dfe;
    }
    
    public boolean isShowSteps()
    {
        return _showSteps;
    }
    public void setShowSteps(boolean showSteps)
    {
        _showSteps = showSteps;
    }
	public int getLower3Dcutoff() {
		return _lower3Dcutoff;
	}
	/** 2-D image planes below this many steps from the lower edge of the image will
	 * have DFE calculated in 2-D. The 3-D DFE calculation assigns all voxels in arteries running 
	 * off the edge of the image low DFE values.*/
	public void setLower3Dcutoff(int lower3Dcutoff) {
		_lower3Dcutoff = lower3Dcutoff;
	}
}

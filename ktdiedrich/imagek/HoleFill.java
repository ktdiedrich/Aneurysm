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

import ij.*;

/** Fill holes in a 3-D artery reconstruction 
 * Checks line of 3 neighbors in 26 directions. If 21 or more of 26 
 * directions has an artery voxel, the voxel called artery and adds 
 * to the artery reconstruction. 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class HoleFill
{
    // public static final short MIN_FILL_VALUE = 255;
    private int _radius;
    private int _directions;
    
    public HoleFill()
    {
        _radius = 0;
        _directions = 0; 
    }
    public void fillHoles(ImageStack origStack, ImageStack arteryStack)
       
    {
        int zSize = arteryStack.getSize();
        int height = arteryStack.getHeight();
        int width = arteryStack.getWidth();
        short[][] arteryPixels = new short[zSize][];
        short[][] origPixels = new short[zSize][];
        
        for (int z=0; z<zSize; z++ )
        {
            arteryPixels[z] = (short[])arteryStack.getPixels(z+1);
            origPixels[z] = (short[])origStack.getPixels(z+1);
        }
        for (int z=0; z<zSize; z++ )
        {
            for (int row=0; row<height; row++)
            {
                for (int col=0; col<width; col++)
                {
                    if (arteryPixels[z][row*width+col] == 0 && origPixels[z][row*width+col] > 0)
                    {
                        int neighbors = arteryNeighbors(col, row, z, origPixels, arteryPixels, width, height, zSize);
                        if (neighbors >= _directions)
                        {
                        	short holeInt = origPixels[z][row*width+col];
                        	arteryPixels[z][row*width+col] = holeInt;
                        	// if (holeInt > MIN_FILL_VALUE)
                        	//	arteryPixels[z][row*width+col] = holeInt;
                        	// else
                        	//	arteryPixels[z][row*width+col] = MIN_FILL_VALUE;
                        	
                        }
                    }
                }
            }
        }
        
    }
    public int arteryNeighbors(int col, int row, int z, short[][] origPixels, short[][] arteryPixels, 
            int width, int height, int zSize)
        
    {
        
        int c=0, r=0, zz=0;
        int neighbors = 0;
        c = col;    r=row; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            c++;
            if (c == width)
                break;
            if (arteryPixels[z][row*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        c=col;  r=row; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            c--;
            if (c < 0)
                break;
            if (arteryPixels[z][row*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        r = row;    c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++;
            if (r == height)
                break;
            if (arteryPixels[z][r*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        r=row; c=col;   zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++; c++;
            if (r == height || c == width)
                break;
            if (arteryPixels[z][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        r=row; c=col;   zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++; c--;
            if (r == height || c < 0)
                break;
            if (arteryPixels[z][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        r=row;  c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--;
            if (r < 0)
                break;
            if (arteryPixels[z][r*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        r=row; c=col;   zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--; c++;
            if (r < 0 || c == width)
                break;
            if (arteryPixels[z][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        r=row; c=col;   zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--; c--;
            if (r < 0 || c < 0)
                break;
            if (arteryPixels[z][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        zz = z; r=row; c=col;
        for (int i=1; i<=_radius; i++)
        {
            zz++;
            if (zz == zSize)
                break;
            
            if (arteryPixels[zz][row*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        c=col; zz=z;    r=row;
        for (int i=1; i<=_radius; i++)
        {
            c++; zz++;
            if (c == width || zz == zSize)
                break;
            if (arteryPixels[zz][row*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        c=col; zz=z;    r=row;
        for (int i=1; i<=_radius; i++)
        {
            c--; zz++;
            if (c < 0 || zz == zSize)
                break;
            if (arteryPixels[zz][row*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; zz=z;    c=col;
        for (int i=1; i<=_radius; i++)
        {
            r++; zz++;
            if (r == height || zz == zSize)
                break;
            if (arteryPixels[zz][r*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++; c++; zz++;
            if (r == height || c == width || zz == zSize)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++; c--; zz++;
            if (r == height || c < 0 || zz == zSize)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; zz=z;    c=col;
        for (int i=1; i<=_radius; i++)
        {
            r--;  zz++;  
            if (r < 0 || zz == zSize)
                break;
            if (arteryPixels[zz][r*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--; c++; zz++;
            if (r < 0 || c == width || zz == zSize)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--; c--; zz++;
            if (r < 0 || c < 0 || zz == zSize)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        zz=z;   r=row; c=col;
        for (int i=1; i<=_radius; i++)
        {
            zz--;
            if (zz < 0)
                break;
            if (arteryPixels[zz][row*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        c=col; zz=z;    r=row;
        for (int i=1; i<=_radius; i++)
        {
            c++; zz--;
            if (c == width || zz < 0)
                break;
            if (arteryPixels[zz][row*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        c=col; zz=z;    r=row;
        for (int i=1; i<=_radius; i++)
        {
            c--; zz--;
            if (c < 0 || zz < 0)
                break;
            if (arteryPixels[zz][row*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; zz=z;    c=col;
        for (int i=1; i<=_radius; i++)
        {
            r++; zz--;
            if (r == height || zz < 0)
                break;
            if (arteryPixels[zz][r*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++; c++; zz--;
            if (r == height || c == width || zz < 0)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r++; c--; zz--;
            if (r == height || c < 0 || zz < 0)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; zz=z;    c=col;
        for (int i=1; i<=_radius; i++)
        {
            r--; zz--;
            if (r < 0 || zz < 0)
                break;
            if (arteryPixels[zz][r*width+col] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--; c++; zz--;
            if (r < 0 || c == width || zz < 0)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        r=row; c=col; zz=z;
        for (int i=1; i<=_radius; i++)
        {
            r--; c--; zz--;
            if (r < 0 || c < 0 || zz < 0)
                break;
            if (arteryPixels[zz][r*width+c] > 0)
            {
                neighbors++;
                break;
            }
        }
        
        return neighbors;
    }
       
    public int getRadius()
    {
        return _radius;
    }
    public void setRadius(int radius)
    {
        _radius = radius;
    }
    public int getDirections()
    {
        return _directions;
    }
    public void setDirections(int directions)
    {
        _directions = directions;
    }
}

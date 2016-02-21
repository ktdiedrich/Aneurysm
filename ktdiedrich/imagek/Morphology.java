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

import ij.ImagePlus;
import ij.ImageStack;

/** Morphological image operations
 * @author Karl Diedrich <ktdiedrich@gmail.com> */
public class Morphology
{
	/** 2-D 3x3 dilation 
	 * @param replaceFrom is the structuring element */
    public static short[][] dilate(short[][] toDilate, short[][] replaceFrom)
    {
        int height = toDilate.length;
        int width = toDilate[0].length;
        short[][] dilated = new short[height][width];
        assert height == replaceFrom.length;
        assert width == replaceFrom[0].length;
        
        for (int r=1; r<height-1; r++)
        {
            for (int c=1; c<width-1; c++)
            {
                dilated[r][c] = toDilate[r][c];
                if (toDilate[r][c] != 0)
                {
                    if (toDilate[r-1][c]==0)
                        dilated[r-1][c] = replaceFrom[r-1][c];
                    if (toDilate[r][c-1]==0)
                        dilated[r][c-1] = replaceFrom[r][c-1];
                    if (toDilate[r+1][c]==0)
                        dilated[r+1][c] = replaceFrom[r+1][c];
                    if (toDilate[r][c+1]==0)
                        dilated[r][c+1] = replaceFrom[r][c+1];
                                                            
                }
            }
        }
        return dilated;
    }
    
    public static void deleteStructure(short[][] voxels, int width, short[][][] structure, int x, int y, int z)
    {
    	int sz = structure.length;
    	int sh = structure[0].length;
    	int sw = structure[0][0].length;
    	int csz = (sz-1)/2;
    	int csh = (sh-1)/2;
    	int csw = (sw-1)/2;
    	x=x-csw;
    	y=y-csh;
    	z=z-csz;
    	//boolean contained = true;
    	for (int wp=0; wp<sw; wp++)
    	{
    		for (int hp=0; hp<sh; hp++)
    		{
    			//if (contained==false) break;
    			for (int zp=0; zp < sz; zp++)
    			{
    				if (structure[zp][hp][wp] > 0 && voxels[(z+zp)][(y+hp)*width+(x+wp)] > 0)
    				{
    					voxels[(z+zp)][(y+hp)*width+(x+wp)] = 0;
    				}
    			}
    		}
    		//if (contained==false) break;
    	}
    }
}

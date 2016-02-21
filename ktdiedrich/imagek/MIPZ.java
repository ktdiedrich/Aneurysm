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
import ij.process.*;
import ktdiedrich.math.MatrixUtil;

/** Maximum Intensity Projection Z buffer 
 * @author Karl Diedrich <ktdiedrich@gmail.com>*/
public class MIPZ 
{
    public static final short DIFF = 2;
    public static final short THRESHOLD = 4;
    public static final short ITERATIONS = 1;
    private short _diff;
    private short _threshold;
    private short _iterations;
    private int _dataLen;
    protected int _height;
    private int _width;
    private short[] _zData;
    public MIPZ()
    {
        this(DIFF, THRESHOLD, ITERATIONS);
    }
    public MIPZ(short difference, short threshold, short iterations)
    {
        _diff = difference;
        _threshold = threshold;
        _iterations = iterations;
    }
    public ImagePlus createMIPZImage(ImageStack stack)
    {
        int slices = stack.getSize();       
        Object slice1 = stack.getPixels(1);
        String dataType = slice1.getClass().getName();
        short[] slice1data = (short[])stack.getPixels(1); 
        _dataLen = slice1data.length;
        _height = stack.getHeight();
        _width = stack.getWidth();
        
        short[] mipData = new short[slice1data.length];
        short[] sliceData;
        _zData = new short[slice1data.length];
        for (short i=1; i <= slices; i++)
        {
            sliceData = (short[])stack.getPixels(i);   
            for (int j=0; j<sliceData.length; j++)
            {
                if(sliceData[j] > mipData[j])
                {
                    mipData[j] = sliceData[j];       
                    _zData[j] = i;
                }
            }
        }
        
        ImageProcessor mipProc = new ShortProcessor(_width, _height, _zData, null);
        ImagePlus mip = new ImagePlus("MIP-Z", mipProc);
        return mip;
    }
    
    public ImagePlus neighborSmoothedImage(ImageStack stack)
    {
        createMIPZImage(stack);
        return neighborSmoothedImage();
    }
    private short[][] smoothImage(short[][] zSqr)
    {
        int index = 0;
        short neighbors = 0;
        short[][] smoothedZSqr = new short[_height][_width];
        for (int row=0; row < _height; row++)
        {
            for (int col=0; col < _width; col++)
            {
                neighbors = 0;
                try 
                {
                    if (col < _width-1)
                    {
                        if (zSqr[row][col] >= zSqr[row][col+1]-_diff && zSqr[row][col] <= zSqr[row][col+1]+_diff)
                        {
                            neighbors++;
                        }
                    }
                    if (row < _height-1)
                    {
                        if (zSqr[row][col] >= zSqr[row+1][col]-_diff && zSqr[row][col] <= zSqr[row+1][col]+_diff)
                        {
                            neighbors++;
                        }
                    }
                    if (col < _width-1 && row < _height-1)
                    {
                        if (zSqr[row][col] >= zSqr[row+1][col+1]-_diff && zSqr[row][col] <= zSqr[row+1][col+1]+_diff)
                        {
                            neighbors++;
                        }
                    }
                    if (row > 0 && col > 0)
                    {
                        if (zSqr[row][col] >= zSqr[row-1][col-1]-_diff && zSqr[row][col] <= zSqr[row-1][col-1]+_diff)
                        {
                            neighbors++;
                        }
                    }
                    if (row > 0)
                    {
                        if (zSqr[row][col] >= zSqr[row-1][col]-_diff && zSqr[row][col] <= zSqr[row-1][col]+_diff)
                        {
                            neighbors++;
                        }   
                        if (col < _width-1 && zSqr[row][col] >= zSqr[row-1][col+1]-_diff && zSqr[row][col] <= zSqr[row-1][col+1]+_diff)
                        {
                            neighbors++;
                        }  
                    } 
                    if (col > 0)
                    {
                        if (zSqr[row][col] >= zSqr[row][col-1]-_diff && zSqr[row][col] <= zSqr[row][col-1]+_diff)
                        {
                            neighbors++;
                        }   
                        if (row < _height-1 && zSqr[row][col] >= zSqr[row+1][col-1]-_diff && zSqr[row][col] <= zSqr[row+1][col-1]+_diff)
                        {
                            neighbors++;
                        }  
                    }                
                    if (neighbors > _threshold)
                    {
                        smoothedZSqr[row][col] = zSqr[row][col];
                    }   
                    else
                    {
                        
                    }
                } 
                catch (ArrayIndexOutOfBoundsException ex)
                {
                    System.err.println("Height: "+_height+" Row: "+row+ " Width: "+_width+" Col: "+col);
                    throw ex;
                }  
                index++;      
            }          
        }
        return smoothedZSqr;
    }
    /**  @return Neighbor-smoothed image of last image stack. */
    public ImagePlus neighborSmoothedImage()
    {
        short[][] zSqr = new short[_height][_width]; 
        int row=0;
        int col=0;        
        for (int i=0; i < _zData.length; i++)
        {
            if (col == _width)
            {
                row++;
                col = 0;
            }
            zSqr[row][col] = _zData[i];
            col++;
        }
        
        for (int i=0; i<_iterations; i++)
        {
            zSqr = smoothImage(zSqr);
        }
       
        
        ImageProcessor smoothProc = new ShortProcessor(_width, _height, MatrixUtil.square2array(zSqr), null);
        return new ImagePlus("Smooth MIP-Z; Difference: "+_diff+" Threshold: "+_threshold+" Iterations: "+_iterations, smoothProc);
    }
}

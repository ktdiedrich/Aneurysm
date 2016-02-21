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
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ktdiedrich.math.MatrixUtil;

/** Tools for Rough images such as MIP Z-buffer
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class RoughUtil
{
    public static final short DIFF = 2;
    public static final short THRESHOLD = 4;
    public static final short ITERATIONS = 1;
    private int _difference;
    private int _threshold;
    private int _iterations;
    private int _height, _width;
    public RoughUtil()
    {
        this(DIFF, THRESHOLD, ITERATIONS);
    }
    /** @param difference the difference in neighboring pixel values to accept. 
     * @param threshold The number of neighboring pixels with the difference value to accept the current pixel
     * @param iterations The number of time to run the algorithm */
    public RoughUtil(int difference, int threshold, int iterations)
    {
        _difference = difference;
        _threshold = threshold;
        _iterations = iterations;
    }
    private short[][] keepSmooth(short[][] zSqr)
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
                        if (zSqr[row][col] >= zSqr[row][col+1]-_difference && zSqr[row][col] <= zSqr[row][col+1]+_difference)
                        {
                            neighbors++;
                        }
                    }
                    if (row < _height-1)
                    {
                        if (zSqr[row][col] >= zSqr[row+1][col]-_difference && zSqr[row][col] <= zSqr[row+1][col]+_difference)
                        {
                            neighbors++;
                        }
                    }
                    if (col < _width-1 && row < _height-1)
                    {
                        if (zSqr[row][col] >= zSqr[row+1][col+1]-_difference && zSqr[row][col] <= zSqr[row+1][col+1]+_difference)
                        {
                            neighbors++;
                        }
                    }
                    if (row > 0 && col > 0)
                    {
                        if (zSqr[row][col] >= zSqr[row-1][col-1]-_difference && zSqr[row][col] <= zSqr[row-1][col-1]+_difference)
                        {
                            neighbors++;
                        }
                    }
                    if (row > 0)
                    {
                        if (zSqr[row][col] >= zSqr[row-1][col]-_difference && zSqr[row][col] <= zSqr[row-1][col]+_difference)
                        {
                            neighbors++;
                        }   
                        if (col < _width-1 && zSqr[row][col] >= zSqr[row-1][col+1]-_difference && zSqr[row][col] <= zSqr[row-1][col+1]+_difference)
                        {
                            neighbors++;
                        }  
                    } 
                    if (col > 0)
                    {
                        if (zSqr[row][col] >= zSqr[row][col-1]-_difference && zSqr[row][col] <= zSqr[row][col-1]+_difference)
                        {
                            neighbors++;
                        }   
                        if (row < _height-1 && zSqr[row][col] >= zSqr[row+1][col-1]-_difference && zSqr[row][col] <= zSqr[row+1][col-1]+_difference)
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
    public ImagePlus keepSmooth(ImagePlus image)
    {
        short[] zData = (short[])image.getProcessor().getPixels();
        _height = image.getHeight();
        _width = image.getWidth();
        short[][] zSqr = MatrixUtil.array2Square(zData, _height, _width);
        
        for (int i=0; i<_iterations; i++)
        {
            zSqr = keepSmooth(zSqr);
        }
       
        
        ImageProcessor smoothProc = new ShortProcessor(_width, _height, MatrixUtil.square2array(zSqr), null);
        return new ImagePlus("MIPZ_Diff_"+_difference+"_Thres_"+_threshold+"_Iter_"+_iterations, smoothProc);
    }
    
    public int getDifference()
    {
        return _difference;
    }
    public void setDifference(int difference)
    {
        _difference = difference;
    }
    public int getThreshold()
    {
        return _threshold;
    }
    public void setThreshold(int threshold)
    {
        _threshold = threshold;
    }
    public int getIterations()
    {
        return _iterations;
    }
    public void setIterations(int iterations)
    {
        _iterations = iterations;
    }
}

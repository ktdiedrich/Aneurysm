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

import java.util.*;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
//import ij.text.TextWindow;

/** Median Filter
 * @author Karl T. Diedrich <ktdiedrich@gmail.com>
 * */
public class MedianFilter
{
    private boolean _showSteps;
    private int _medianFilterSize; 
    public static final int MEDIAN_FILTER_SIZE = 5;
    public static final double STD_DEV_FACTOR = 1.0;
    private double _stdDevFactor;
    //private TextWindow _twin;
    public MedianFilter()
    {
        _medianFilterSize = MEDIAN_FILTER_SIZE;
        _stdDevFactor = STD_DEV_FACTOR;
        //_twin = new TextWindow("Median_Filter", "", 400, 400);
    }
    public void medianFilter(ImagePlus image)
    {
    	medianFilter(image.getImageStack());
    }
    public void medianFilter(ImageStack inputStack)
    {
        
        int width = inputStack.getWidth();
        int height = inputStack.getHeight();
        short sliceAveIntSum = 0;
        ImageStack medianFilteredStack = new ImageStack(width, height);
        int zSize = inputStack.getSize();
        
        for (int i=1; i<=zSize; i++)
        {
            ImageProcessor inputProc =  inputStack.getProcessor(i);
            ImageStat stat = new ImageStat(inputProc);
            double aveNonZeroPixelInt = stat.getNonZeroPixelAveInt();
            short avePixelInt = (short) aveNonZeroPixelInt;
            sliceAveIntSum += avePixelInt;
        }
        short imageAveInt = (short) (sliceAveIntSum/zSize);
        
        for (int i=1; i<=zSize; i++)
        {
            ImageProcessor inputProc =  inputStack.getProcessor(i);
            ImageProcessor inputProcDup = inputProc.duplicate();
            ImageStat stat = new ImageStat(inputProcDup);
            double aveNonZeroPixelInt = stat.getNonZeroPixelAveInt();
            short avePixelInt = (short) aveNonZeroPixelInt;
            sliceAveIntSum += avePixelInt;
            double stdDev = stat.getStdDev();
            short upperCut = (short) (aveNonZeroPixelInt+(_stdDevFactor*stdDev));
            short[] pix = (short[])inputProcDup.getPixels();
            for (int j=0; j<pix.length; j++)
            {
                if (pix[j] > upperCut)
                {
                    pix[j] = avePixelInt;
                }
            }
            ImageProcessor medianFiltered = medianFilter(inputProcDup, _medianFilterSize);
            medianFilteredStack.addSlice("MedianFiltered"+(i), medianFiltered);
            ImageProcess.subtract(inputProc, medianFiltered, imageAveInt);   
        }
        
        if (_showSteps)
        {
            System.out.println("Average stack intensity: "+imageAveInt);
            ImagePlus image = new ImagePlus("Median_Filtered_Image");
            image.setStack("Median_Filtered_Stack", medianFilteredStack);
            image.show();
            image.updateAndDraw();
        }
    }
    
    /** median filter Short image 
     * @param halfSize total filter size is 2 * halfSize + 1*/
    public ImageProcessor medianFilter(ImageProcessor proc, int halfSize)
    {
        int fullSize = (halfSize*2) + 1;
        int height = proc.getHeight();
        int width = proc.getWidth();
        // ImageProcessor medianProc = new ShortProcessor(width, height);
        ImageProcessor medianProc = proc.duplicate();
        short[] medianPixels = (short[])medianProc.getPixels();
        short[] pixels = (short[])proc.getPixels();
        
        int inset = halfSize;
        
        for (int row=inset; row < height-inset; row++)
        {
            boolean madeFirst = false;
            List<Short> filterVals = new ArrayList<Short>(fullSize*fullSize);
            for (int col=inset; col<width-inset; col++)
            {
                if (pixels[row*width+col] > 0)
                {
                    if (madeFirst == false) // fill first window in row 
                    {
                        for (int i=-halfSize; i<=halfSize; i++)
                        {
                            for (int j=-halfSize; j <=halfSize; j++)
                            {
                                short p = pixels[((row+i)*width)+(col+j)];
                                if (p>0)
                                    filterVals.add(p);
                            }
                        }
                        madeFirst = true;
                    }
                    else // shift columns
                    {
                        for (int i=-halfSize; i<=halfSize; i++)
                        {
                            short p = 0;
                            p = pixels[((row+i)*width)+(col-halfSize-1)];
                            if (p > 0)
                               filterVals.remove((Short)p);
                            p = pixels[((row+i)*width)+(col+halfSize)];
                            if (p>0)
                                filterVals.add(p);
                        }
                    }
                    // TODO only sort in new elements 
                    Collections.sort(filterVals);
                    int ln = filterVals.size();
                    int cn = ln/2;
                    Short val = 0;
                    if (ln%2 == 1)
                        val = filterVals.get(cn);
                    else
                        val = (short) ((filterVals.get(cn) + filterVals.get(cn-1))/2);
                    medianPixels[row*width+col] = val.shortValue(); 
                }   
            }
        }
        return medianProc;
    }
    public boolean isShowSteps()
    {
        return _showSteps;
    }
    public void setShowSteps(boolean showSteps)
    {
        _showSteps = showSteps;
    }
    public int getMedianFilterSize()
    {
        return _medianFilterSize;
    }
    public void setMedianFilterSize(int medianFilterSize)
    {
        _medianFilterSize = medianFilterSize;
    }
    public double getStdDevFactor()
    {
        return _stdDevFactor;
    }
    public void setStdDevFactor(double stdDevFactor)
    {
        _stdDevFactor = stdDevFactor;
    }
}

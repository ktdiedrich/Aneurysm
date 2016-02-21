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

import ij.process.ImageProcessor;

import java.util.SortedMap;
import java.util.TreeMap;

/** Statistics on Images  
 * Generate a histogram of image values. 
 * @return Intensity value: count pairs 
 * @uathor Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class ImageStat
{   
    private int _nonZeroPixelCount;
    private double _nonZeroPixelAveInt;
    private ImageProcessor _proc;
    private SortedMap<Short, Integer> _histogram;
    private double _stdDev;
    public ImageStat(ImageProcessor proc)
    {
        _proc = proc;
        makeHistogram();
    }
    protected void makeHistogram()
    {
        _histogram = new TreeMap<Short, Integer>();
        short[] pixels = (short[])_proc.getPixels();
        _nonZeroPixelCount = 0;
        int sumPixelInt = 0;
        int sum2 = 0;
        for (int i=0; i<pixels.length; i++)
        {
            short intensity = pixels[i];
            if (intensity > 0)
            {
                if (_histogram.containsKey(intensity))
                {
                    Integer count = _histogram.get(intensity);
                    count++;
                    _histogram.put(intensity, count);
                }
                else
                {
                    _histogram.put(intensity, 1);
                }
                sumPixelInt += (int) intensity;
                sum2 += ( (int) (intensity*intensity) );
                _nonZeroPixelCount++;
            }
        }
        _nonZeroPixelAveInt = (double)sumPixelInt / (double)_nonZeroPixelCount;
        
        double var = (1.0/(_nonZeroPixelCount-1.0)) * 
            (sum2 - (1.0/_nonZeroPixelCount)*(sumPixelInt*sumPixelInt));
        _stdDev = Math.sqrt(var);
    }
    public ImageProcessor getProc()
    {
        return _proc;
    }
    public void setProc(ImageProcessor proc)
    {
        _proc = proc;
        makeHistogram();
    }
    public int getNonZeroPixelCount()
    {
        return _nonZeroPixelCount;
    }
    public double getNonZeroPixelAveInt()
    {
        return _nonZeroPixelAveInt;
    }
    public SortedMap<Short, Integer> getHistogram()
    {
        return _histogram;
    }
    public double getStdDev()
    {
        return _stdDev;
    }
}

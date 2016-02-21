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

/** Generate a histogram of image values. 
 * @return Intensity value: count pairs 
 * @uathor Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Histogram
{
    public SortedMap<Short, Integer> histogram(ImageProcessor proc)
    {
        SortedMap<Short, Integer> histogram = new TreeMap<Short, Integer>();
        short[] pixels = (short[])proc.getPixels();
        int allHistCount = 0;
        for (int i=0; i<pixels.length; i++)
        {
            short intensity = pixels[i];
            if (histogram.containsKey(intensity))
            {
                Integer count = histogram.get(intensity);
                count++;
                histogram.put(intensity, count);
            }
            else
            {
                histogram.put(intensity, 1);
            }
            allHistCount++;
        }
        return histogram;
    }
}

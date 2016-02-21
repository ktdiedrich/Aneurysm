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

package ktdiedrich.math;

import ij.ImageStack;
import ij.process.ImageProcessor;

import java.util.LinkedList;
import java.util.List;


/** Statistics Utilities
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class StatsUtil
{
    protected StatsUtil()
    {
        
    }
    public static double standardDeviation(List<Short> values, double xbar)
    {
        double N  = (double)values.size();
        double dev2 = 0.0;
        for (Short v: values)
        {
            dev2 += Math.pow( (double)v - xbar, 2); 
        }
        return Math.pow(dev2/(N), 0.5);
        
    }
    public static void main(String args[])
    {
        // c(5,7,3,8,9,10,4,6, 11, 5)
        List<Short> vals = new LinkedList<Short>();
        vals.add((short)5); vals.add((short)7); vals.add((short)3); vals.add((short)8); vals.add((short)9);
        vals.add((short)10); vals.add((short)4); vals.add((short)6); vals.add((short)11); vals.add((short)5);
        
        double sd = standardDeviation(vals, 6.8);
        System.out.println("Standard deviation: "+sd);
    }
    public void histogram(ImageStack stack)
    {
        int zSize = stack.getSize();
        int imageHigh = 0;
        
        
        for (int i=1; i<zSize; i++)
        {
            ImageProcessor proc = stack.getProcessor(i);
            short[] intensities = (short[])proc.getPixels();
            for (int j=0; j<intensities.length; j++)
            {
                if (intensities[j]>imageHigh)
                    imageHigh = intensities[j];
            }
        }
        short[] histogram = new short[imageHigh+1];
        System.out.println("Image high: "+imageHigh);
        for (int i=1; i<zSize; i++)
        {
            ImageProcessor proc = stack.getProcessor(i);
            short[] intensities = (short[])proc.getPixels();
            for (int j=0; j<intensities.length; j++)
            {
                histogram[ intensities[j] ]++;
            }
        }
        for (int i=0; i<histogram.length; i++)
        {
            System.out.println("Value: "+i+", Count: "+histogram[i]);
        }
    }
}

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

import java.util.LinkedList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

/** Threshold functions
 * @author Karl Diedrich <ktdiedrich@gmail.com> */
public class Threshold
{
    public static short averageIntensity(ImageProcessor proc)
    {
        short[] pixels = (short[])proc.getPixels();
        int sum = 0;
        for (int i=0; i< pixels.length; i++)
        {
            sum += (int)pixels[i];
        }
        return (short)(sum/pixels.length);
    }
    public static short averageIntensity(ImagePlus image)
    {
        return averageIntensity(image.getProcessor());
    }
    /** Set pixels below the threshold to 0, leave pixels above the threshold at their current value */
    public static void thresholdBelow(ImageProcessor proc,  short threshold)
    {
        short[] pixels = (short[])proc.getPixels();
        for (int i=0; i<pixels.length; i++)
        {
            if (pixels[i] < threshold)
                pixels[i] = 0;
        }
    }
    /** Set pixels below the threshold to 0, leave pixels above the threshold at their current value */
    public static void thresholdBelow(ImageProcessor proc,  byte threshold)
    {
        byte[] pixels = (byte[])proc.getPixels();
        for (int i=0; i<pixels.length; i++)
        {
            if (pixels[i] < threshold)
                pixels[i] = 0;
        }
    }
    
    /** Set pixels below the threshold to 0, leave pixels above the threshold at their current value */
    public static void thresholdBelow(List<BaseGraph<Short> > graphs,  short threshold)
    {
        for (BaseGraph<Short> graph: graphs)
        {
            LinkedList<BaseNode<Short> > nodes = graph.getNodes();
        
            for (BaseNode<Short> n: nodes)
            {
                if (n.getValue() < threshold)
                {
                    n.setValue((short)0);
                }
            }
        }
    }
    /** Set pixels below the threshold to 0, leave pixels above the threshold at their current value */
    public static void thresholdBelow(List<BaseGraph<Float> > graphs,  float threshold)
    {
        for (BaseGraph<Float> graph: graphs)
        {
            LinkedList<BaseNode<Float> > nodes = graph.getNodes();
        
            for (BaseNode<Float> n: nodes)
            {
                if (n.getValue() < threshold)
                {
                    n.setValue(0.0F);
                }
            }
        }
    }
    public static void thresholdStack(ImageStack stack, short lower, short upper)
    {
        for (int i=1; i<=stack.getSize(); i++)
        {
            ImageProcessor proc = stack.getProcessor(i);
            short[] values = (short[])proc.getPixels();
            for (int j=0; j<values.length; j++)
            {
                if (values[j] < lower || values[j] > upper)
                {
                    values[j] = 0;
                }
            }
        }
    }
    public static void thresholdBelow(ImagePlus image, short threshold)
    {
        thresholdBelow(image.getProcessor(), threshold);
    }
    public static void thresholdBelow(byte[][] voxels, byte threshold)
    {
        for (int i=0; i<voxels.length; i++)
        {
            for (int j=0; j<voxels[0].length; j++)
            {
                if (voxels[i][j] < threshold)
                    voxels[i][j] = 0;
            }
        }
    }
    public static void thresholdUnder(short[][] voxels, short threshold)
    {
    	IJ.log("Threshold: "+threshold);
        for (int i=0; i<voxels.length; i++)
        {
            for (int j=0; j<voxels[0].length; j++)
            {
                if (voxels[i][j] < threshold)
                    voxels[i][j] = 0;
            }
        }
    }
    public static void thresholdBelow(short[] pixels, short threshold)
    {
        for (int i=0; i<pixels.length; i++)
        {
            if (pixels[i] < threshold)
                pixels[i] = 0;
        }
    }
    public static void thresholdBelow(byte[] pixels, byte threshold)
    {
        for (int i=0; i<pixels.length; i++)
        {
            if (pixels[i] < threshold)
                pixels[i] = 0;
        }
    }
}

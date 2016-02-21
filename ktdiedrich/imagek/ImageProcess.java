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

import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.ByteProcessor;
import ij.ImagePlus;
import ij.ImageStack;


/** Processes of images
 * @author Karl T. Diedrich <ktdiedrich@gmail.com> 
 * */
public class ImageProcess
{
    public ImageProcess()
    {
        
    }
    public static ImageStack duplicate(ImageStack stack)
    {
    	int height = stack.getHeight();
    	int width = stack.getWidth();
    	int zSize = stack.getSize();
    	ImageStack dup = new ImageStack(width, height);
    	for (int i=0; i < zSize; i++)
    	{
    		dup.addSlice(""+i, stack.getProcessor(i+1).duplicate());
    	}
    	return dup;
    }
    public static void subtract(ImageStack stack1, ImageStack stack2, short plus)
    {
        for (int i=1; i <= stack1.getSize(); i++)
        {
            ImageProcessor proc1 = stack1.getProcessor(i);
            ImageProcessor proc2 = stack2.getProcessor(i);
            subtract(proc1, proc2, plus);
        }
    }
    public static void subtract(ImageStack stack1, ImageStack stack2)
    {
        for (int i=1; i <= stack1.getSize(); i++)
        {
            ImageProcessor proc1 = stack1.getProcessor(i);
            ImageProcessor proc2 = stack2.getProcessor(i);
            subtract(proc1, proc2);
        }
    }
    
    public static void subtract(ImageProcessor proc1, ImageProcessor proc2)
    {
        subtract(proc1, proc2, (short)0);
    }
    /** Subtract the short values of 2 processors and store the results in proc1.
     * @param plus add back in to make everything positive. */
    public static void subtract(ImageProcessor proc1, ImageProcessor proc2, short plus)
    {
        short[] pixels1 = (short[])proc1.getPixels();
        short[] pixels2 = (short[])proc2.getPixels();
        
        for (int i=0; i < pixels1.length; i++)
        {
            if (pixels1[i]>0 || pixels2[i]>0)
            {
                int sub = pixels1[i]-pixels2[i] + plus;
                pixels1[i] = (short)(sub);
            }
        }   
    }
    /** Add the short values of 2 processors and store the results in proc1. */
    public static void add(ImageProcessor proc1, ImageProcessor proc2)
    {
        short[] pixels1 = (short[])proc1.getPixels();
        short[] pixels2 = (short[])proc2.getPixels();
        
        for (int i=0; i < pixels1.length; i++)
        {
            if (pixels1[i]>0 || pixels2[i]>0)
            {
                int sub = pixels1[i]+pixels2[i];
                pixels1[i] = (short)(sub);
            }
        }   
    }
    /** Add stacks storing the result in stack1. */
    public static void add(ImageStack stack1, ImageStack stack2)
    {
        for (int i=1; i <= stack1.getSize(); i++)
        {
            ImageProcessor proc1 = stack1.getProcessor(i);
            ImageProcessor proc2 = stack2.getProcessor(i);
            add(proc1, proc2);
        }
    }
    
    /** Multiply all intensities in the the same size images and store the results in proc1. */
    public static void multiply(ImageProcessor proc1, ImageProcessor proc2)
    {
        short[] pixels1 = (short[])proc1.getPixels();
        short[] pixels2 = (short[])proc2.getPixels();
        
        for (int i=0; i < pixels1.length; i++)
        {
            if (pixels1[i]>0 || pixels2[i]>0)
            {
                int sub = pixels1[i] * pixels2[i];
                pixels1[i] = (short)(sub);
            }
        }   
    }
    
    // Short 
    public static ImageStack makeStack(short[][] voxels, int width, int height)
    {
        ImageStack stack = new ImageStack(width, height);
        for (int i=0; i < voxels.length; i++)
        {
            ImageProcessor iproc = new ShortProcessor(width, height);
            iproc.setPixels(voxels[i]);
            stack.addSlice(""+i, iproc);
        }
        return stack;
    }
    public static ImagePlus makeImage(short[][] voxels, int width, int height, String name)
    {
        ImageStack stack = makeStack(voxels, width, height);
        ImagePlus image = new ImagePlus();
        
        image.setStack(name, stack);
        return image;
    }
    
    public static ImagePlus display(short[][] voxels, int width, int height, String name)
    {
        ImagePlus image = makeImage(voxels, width, height, name);
        image.show();
        image.updateAndDraw();
        return image;
    }
    
    /** @return reference to stack voxels */
    public static short[][] getShortStackVoxels(ImageStack stack)
    {
        int zSize = stack.getSize();
        
        short[][] voxels = new short[zSize][];
        for (int i=0; i<zSize; i++)
        {
            voxels[i] = (short[])stack.getProcessor(i+1).getPixels();
        }
        return voxels;
    }
    /** @return reference to stack voxels */
    public static byte[][] getByteStackVoxels(ImageStack stack)
    {
        int zSize = stack.getSize();
        
        byte[][] voxels = new byte[zSize][];
        for (int i=0; i<zSize; i++)
        {
            voxels[i] = (byte[])stack.getProcessor(i+1).getPixels();
        }
        return voxels;
    }
    // floats
    public static ImageStack makeStack(float[][] voxels, int width, int height)
    {
        ImageStack stack = new ImageStack(width, height);
        for (int i=0; i < voxels.length; i++)
        {
            ImageProcessor iproc = new FloatProcessor(width, height);
            iproc.setPixels(voxels[i]);
            stack.addSlice(""+i, iproc);
        }
        return stack;
    }
    public static ImagePlus makeImage(float[][] voxels, int width, int height, String name)
    {
        ImageStack stack = makeStack(voxels, width, height);
        ImagePlus image = new ImagePlus();
        
        image.setStack(name, stack);
        return image;
    }
    
    public static ImagePlus display(float[][] voxels, int width, int height, String name)
    {
        ImagePlus image = makeImage(voxels, width, height, name);
        image.show();
        image.updateAndDraw();
        return image;
    }
    
    public static float[][] getFloatStackVoxels(ImageStack stack)
    {
        int zSize = stack.getSize();
        
        float[][] voxels = new float[zSize][];
        for (int i=0; i<zSize; i++)
        {
            voxels[i] = (float[])stack.getProcessor(i+1).getPixels();
        }
        return voxels;
    }
    
    // Bytes
    public static ImageStack makeStack(byte[][] voxels, int width, int height)
    {
        ImageStack stack = new ImageStack(width, height);
        for (int i=0; i < voxels.length; i++)
        {
            ImageProcessor iproc = new ByteProcessor(width, height);
            iproc.setPixels(voxels[i]);
            stack.addSlice(""+i, iproc);
        }
        return stack;
    }
    public static ImagePlus makeImage(byte[][] voxels, int width, int height, String name)
    {
        ImageStack stack = makeStack(voxels, width, height);
        ImagePlus image = new ImagePlus();
        
        image.setStack(name, stack);
        return image;
    }
    
    public static ImagePlus display(byte[][] voxels, int width, int height, String name)
    {
        ImagePlus image = makeImage(voxels, width, height, name);
        image.show();
        image.updateAndDraw();
        return image;
    }
    
    public static byte[][] getStackByteVoxels(ImageStack stack)
    {
        int zSize = stack.getSize();
        
        byte[][] voxels = new byte[zSize][];
        for (int i=0; i<zSize; i++)
        {
            voxels[i] = (byte[])stack.getProcessor(i+1).getPixels();
        }
        return voxels;
    }
}

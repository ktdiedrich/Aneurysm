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
import ij.process.*;
import ij.text.TextWindow;


/** 3D stack rotations 
 * @author Karl Diedrich <ktdiedrich@gmail.com> */
public class Rotate3D
{
    public static short Flip_Z = 0;
    public static short Z_UP = 1;
    private TextWindow _twin;
    private Rotate3D()
    {

    }

    /** Flip an image with a stack in the Z direction and horizontally. */
    public static ImagePlus flipZ(ImagePlus image)
    {
    	ImageStack stack = flipZ(image.getStack());
    	ImagePlus flipped = new ImagePlus(image.getShortTitle()+"Flipped", stack);
    	return flipped;
    }
    /**  */
    
    
    /** Flip stack in Z direction and horizontally. */
    public static ImageStack flipZ(ImageStack stack)
    {
    	int zSize = stack.getSize();
        int height = stack.getHeight();
        int width = stack.getWidth();
        ImageStack flipped = new ImageStack(width, height);
        int i = 1;
        for (int z=zSize; z >= 1; z--) 
        {
            ImageProcessor sliceProc = stack.getProcessor(z);
            sliceProc.flipHorizontal();
            flipped.addSlice(""+i, sliceProc);
            i++;
        }
        return flipped;
    }
    public static ImagePlus rotateZupY(ImagePlus image)
    {
    	ImageStack stack = image.getImageStack();
    	int zSize = stack.getSize();
    	int width = stack.getWidth();
    	int height = stack.getHeight();
    	int cols = width;
    	int rows = zSize;
    	int depth = height;
    	
    	ImageProcessor proc1 = stack.getProcessor(1);
    	ImageStack upStack = new ImageStack(cols, rows);
    	
    	
    	if (proc1 instanceof ShortProcessor)
    	{
    		for (int d=0; d < depth; d++)
        	{
        		upStack.addSlice(""+d, new ShortProcessor(cols, rows));
        	}
    		short[][] voxels = ImageProcess.getShortStackVoxels(stack);
    		short[][] upVoxels = ImageProcess.getShortStackVoxels(upStack);
    		for (int d=0; d < depth; d++)
    		{
    			for (int r=0; r < rows; r++)
    			{
    			    for (int c=0; c < cols; c++)
    				{
    					short val = voxels[r][d*width+c];
    					upVoxels[d][r*width+c] = val;
    				}
    			}
    		}
    	}
    	else if (proc1 instanceof ColorProcessor)
    	{
    		
    		ImageProcessor[] procs = new ImageProcessor[zSize];
    		for (int z=0; z < zSize; z++)
    		{
    			procs[z] = stack.getProcessor(z+1);
    		}
    		ImageProcessor[] upProcs = new ImageProcessor[depth];
    		for (int d=0; d < depth; d++)
        	{
    			ImageProcessor p = new ColorProcessor(cols, rows);
        		upStack.addSlice(""+d, p);
        		upProcs[d] = p;
        	}
    		int[] rgbPix = new int[3];
    		for (int d=0; d < depth; d++)
    		{
    			for (int r=0; r < rows; r++)
    			{
    			    for (int c=0; c < cols; c++)
    				{
    					int[] val = procs[r].getPixel(c, d, rgbPix);
    					upProcs[d].putPixel(c, r, val);
    				}
    			}
    		}
    	}
    	else if (proc1 instanceof FloatProcessor)
    	{
    		for (int d=0; d < depth; d++)
        	{
        		upStack.addSlice(""+d, new FloatProcessor(cols, rows));
        	}
    		float[][] voxels = ImageProcess.getFloatStackVoxels(stack);
    		float[][] upVoxels = ImageProcess.getFloatStackVoxels(upStack);
    		for (int d=0; d < depth; d++)
    		{
    			for (int r=0; r < rows; r++)
    			{
    			    for (int c=0; c < cols; c++)
    				{
    					float val = voxels[r][d*width+c];
    					upVoxels[d][r*width+c] = val;
    				}
    			}
    		}
    	}
    	else if (proc1 instanceof ByteProcessor)
    	{
    		for (int d=0; d < depth; d++)
        	{
        		upStack.addSlice(""+d, new ByteProcessor(cols, rows));
        	}
    		byte[][] voxels = ImageProcess.getByteStackVoxels(stack);
    		byte[][] upVoxels = ImageProcess.getByteStackVoxels(upStack);
    		for (int d=0; d < depth; d++)
    		{
    			for (int r=0; r < rows; r++)
    			{
    			    for (int c=0; c < cols; c++)
    				{
    					byte val = voxels[r][d*width+c];
    					upVoxels[d][r*width+c] = val;
    				}
    			}
    		}
    	}
    	   
    	ImagePlus up = new ImagePlus(image.getShortTitle()+"Zup", upStack);
    	return up;
    }

    public TextWindow getTwin() {
        return _twin;
    }

    public void setTwin(TextWindow twin) {
        _twin = twin;
    }
}

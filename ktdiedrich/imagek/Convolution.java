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

import ij.ImageStack;
import ij.process.ImageProcessor;

/** Image convolution kernels and functions. 
 * Kernels are in row major order 
 * @author ktdiedrich@gmail.com
 * */
public class Convolution 
{
	public static final int[] PREWITT_KERNEL_3X3_X = {	-1, 0, 1, 
	  													-1, 0, 1, 
	  													-1, 0, 1};
	
	public static final int[] PREWITT_KERNEL_3X3_Y = {	1,   1,  1, 
													  	0,   0,  0, 
													   -1,  -1, -1};
	public static final int[] PREWITT_KERNEL_3X3_Z = {	-1, 0, 1, 
													  	-1, 0, 1, 
													  	-1, 0, 1};
	
	public static final int[] SOBEL_KERNEL_3X3_X   = {	-1, 0, 1, 
														-2, 0, 2, 
														-1, 0, 1};
	public static final int[] SOBEL_KERNEL_3X3_Y   = {	 1,  2,  1, 
														 0,  0,  0, 
														-1, -2, -1};
	//public static final int[] SOBEL_KERNEL_3X3_Z   = {	 1,  2,  1, 
	//													 0,  0,  0, 
	//													-1, -2, -1};
	public static final int[] SOBEL_KERNEL_3X3_Z   = {	-1, 0, 1, 
														-2, 0, 2, 
														-1, 0, 1};
	
	/** Gradient of image using Sobel operators in 3-D */
	public static void sobelGradient3x3short3D(ImageStack stack)
	{
		ImageStack dupY = ImageProcess.duplicate(stack);
		ImageStack dupZ  = ImageProcess.duplicate(stack);
		// stack used for X-gradient 
		convolve3x3Short(stack, SOBEL_KERNEL_3X3_X);
		convolve3x3Short(dupY, SOBEL_KERNEL_3X3_Y);
		convolveZ3x3Short(dupZ, SOBEL_KERNEL_3X3_Z);
		ImageProcess.add(stack, dupY);
		ImageProcess.add(stack, dupZ);
	}
	
	/** Gradient of image using Prewitt operators in 3-D */
	public static void prewittGradient3x3short3D(ImageStack stack)
	{
		ImageStack dupY = ImageProcess.duplicate(stack);
		ImageStack dupZ  = ImageProcess.duplicate(stack);
		// stack used for X-gradient 
		convolve3x3Short(stack, PREWITT_KERNEL_3X3_X);
		convolve3x3Short(dupY, PREWITT_KERNEL_3X3_Y);
		convolveZ3x3Short(dupZ, PREWITT_KERNEL_3X3_Z);
		ImageProcess.add(stack, dupY);
		ImageProcess.add(stack, dupZ);
	}
	public static void convolve3x3Short(ImageStack stack, int[] kernel)
	{
		for (int i=1; i <= stack.getSize(); i++)
		{
			convolve3x3Short(stack.getProcessor(i), kernel);
		}
	}
	
	/** Convolve the Image with the 3x3 row major order kernel taking the absolute value of the
	 * convolution result at each pixel. Starts and ends one pixel in the the edge of the image.  */
	public static void convolve3x3Short(ImageProcessor ip, int[] kernel)
	{
		ImageProcessor ipDup = ip.duplicate();
		short[] dupVoxels = (short[])ipDup.getPixels();
		short[] voxels = (short[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		int kw = 3;
		int kh = 3;
		for (int c=1; c < width-1; c++)
		{
			for (int r=1; r < height-1; r++)
			{
				int val = 0;
				for(int kcol=0, x=-1; kcol < kw ; kcol++, x++)
				{
					for(int krow=0, y=-1; krow < kh; krow++, y++)
					{
						int col = c+x;
						int row = r+y;
						short v = dupVoxels[row*width+col];
						short k = (short)kernel[krow*kw+kcol];
						val +=  v*k ;
					}
				}
				// voxels[r*width+c] = (short)Math.abs(val);
				voxels[r*width+c] = (short)val;
			}
		}
	}
	/** Convolve the Image in the Z plane with the 3x3 row major order kernel taking the 
	 * absolute value of the convolution result at each pixel. 
	 * Starts and ends one pixel in the the edge of the image.  Set z=0 and z= zSize-1 plane to 0 value,
	 * this is useful when adding X + Y + Z gradient image stacks together. If not the 0 and zSize-1 Z planes
	 * have the original values. */
	public static void convolveZ3x3Short(ImageStack stack, int[] kernel)
	{
		int width = stack.getWidth();
		int height = stack.getHeight();
		int zSize = stack.getSize();
		
		ImageStack dupStack = new ImageStack(width, height);
		short[][] voxels = new short[zSize][];
		short[][] dupVoxels = new short[zSize][];
		for (int i = 0; i < zSize; i++)
		{
			ImageProcessor ip = stack.getProcessor(i+1);
			ImageProcessor ipDup = ip.duplicate();
			dupStack.addSlice(""+i, ipDup);
			dupVoxels[i] = (short[])ipDup.getPixels();
			voxels[i] = (short[])ip.getPixels();
		}
		
		
		int kw = 3;
		int kh = 3;
		
		for (int c=1; c < width-1; c++)
		{			
			for (int r=1; r < height-1; r++)
			{
				for (int z=1; z < zSize-1; z++)
				{
					int val = 0;
					for(int kcol=0, x=-1; kcol < kw ; kcol++, x++)
					{
						for(int krow=0, y=-1; krow < kh; krow++, y++)
						{
							int col = c;
							int row = r+y;
							int zDepth = z+x;
							short v = dupVoxels[zDepth][row*width+col];
							short k = (short)kernel[krow*kw+kcol];
							val +=  v*k ;
						}
					}
					voxels[z][r*width+c] = (short)Math.abs(val);
				}
				voxels[0][r*width+c] = 0;
				voxels[zSize-1][r*width+c] = 0;
			}
		}
	}
	
	private Convolution()
	{
		
	}
	
}

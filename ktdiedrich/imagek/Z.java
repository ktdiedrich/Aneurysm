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
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** Z Direction of image stacks. */
public class Z 
{
	public Z()
	{
		
	}
	public ImagePlus fillShort(ImagePlus image, int fillLayers)
	{
		ImageStack stack = fillShort(image.getStack(), fillLayers);
		ImagePlus filledImage = new ImagePlus();
		filledImage.setStack(image.getShortTitle()+"Z_filled", stack);
		return filledImage;
	}
	/** Fill in between layers of a stack. Interpolate between image intensities. */
	public ImageStack fillShort(ImageStack stack, int fillLayers)
	{
		int slices = stack.getSize();       
		int height = stack.getHeight();
        int width = stack.getWidth();
        ImageStack filledStack = new ImageStack(width, height);
		
        ImageProcessor prevProc = stack.getProcessor(2);
        short[] prevPixels = (short[])prevProc.getPixels();
        int sliceNum = 1;
        filledStack.addSlice(""+sliceNum, prevProc);
        
        for (int i=2; i <= slices; i++)
        {
        	sliceNum++;
            ImageProcessor curProc = stack.getProcessor(i);
            short[] curPixels = (short[])curProc.getPixels();
            filledStack.addSlice(""+sliceNum, curProc);
            
            ImageProcessor[] fillProcs = new ImageProcessor[fillLayers];
            short[][] fillPixels = new short[fillLayers][];
            for (int j=0; j < fillLayers; j++)
            {
            	sliceNum++;
            	ImageProcessor fillProc = new ShortProcessor(width, height); 
            	fillProcs[j] = fillProc;
            	fillPixels[j] = (short[])fillProc.getPixels();
            	filledStack.addSlice(""+sliceNum, fillProc);
            }
            
            for (int x=0; x < width; x++)
            {
            	for (int y=0; y < height; y++)
            	{
            		short prevPixel = prevPixels[y*width+x];
            		short curPixel = curPixels[y*width+x];
            		if (prevPixel > 0 && curPixel > 0)
            		{
            			short range = (short)Math.abs(curPixel-prevPixel);
            			double step = (double)range/(fillLayers+1);
            			if (prevPixel > curPixel)
            				step = -step;
            			double val = (double)prevPixel;
            			for (int k=0; k < fillLayers; k++)
            			{
            				val+=step;
            				fillPixels[k][y*width+x] = (short)Math.round(val);
            			}
            		}
            	}
            }
            
            prevPixels = curPixels;
        }
        
        return filledStack;
	}
}

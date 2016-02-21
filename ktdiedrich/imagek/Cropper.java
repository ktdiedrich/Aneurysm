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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;


/** Crop images. 
 * @author ktdiedrich@gmail.com */
public class Cropper 
{
	public Cropper()
	{
		
	}
	public static ImagePlus crop3d(ImagePlus image, int minX, int maxX, int minY, int maxY, int minZ, int maxZ)
	{
		ImageStack stack = crop3d(image.getImageStack(), minX, maxX, minY, maxY, minZ, maxZ);
		if (stack.getSize() > 0)
		{
			ImagePlus cropped = new ImagePlus(image.getShortTitle()+"Crop", stack);
			return cropped;
		}
		else return null;
	}
	public static ImageStack crop3d(ImageStack stack, int minX, int maxX, int minY, int maxY, int minZ, int maxZ)
	{
		int cw = maxX-minX;
		int ch = maxY-minY;
		IJ.log("Input stack width: "+stack.getWidth()+" height: "+stack.getHeight());
		IJ.log("Stack width: "+cw+" height: "+ch);
		ImageStack cropped = new ImageStack(cw, ch);
		int zSize = stack.getSize();
		if (maxZ >= zSize) maxZ = zSize-1;
		for (int z=minZ; z < maxZ; z++)
		{
			
			ImageProcessor ip = stack.getProcessor(z+1);
			ip.setRoi(minX, minY, cw, ch);
			ImageProcessor croppedIp = ip.crop();
			int w = croppedIp.getWidth();
			int h = croppedIp.getHeight();
			
			if (w==cw && h==ch)
			{
				cropped.addSlice(z+"", croppedIp);
			}
			else
			{
				IJ.log("Cropped width: "+w+" height: "+h+" doesn't match: "+cw+" x "+ch+" ");
			}
			
		}
		return cropped;
	}
}

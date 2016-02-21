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

import java.util.List;

/** Dot enhance the image to detect aneurysms. Based on the paper: 
 * 1. Uchiyama, Y. et al. Computer-Aided Diagnosis Scheme for Detection of Unruptured Intracranial Aneurysms in MR Angiography. Conf Proc IEEE Eng Med Biol Soc 3, 3031-4(2005). 
 * @author ktdiedrich@gmail.com
 * */
public class DotEnhance 
{
	private List<Graph> _imageGraphs;
	private ImagePlus _image;
	private float _xRes, _yRes, _zRes;
	private int _width, _height, _zSize;
	/** @param image Segmented short arterial image. */
	public DotEnhance(List<Graph> imageGraphs, ImagePlus image, float xRes, float yRes, float zRes)
	{
		_imageGraphs = imageGraphs;
		_image = image;
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
		_width = image.getWidth();
		_height = _image.getHeight();
		_zSize = _image.getStackSize();
	}
	public ImagePlus makeEnhancedImage()
	{
		ImageStack enStack = new ImageStack(_width, _height);
		short[][] enVoxels = new short[_zSize][];
		
		for (int i=0; i < _zSize; i++)
		{
			ImageProcessor enProc = new ShortProcessor(_width, _height);
			enVoxels[i] = (short[])enProc.getPixels();
		}
		// TODO dot enhance image 
		
		
		ImagePlus enImage = new ImagePlus(_image.getShortTitle()+"Dot", enStack);
		
		return enImage;
	}
}

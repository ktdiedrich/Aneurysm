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

import java.util.List;

/** Thread to Dot enhance the image to detect aneurysms. 
 * @author ktdiedrich@gmail.com
 * */
public class DotEnhanceThread extends Thread 
{
	private List<Graph> _imageGraphs;
	private ImagePlus _image;
	private float _xRes, _yRes, _zRes;
	public DotEnhanceThread(List<Graph> imageGraphs, ImagePlus image, float xRes, float yRes, float zRes)
	{
		_imageGraphs = imageGraphs;
		_image = image;
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
	}
	public void run()
	{
		DotEnhance dot = new DotEnhance(_imageGraphs, _image, _xRes, _yRes, _zRes);
	}
}

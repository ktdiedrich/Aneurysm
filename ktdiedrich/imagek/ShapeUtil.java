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

/** Drawing shapes on an image  
 * */
public class ShapeUtil 
{
    public static final short INTENSITY = 80;
   // private short[][] _voxels;
    private ImagePlus _image;
    private int _intensity;

	public ShapeUtil(ImagePlus image)
    {
    	_intensity = INTENSITY;
    	_image = image;
    }

	public int getIntensity() 
	{
		return _intensity;
	}
	public void setIntensity(int intensity) 
	{
		_intensity = intensity;
	}

    /** Draw a sphere around the point (a, b, c) . 
     * @param a x coordinate center point.
     * @param b y coordinate center point.
     * @param c z coordinate center point. 
     * @param radius radius of the sphere. 
     * */
    public void surroundSolidSphere(int a, int b, int c, int radius)
    {
    	int width = _image.getWidth();
    	ImageStack stack = _image.getImageStack();
    	
    	double q=0.0, p=0.0, x = 0.0, y=0.0, z=0.0;
    	double step = 1.0/1000.0*Math.PI;
    	for (q=0.0; q < Math.PI; q+=step)
	    {
    		for (p=0.0; p < 2*Math.PI; p+=step)
		    {
    			for (int r=0; r <=radius; r++)
    			{
    				x = r * Math.cos(q) * Math.sin(p) + a;
    				y = r * Math.sin(q) * Math.sin(p) + b;
    				z = r * Math.cos(p) + c;
    				// int i = (int)Math.round(y) * width + (int)Math.round(x);
    				// _voxels[(int)Math.round(z)][i] = _intensity;
    				stack.getProcessor((int)Math.round(z)).putPixel((int)Math.round(x), (int)Math.round(y), _intensity);
			    }
		    }
	    }
    }
}

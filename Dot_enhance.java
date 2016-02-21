/*=========================================================================
 *
 *  Copyright (c)   Karl T. Diedrich 
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

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.imagek.*;

/** Enhance dots in a an image stack.  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Dot_enhance  implements PlugInFilter 
{
    private ImagePlus _imp;
    
    public int setup(String arg, ImagePlus imp) 
	{
	    this._imp = imp;
	    //return DOES_ALL+STACK_REQUIRED;
	    return DOES_ALL;
    }
    
    public void run(ImageProcessor ip) 
    {
	    
	    /*
	    GenericDialog gd = new GenericDialog("Shaded_Surface");
	   
	    gd.showDialog();
        if (gd.wasCanceled()) 
        {
            IJ.error("PlugIn canceled!");
            return;
        }
	    
	    */
    	
    	
	    
    	ImageStack inputStack = _imp.getImageStack();
    	int width = inputStack.getWidth();
	    int height = inputStack.getHeight();
	    int zSize = inputStack.getSize();
	    
	    // Convolution.prewittGradient3x3short3D(_inputStack);
	    //Convolution.convolveZ3x3Short(inputStack, Convolution.PREWITT_KERNEL_3X3_Z);
	    //Convolution.convolveZ3x3Short(inputStack, Convolution.SOBEL_KERNEL_3X3_Z);
    	
	    //Convergence2D convergence2D = new Convergence2D(ip);
	    //convergence2D.filter();
	    Convergence3D con3D = new Convergence3D(_imp);
	    con3D.filter();
	    _imp.updateAndDraw();
    	
    	
    }
}

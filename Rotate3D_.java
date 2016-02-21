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
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.GenericDialog;
import ktdiedrich.imagek.*;
/**  3D Rotations of stacks   
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Rotate3D_ implements PlugInFilter 
{
    ImagePlus imp;
    
    public int setup(String arg, ImagePlus imp) 
	{
	    this.imp = imp;
	    return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        GenericDialog gd = new GenericDialog("Rotate3D");
        String[] rotations = {"Flip Z", "Z up"};
        gd.addChoice("Rotation", rotations, "Flip Z");
        gd.showDialog();
        if (gd.wasCanceled()) 
        {
            IJ.error("PlugIn canceled!");
            return;
        }
        String rotationName = gd.getNextChoice();
        //TextWindow twin = new TextWindow("Rotate3D", rotationName, 400,400);
    	short rotationType = Rotate3D.Flip_Z;
    	if (rotationName.equals("Flip Z"))
    	    rotationType = Rotate3D.Flip_Z;
    	else if (rotationName.equals("Z up"))
    	    rotationType = Rotate3D.Z_UP;
    	
    	
    	
    	ImagePlus rotatedImage = null;
    	//rotate3D.setTwin(twin);
    	if (rotationType == Rotate3D.Flip_Z)
    	{
    		rotatedImage = Rotate3D.flipZ(imp);
    	}
    	else if (rotationType == Rotate3D.Z_UP)
    	{
    		rotatedImage = Rotate3D.rotateZupY(imp);
    	}
    	    
    	if (rotatedImage != null)
    	{
    		rotatedImage.show();
	        rotatedImage.updateAndDraw();
    	}
    }
}

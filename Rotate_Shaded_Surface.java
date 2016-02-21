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

import java.awt.Checkbox;
import java.util.Vector;

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.imagek.*;
/** Make a rotating shaded surface image stack. 
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Rotate_Shaded_Surface implements PlugInFilter 
{
    private ImagePlus _imp;
    
    public int setup(String arg, ImagePlus imp) 
	{
	    this._imp = imp;
	    return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {   
	    GenericDialog gd = new GenericDialog("Rotate_Shaded_Surface");
	    String[] axises = {"X axis", "Y axis", "Z axis"};
	    gd.addChoice("Rotate axis", axises, "Y axis");
	    gd.addNumericField("Degrees", 10.0, 1);
	    
	    gd.showDialog();
        if (gd.wasCanceled()) 
        {
            IJ.error("PlugIn canceled!");
            return;
        }
        
        String rotationName = gd.getNextChoice();
	    short rotationAxis = Free3DRotation.Y_AXIS;
    	if (rotationName.equals("X axis"))
    	    rotationAxis = Free3DRotation.X_AXIS;
    	else if (rotationName.equals("Z axis"))
    	    rotationAxis = Free3DRotation.Z_AXIS;
    	
	    double degrees = gd.getNextNumber();
	    ShadedSurface shadedSurf = new ShadedSurface();
	    
	    ImagePlus shadedIm = shadedSurf.rotateShadedSurface(_imp, degrees, rotationAxis);
	    
        shadedIm.show();
        shadedIm.updateAndDraw();
    }
}

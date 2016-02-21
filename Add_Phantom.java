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
import ij.IJ;

/** Add a phantom tumor to an existing object.   
*/
public class Add_Phantom implements PlugInFilter 
{
    private ImagePlus _imp;
    public int setup(String arg, ImagePlus imp) 
    {
	this._imp = imp;
	return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
    	int w = _imp.getWidth();
    	int h = _imp.getHeight();
    	int s = _imp.getStackSize();
    	
        GenericDialog gd = new GenericDialog("Add phantom");
        gd.addNumericField("x", w/2, 0);
        gd.addNumericField("y", h/2, 0);
        gd.addNumericField("z", s/2, 0);
        gd.addNumericField("r", 10, 0);
        gd.addNumericField("intensity", 33000, 0);
	
        gd.showDialog();
        if (gd.wasCanceled()) 
        {
            IJ.error("PlugIn canceled!");
            return;
        }
        
        int x = (int)gd.getNextNumber();
        int y = (int)gd.getNextNumber();
        int z = (int)gd.getNextNumber();
        int r = (int)gd.getNextNumber();
        int intensity  = (int)gd.getNextNumber();
        
        
        ShapeUtil shapeUtil = new ShapeUtil(_imp);
        shapeUtil.setIntensity(intensity);
        shapeUtil.surroundSolidSphere(x, y, z, r);
        _imp.updateAndDraw();
    }
    
}

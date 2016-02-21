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

import java.util.LinkedList;
import java.util.List;

import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.GenericDialog;
import ktdiedrich.imagek.*;
/** Maximum Intensity Projection Z-Buffer  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class MIP_ implements PlugInFilter 
{
    ImagePlus imp;
    
    public int setup(String arg, ImagePlus imp) 
	{
	    this.imp = imp;
	    return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        ImageStack stack = imp.getStack();

        GenericDialog gd = new GenericDialog("MIP");
        String[] axises = {"X", "Y", "Z", "X_Y_Z"};
        gd.addChoice("Axis", axises, "X_Y_Z");
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        String axis = gd.getNextChoice();
    	short axisType = MIP.Z_AXIS;
    	if (axis.equals("X"))
    	    axisType = MIP.X_AXIS;
    	if (axis.equals("Y"))
    	    axisType = MIP.Y_AXIS;
    	if (axis.equals("X_Y_Z"))
    	    axisType = MIP.XYZ_AXIS;
    	
    	ImageProcessor proc1 = stack.getProcessor(1);
    	List<ImagePlus> mips = new LinkedList<ImagePlus>();
    	
    	if (proc1 instanceof ShortProcessor)
    	{
    		if (axisType == MIP.XYZ_AXIS)
    		{
    			mips.add(MIP.createShortMIP(imp, MIP.X_AXIS));
    			mips.add(MIP.createShortMIP(imp, MIP.Y_AXIS));
    			mips.add(MIP.createShortMIP(imp, MIP.Z_AXIS));
    		}
    		else
    		{
    			mips.add(MIP.createShortMIP(imp, axisType));
    		}
    	}
    	if (proc1 instanceof FloatProcessor)
    	{
    		if (axisType == MIP.XYZ_AXIS)
    		{
    			mips.add(MIP.createFloatMIP(imp, MIP.X_AXIS));
    			mips.add(MIP.createFloatMIP(imp, MIP.Y_AXIS));
    			mips.add(MIP.createFloatMIP(imp, MIP.Z_AXIS));
    		}
    		else
    		{
    			mips.add(MIP.createFloatMIP(imp, axisType));
    		}
    	}
    	if (proc1 instanceof ByteProcessor)
    	{
    		if (axisType == MIP.XYZ_AXIS)
    		{
    			mips.add(MIP.createByteMIP(imp, MIP.X_AXIS));
    			mips.add(MIP.createByteMIP(imp, MIP.Y_AXIS));
    			mips.add(MIP.createByteMIP(imp, MIP.Z_AXIS));
    		}
    		else
    		{
    			mips.add(MIP.createByteMIP(imp, axisType));
    		}
    	}
    	if (proc1 instanceof ColorProcessor)
    	{
    		if (axisType == MIP.XYZ_AXIS)
    		{
    			mips.add(MIP.createColorMIP(imp, MIP.X_AXIS));
    			mips.add(MIP.createColorMIP(imp, MIP.Y_AXIS));
    			mips.add(MIP.createColorMIP(imp, MIP.Z_AXIS));
    		}
    		else
    		{
    			mips.add(MIP.createColorMIP(imp, axisType));
    		}
    	}
    	
    	for (ImagePlus m : mips) 
    	{
	        m.show();
	        m.updateAndDraw();
        }
    }
}

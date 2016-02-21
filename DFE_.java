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

import java.io.IOException;

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.imagek.*;
import ktdiedrich.util.TempProperties;
/** Distance From Edge (DFE)  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class DFE_ implements PlugInFilter 
{
    private ImagePlus _imp;
    
    public int setup(String arg, ImagePlus imp) 
	{
	    this._imp = imp;
	    return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
    	float xres = 1, yres = 1, zres = 1;
    	try 
		{
			TempProperties tp = new TempProperties(TempProperties.ANERUYSM_TEMP);
			xres = Float.parseFloat(tp.getProperty("xres"));
			yres = Float.parseFloat(tp.getProperty("yres"));
			zres = Float.parseFloat(tp.getProperty("zres"));
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
	    
	    GenericDialog gd = new GenericDialog("Distance_From_Edge");
        gd.addNumericField("X Resolution", xres, 4);
        gd.addNumericField("Y Resolution", yres, 4);
        gd.addNumericField("Z Resolution", zres, 4);
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        
        float xRes = (float)gd.getNextNumber();
        float yRes = (float)gd.getNextNumber();
        float zRes = (float)gd.getNextNumber();
        DistanceFromEdge dfer = new DistanceFromEdge(xRes, yRes, zRes);
        
        short[][] dfe = dfer.distanceFromEdge(_imp); 
        int width = _imp.getWidth();
        int height = _imp.getHeight();
        ImageProcess.display(dfe, width, height, _imp.getShortTitle()+"DFE");
        CoolHotColor colorDFE = new CoolHotColor(dfe, width, height, 
                _imp.getShortTitle()+"DFE");
        ImagePlus colorDFEim = colorDFE.getImage();
        colorDFEim.show();
        colorDFEim.updateAndDraw();
    }
}

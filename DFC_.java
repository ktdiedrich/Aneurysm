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
import java.util.List;

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.imagek.*;
import ktdiedrich.util.TempProperties;
/** Distance From Centerline (DFC) of the image stack and colorizes cool blue to hot red.   
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class DFC_ implements PlugInFilter 
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
    	float dfeThreshold = 0.5f;
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
		if (zres > 0)
		{
			dfeThreshold = zres;
			if (xres > dfeThreshold) dfeThreshold = xres;
			if (yres > dfeThreshold) dfeThreshold = yres;
		}
	    
	    GenericDialog gd = new GenericDialog("Distance_From_Centerline");
        gd.addNumericField("Distance From Edge threshold", dfeThreshold, 1);
        gd.addNumericField("Minimum line length", 30, 0);
        gd.addNumericField("X Resolution", xres, 8);
        gd.addNumericField("Y Resolution", yres, 8);
        gd.addNumericField("Z Resolution", zres, 8);
        gd.addNumericField("Weight A", Centerlines.A, 1);
        gd.addNumericField("Weight b", Centerlines.B, 1);
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        
        dfeThreshold = (float)gd.getNextNumber();
        int minLineLen = (int)gd.getNextNumber();
        xres = (float)gd.getNextNumber();
        yres = (float)gd.getNextNumber();
        zres = (float)gd.getNextNumber();
        float A = (float)gd.getNextNumber();
        float b = (float)gd.getNextNumber();
        
        Centerlines center = new Centerlines();
        center.setDfeThreshold(dfeThreshold);
        center.setMinLineLength(minLineLen);
        center.setXRes(xres);
        center.setYRes(yres);
        center.setZRes(zres);
        center.setA(A);
        center.setB(b);
        center.findCenterlines(_imp);
        
        List<CenterlineGraph> imageCenterlineGraphs = center.getCenterlineGraphs();
        List<Graph> imageGraphs = center.getImageGraphs();
        
        DistanceFromCenterline dtc = new DistanceFromCenterline(xres, yres, zres);
        dtc.setNodesDFC(imageCenterlineGraphs, imageGraphs, _imp.getStackSize());
        dtc.enhanceAneurysm(imageCenterlineGraphs, _imp.getStackSize());
        ImagePlus dfcImage = DistanceFromCenterline.makeDFCImage(imageGraphs, _imp.getWidth(), _imp.getHeight(), 
        		_imp.getStackSize(), _imp.getShortTitle()+"DFC");
        
        CoolHotColor coolHotColor = new CoolHotColor(dfcImage);
        dfcImage = coolHotColor.getImage();
        Colors colors = Colors.getColors();
        Overlay.overlayCenterlineOnColorImage(dfcImage, imageCenterlineGraphs, colors.centerline);
        dfcImage.show();
        dfcImage.updateAndDraw();
    }
}


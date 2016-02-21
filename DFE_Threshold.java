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

import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import ktdiedrich.imagek.CenterOfMass;
import ktdiedrich.imagek.Centerlines;
import ktdiedrich.imagek.ConnectedGraph;
import ktdiedrich.imagek.DistanceFromEdge;
import ktdiedrich.imagek.Graph;
import ktdiedrich.imagek.GraphNode;
import ktdiedrich.imagek.ImageProcess;
import ktdiedrich.imagek.MIP;
import ktdiedrich.imagek.Threshold;
import ktdiedrich.imagek.VoxelDistance;
import ktdiedrich.util.TempProperties;
import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.io.*;

/** Threshold the image by Distance From Edge  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class DFE_Threshold implements PlugInFilter {
    private ImagePlus _imp;
    public int setup(String arg, ImagePlus imp) 
    {
        _imp = imp;
        //return DOES_16+STACK_REQUIRED;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
    	float xres = 0;
    	float yres = 0;
    	float zres = 0;
    	float dfeThreshold = 0.0f;
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
        GenericDialog gd = new GenericDialog("DFE_Threshold");
        gd.addNumericField("Distance From Edge threshold", 0.3, 1);
        gd.addNumericField("X Resolution", xres, 8);
        gd.addNumericField("Y Resolution", yres, 8);
        gd.addNumericField("Z Resolution", zres, 8);
        
        
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        
        dfeThreshold = (float)gd.getNextNumber();
        xres = (float)gd.getNextNumber();
        yres = (float)gd.getNextNumber();
        zres = (float)gd.getNextNumber();
        
        
        ImageStack segmentationStack = _imp.getImageStack();
        int width = segmentationStack.getWidth();
        int height = segmentationStack.getHeight();
        int zSize = segmentationStack.getSize();
        
        short[][] inputVoxels = ImageProcess.getShortStackVoxels(segmentationStack);
        //IJ.log("Got input voxels");
        DistanceFromEdge dfer = new DistanceFromEdge(xres, yres, zres);
        short[][] dfes = dfer.distanceFromEdge(inputVoxels, width, height);
        //IJ.log("Create DFE, threshold: "+dfeThreshold);
        if (dfeThreshold > 0)
        {
            Threshold.thresholdUnder(dfes, (short)Math.round(dfeThreshold*VoxelDistance.DISTANCE_PRECISION));
        }
        for (int z=0; z < zSize; z++)
        {
        	for (int i=0; i < dfes[0].length; i++)
        	{
        		if (dfes[z][i] == 0)
        		{
        			inputVoxels[z][i] = 0;
        		}
        	}
        }
        
        _imp.updateAndDraw();
        IJ.log("Updated image");
    }
   

}



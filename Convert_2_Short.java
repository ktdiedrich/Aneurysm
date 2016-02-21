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

import ktdiedrich.imagek.Centerlines;
import ktdiedrich.imagek.ImageProcess;
import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/** Find centerlines from a 3-D image stack  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Convert_2_Short implements PlugInFilter {
    protected ImagePlus _imp;
    
    public int setup(String arg, ImagePlus imp) 
    {
        _imp = imp;
        return DOES_32+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
    	 GenericDialog gd = new GenericDialog("Convert float to short");
         gd.addNumericField("Multiply accuracy", 1, 0);
         
         
         
         gd.showDialog();
         if (gd.wasCanceled()) {
             IJ.error("PlugIn canceled!");
             return;
         }
         double accuracy = gd.getNextNumber();
         IJ.log("Conversion accuracy: "+accuracy);
         
    	ImageStack stack = _imp.getImageStack();
    	// TODO negative values in some float images 
    	float[][] fVoxels = ImageProcess.getFloatStackVoxels(stack);
    	int zLen = fVoxels.length;
    	int iLen = fVoxels[0].length;
    	short[][] sVoxels = new short[zLen][iLen];
    	float min = Float.MAX_VALUE;
    	for (int z=0; z < zLen; z++)
    	{
    		for (int i=0; i < iLen; i++)
    		{
    			float pix = fVoxels[z][i];
    			if (pix < min)
    			{
    				min = pix;
    			}
    		}
    	}
    	if (min >= 0)
    	{
    		for (int z=0; z < zLen; z++)
    		{
    			for (int i=0; i < iLen; i++)
    			{
    				sVoxels[z][i] = (short)Math.round(fVoxels[z][i]*accuracy);
    			}
    		}
    	}
    	else // min < 0; add min to each voxel to make the minimum 0 
    	{
    		for (int z=0; z < zLen; z++)
    		{
    			for (int i=0; i < iLen; i++)
    			{
    				sVoxels[z][i] = (short)Math.round((fVoxels[z][i]-min)*accuracy);
    			}
    		}
    	}
    	ImagePlus sImage = ImageProcess.makeImage(sVoxels, stack.getWidth(), stack.getHeight(), _imp.getShortTitle()+"Short");
    	sImage.show();
    	sImage.updateAndDraw();
    }
}


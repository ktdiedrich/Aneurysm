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

import ktdiedrich.imagek.ImageProcess;
import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/** Rewrite ITK centerline files with simple numbers, reduces errors later on. 
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class RewriteSimpleShort_ implements PlugInFilter {
    protected ImagePlus _imp;
    
    public int setup(String arg, ImagePlus imp) 
    {
        _imp = imp;
        return DOES_16+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
    	ImageStack stack = _imp.getImageStack();
    	
    	short[][] fVoxels = ImageProcess.getShortStackVoxels(stack);
    	int zLen = fVoxels.length;
    	int iLen = fVoxels[0].length;
    	short[][] sVoxels = new short[zLen][iLen];
    	int max = 0;
    	for (int z=0; z < zLen; z++)
    	{
    		for (int i=0; i < iLen; i++)
    		{
    			int p = 0xFFFF & fVoxels[z][i];
    			if (p > max)
    			{
    				max = p;
    			}
    		}
    	}
    	
    	for (int z=0; z < zLen; z++)
    	{
    		for (int i=0; i < iLen; i++)
    		{
    			int p = 0xFFFF & fVoxels[z][i];
    			if (p == max)
    			{
    				sVoxels[z][i] = (short)1;
    			}
    		}
    	}
    	ImagePlus sImage = ImageProcess.makeImage(sVoxels, stack.getWidth(), stack.getHeight(), _imp.getShortTitle()+"Short");
    	sImage.show();
    	sImage.updateAndDraw();
    }
}


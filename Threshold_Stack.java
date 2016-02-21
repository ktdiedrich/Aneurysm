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

/** Threshold a stack and region grow from the seed created by the threshold.
 * Useful for segmenting CTA images. 
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Threshold_Stack implements PlugInFilter {
    ImagePlus _imp;

    public int setup(String arg, ImagePlus imp) {
        _imp = imp;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        
        GenericDialog gd = new GenericDialog("Threshold_Stack");
        gd.addNumericField("Threhold seed below", 1250, 0);
        gd.addNumericField("Threshold seed above", 1400, 0);
        gd.addNumericField("Growing lower threshold", 1100, 0);
        gd.addNumericField("Growing upper threshold", 1500, 0);
        gd.addNumericField("Minimum 3-D cluster size", 100, 0);
        gd.addCheckbox("Show Steps", false);
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        short lower = (short)gd.getNextNumber();
        short upper = (short)gd.getNextNumber();
        short growingLower = (short)gd.getNextNumber();
        short growingUpper = (short)gd.getNextNumber();
        int clusterSize = (int)gd.getNextNumber();
        boolean showSteps = gd.getNextBoolean();
        
        ImageStack stack = _imp.getStack();
        ThresholdCluster thresholder = new ThresholdCluster();
        thresholder.setClusterSizeThreshold(clusterSize);
        thresholder.set3Dlower(growingLower);
        thresholder.set3Dupper(growingUpper);
        thresholder.setShowSteps(showSteps);
        ImagePlus thresClusIm = thresholder.thresholdClusterStack(stack, lower, upper);
        thresClusIm.show();
        thresClusIm.updateAndDraw();
        
    }

}


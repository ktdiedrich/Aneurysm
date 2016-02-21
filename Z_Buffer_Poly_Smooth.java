/*=========================================================================
 *
 *  Copyright (c) Karl T. Diedrich 
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

/** Keep smooth parts of an image like a Maximum Intensity Projection Z-Buffer  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Z_Buffer_Poly_Smooth implements PlugInFilter
{
    ImagePlus _imp;

    public int setup(String arg, ImagePlus imp) {
        this._imp = imp;
        return DOES_ALL;
    }
    
    public void run(ImageProcessor ip) 
    {      
        GenericDialog gd = new GenericDialog("ZBufferPolySmooth");
        gd.addNumericField("Filter Z difference",6.0, 0);
        gd.addNumericField("Minimum cluster size", 30.0, 0);
        gd.addNumericField("Max chi squared smoothness", 100.0, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        short zDiff = (short)gd.getNextNumber();
        int clusterMin = (int)gd.getNextNumber();
        int chisqMax = (int)gd.getNextNumber();
        
        ZBufferPolySmooth smoother = new ZBufferPolySmooth();
        smoother.setMaxChisq(chisqMax);
        smoother.setZDiff(zDiff);
        smoother.setClusterSize(clusterMin);
        
        ImagePlus smooth = smoother.smooth(_imp);
        smooth.show();
        smooth.updateAndDraw();
    }
}

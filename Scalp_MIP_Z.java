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

import java.util.List;

import ij.*;
import ij.gui.GenericDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.imagek.*;

/** Scalp the head and do a Maximum Intensity Projection Z-Buffer  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Scalp_MIP_Z implements PlugInFilter {
    ImagePlus _imp;

    public int setup(String arg, ImagePlus imp) {
        this._imp = imp;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        GenericDialog gd = new GenericDialog("ScalpMIPZ");
        gd.addNumericField("Threshold:",0,0);
        gd.addNumericField("Scalp depth", 40, 0);
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        int threshold = (int)gd.getNextNumber();
        int depth = (int)gd.getNextNumber();
        
        Scalper knife = new Scalper();
        knife.setInnerDist(depth);
        ImagePlus mip = MIP.createShortMIP(_imp, MIP.Z_AXIS);
        List<Edge> scalpEdges = knife.scalp(mip);
        
        ImagePlus mipz = MIPZUtil.createMIPZImage(_imp, threshold);
        ImageProcessor mipzScalpProc = Scalper.scalpEdges(mipz.getProcessor(), scalpEdges);
        ImagePlus scalpedMipZ = new ImagePlus(_imp.getShortTitle()+"ScalpedMIPZ", mipzScalpProc);
        scalpedMipZ.show();
        scalpedMipZ.updateAndDraw();       
    }

}


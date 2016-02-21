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

import ktdiedrich.imagek.TortuosityScore;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;

import ij.plugin.filter.*;
import ij.text.TextWindow;

/** Dimensionless tortuosity Score 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Tortuosity_Score implements PlugInFilter
{
    protected ImagePlus _imp;

    public int setup(String arg, ImagePlus imp) {
        this._imp = imp;
        return DOES_ALL;
    }
    
    public void run(ImageProcessor ip) 
    {       
        GenericDialog gd = new GenericDialog("Tortuosity_Score");
        gd.addNumericField("Border Distance From Edge", 2, 0);
        gd.addCheckbox("Show Intermediate steps", false);
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        int borderDFE = (int)gd.getNextNumber();
        boolean showSteps = gd.getNextBoolean();
        
        TortuosityScore scorer = new TortuosityScore();
        scorer.setBorderDFE(borderDFE);
        scorer.setShowSteps(showSteps);
        double score = scorer.score3D(_imp.getStack());
        new TextWindow("Normalized Tortuosity Score DFE: "+scorer.getBorderDFE(), 
                ""+score, 400, 200);
    }
}

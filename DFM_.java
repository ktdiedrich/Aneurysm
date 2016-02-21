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

import java.awt.Label;
import java.awt.Panel;

import ij.*;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.process.*;
import ij.plugin.filter.*;
import ij.text.TextWindow;
import ktdiedrich.imagek.*;

/** Distance Factor Metric Tortuosity Measurement 
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class DFM_ implements PlugInFilter {
    protected ImagePlus _imp;
    protected ImageCanvas _canvas;
    public int setup(String arg, ImagePlus imp) {
        _imp = imp;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        //DFMFromPointListener dfmp = new DFMFromPointListener(_imp);
        //dfmp.setMessageWindow(new TextWindow("DFM", "", 400, 400));
        GenericDialog gd = new GenericDialog("DFM");
        Panel directionsPanel = new Panel();
        directionsPanel.add(new Label(
        		"After pressing Okay, click mouse on the point to calculate Distance Factor Metric from."));
        gd.addPanel(directionsPanel);
        gd.showDialog();
        if (gd.wasCanceled()) 
        {
            IJ.error("PlugIn canceled!");
            return;
        }
       
       
    }
}



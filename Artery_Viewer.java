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

import javax.swing.JFrame;
import ij.*;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ktdiedrich.imagek.*;

/** Display a sub regioned image of the artery reading or writing the image location in aneurys.arterydisplay
 * database table. 
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Artery_Viewer implements PlugIn {
    protected ImagePlus _imp;
    protected ImageCanvas _canvas;
   
    public void run(String arg0) 
    {   
        JFrame rateFrame = RecordRatingPanel.makeRatingFrame();
        rateFrame.setVisible(true);
    }
}


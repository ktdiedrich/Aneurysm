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
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import ij.*;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.imagek.*;

/** Cut bone from image stack
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Bone_Cut implements PlugInFilter {
    protected ImagePlus _imp;
    protected ImageCanvas _canvas;
    public int setup(String arg, ImagePlus imp) {
        _imp = imp;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        BrainSegmentor seg = new BrainSegmentor();
        BrainCenterSelector centerSelector = new BrainCenterSelector(seg);
        _canvas = _imp.getCanvas();
        _canvas.addMouseListener(centerSelector);
        
        GenericDialog gd = new GenericDialog("BoneCut");
        gd.addNumericField("Bone edge threshold", 
                BrainSegmentor.EDGE_THRESHOLD, 0);
        gd.addNumericField("Cut in from edge", 
                BrainSegmentor.INNER_DIST, 0);
        gd.addCheckbox("Show Steps", false);
        Panel directionsPanel = new Panel();
        directionsPanel.add(new Label("After pressing Okay, click mouse in the brain cavity to segment."));
        gd.addPanel(directionsPanel);
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        short edgeThreshold = (short)gd.getNextNumber();
        int innerDist = (int)gd.getNextNumber();
        boolean showSteps = gd.getNextBoolean();
        
        ImageStack stack = _imp.getStack();
        
        seg.setEdgeThreshold(edgeThreshold);
        seg.setInnerDist(innerDist);
        seg.setShowSteps(showSteps);
        seg.setOrigStack(stack);
    }
    class BrainCenterSelector implements MouseListener, Runnable
    {
        BrainSegmentor _seg;
        protected boolean _enable;
        public BrainCenterSelector(BrainSegmentor seg )
        {
            _seg = seg;
            _enable = true;
        }
        public void mousePressed(MouseEvent e) 
        {
            
        }

         public void mouseReleased(MouseEvent e) 
         {
            Point pt = e.getPoint();
            System.out.println("Center: ["+pt.x+", "+pt.y+"]");
            _seg.setStartPoint(pt);
            if (_enable)
            {
                _enable = false;
                Thread thread = new Thread(this);
                thread.start();
            }
            else
            {
                System.out.println("Rerun plugin to find brain. ");
            }
         }

         public void mouseEntered(MouseEvent e) 
         {
            
         }

         public void mouseExited(MouseEvent e) 
         {
            
         }

         public void mouseClicked(MouseEvent e) 
         {
            
         }
         public void run()
         {
             _seg.findBrain();
             _imp.updateAndDraw();
         }
    }
}



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

package ktdiedrich.imagek;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.text.TextWindow;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class DFMFromPointListener implements MouseListener, Runnable
{
    private DFMFromPoint _dfmp;
    private boolean _enable;
    private ImagePlus _inputImage;
    private ImageCanvas _inputCanvas;
    private TextWindow _messageWindow;
    private Position _startPos;
    
	public DFMFromPointListener(ImagePlus inputImage, List<CenterlineGraph> centerlines, 
			float xRes, float yRes, float zRes, int width, int height, int zSize )
    {
    	_inputImage = inputImage;
    	_inputCanvas = _inputImage.getCanvas();
    	_inputCanvas.addMouseListener(this);
        _dfmp = new DFMFromPoint(centerlines, xRes, yRes, zRes, width, height, zSize);
        _enable = true;
    }
    public void mousePressed(MouseEvent e) 
    {
        
    }

     public void mouseReleased(MouseEvent e) 
     {
        Point pt = e.getPoint();
        int x = _inputCanvas.offScreenX(pt.x);
        int y  = _inputCanvas.offScreenY(pt.y);
        int z = _inputImage.getCurrentSlice()-1;
        _startPos = new Position(x, y, z);
        if (_messageWindow != null)
        {
        	_messageWindow.append("DFM Start point: ("+_startPos.getColumn()+", "+_startPos.getRow()+", "+
        			_startPos.getZ()+")");
        }
        
        if (_enable)
        {
        	// TODO enable and disable when not running and when running 
            //_enable = false;
            Thread thread = new Thread(this);
            thread.start();
        }
        else
        {
            if (_messageWindow != null)
            {
            	_messageWindow.append("Already ran DFM. Rerun plugin to run again. ");
            }
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
     /** Execute the DFM calculation */
     public void run()
     {
    	 if (_messageWindow != null)
    	 {
    		 _messageWindow.append("Running DFM tortuosity calculation");
    	 }
    	 ImagePlus dfmBifurcation = _dfmp.setStartPosition(_startPos);
    	 if (dfmBifurcation != null)
    	 {
    		 dfmBifurcation.show();
    		 dfmBifurcation.updateAndDraw();
    	 }
     }
     public TextWindow getMessageWindow() 
     {
 		return _messageWindow;
 	}
 	public void setMessageWindow(TextWindow messageWindow) 
 	{
 		_messageWindow = messageWindow;
 		_messageWindow.append(
 				"Waiting for selection of start point for DFM by clicking mouse of the color artery image");
 		_dfmp.setMessageWindow(messageWindow);
 	}
 	public void setThickenOutput(boolean thicken)
 	{
 		_dfmp.setThickenOutput(thicken);
 	}
}

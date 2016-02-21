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

/** Display image of a single artery. 
 * @author ktdiedrich@gmail.com 
 * */
public class SingleArteryListener implements MouseListener, Runnable
{
    private SingleArtery _singleArtery;
    private ImagePlus _inputImage;
    private ImageCanvas _inputCanvas;
    private TextWindow _messageWindow;
    private Position _selectedPos;
    private float _xRes, _yRes, _zRes;
    int _width, _height, _zSize;
    private int _count;
	public SingleArteryListener(ImagePlus inputImage, List<CenterlineGraph> centerlines, 
			float xRes, float yRes, float zRes, int width, int height, int zSize )
    {
    	_inputImage = inputImage;
    	_xRes = xRes;
    	_yRes = yRes;
    	_zRes = zRes;
    	_width = width;
    	_height= height;
    	_zSize = zSize;
    	_inputCanvas = _inputImage.getCanvas();
    	_inputCanvas.addMouseListener(this);
        _singleArtery = new SingleArtery(centerlines, width, height, zSize);
        _count = 1;
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
        
        _selectedPos = new Position(x, y, z);
        if (_messageWindow != null)
        {
        	_messageWindow.append("Selected point: ("+_selectedPos.getColumn()+", "+_selectedPos.getRow()+", "+
        			_selectedPos.getZ()+")");
        }
        
        Thread thread = new Thread(this);
        thread.start();
       
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
     /** Generate the single artery image. */
     public void run()
     {
    	 if (_messageWindow != null)
    	 {
    		 _messageWindow.append("Generating single artery image. ");
    	 }
    	 ImagePlus singleArteryImage = _singleArtery.setStartPosition(_selectedPos, _inputImage.getShortTitle()+_count);
    	 if (singleArteryImage != null)
    	 {
    		 _count++;
    		 singleArteryImage.show();
    		 singleArteryImage.updateAndDraw();
    		 DFMFromPointListener dfmp = new DFMFromPointListener(singleArteryImage, _singleArtery.getSingleCenterlineGraph(), 
    	         		_xRes, _yRes, _zRes, _width, _height, _zSize); 
    	     dfmp.setThickenOutput(false);
    	         
    	     if (_messageWindow != null)
    	     {
    	      	dfmp.setMessageWindow(_messageWindow);
    	     }
    	 }
    	 else
    	 {
    		 _messageWindow.append("Singe Artery Image is null");
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
 				"Select a single artery to view by clicking mouse of the color artery image");
 		_singleArtery.setMessageWindow(messageWindow);
 	}
 	
}

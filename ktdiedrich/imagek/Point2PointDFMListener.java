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

/** Measure the Distance Factor Metric tortuosity score from Point to Point
 * along a centerline by selecting the ends with the left mouse button. First point selected
 * is the point to measure distances from. The middle mouse button selects and turns off 
 * a centerline. 
 * @author ktdiedrich@gmail.com 
 * */
public class Point2PointDFMListener implements MouseListener, Runnable, Message
{
    private Point2PointDFM _p2pdfm;
    private ImagePlus _inputImage;
    private ImageCanvas _inputCanvas;
    private Message _messageWindow;
    private Position _selectedPos;
    private int _mouseButton;
    private List<CenterlineGraph> _centerlineGraphs;
	public Point2PointDFMListener(ImagePlus inputImage, List<CenterlineGraph> centerlines, Point2PointDFM p2pdfm )
    {
    	_inputImage = inputImage;
    	_inputCanvas = _inputImage.getCanvas();
    	_inputCanvas.addMouseListener(this);
        _p2pdfm = p2pdfm;
        _centerlineGraphs = centerlines;
    }
    public void mousePressed(MouseEvent e) 
    {
        
    }

     public void mouseReleased(MouseEvent e) 
     {
       
     }

     public void mouseEntered(MouseEvent e) 
     {
        
     }

     public void mouseExited(MouseEvent e) 
     {
        
     }

     public void mouseClicked(MouseEvent e) 
     {
        _mouseButton = e.getButton();
        Point pt = e.getPoint();
        int x = _inputCanvas.offScreenX(pt.x);
        int y  = _inputCanvas.offScreenY(pt.y);
        int z = _inputImage.getCurrentSlice()-1;
        
        _selectedPos = new Position(x, y, z);
        //message("Selected point: ("+_selectedPos.getColumn()+", "+_selectedPos.getRow()+", "+_selectedPos.getZ()+")");
        
        Thread thread = new Thread(this);
        thread.start();
     }
     /** Generate the single artery image. */
     public void run()
     {	
    	 if (_mouseButton == MouseEvent.BUTTON1)
    	 {
    		 _p2pdfm.setPoint(_selectedPos);
    	 }
    	 else if (_mouseButton == MouseEvent.BUTTON2)
    	 {
    		 message("Select a centerline");
    		 CenterlineSelect centSel = new CenterlineSelect(_inputImage, _centerlineGraphs);
    		 centSel.setPoint(_selectedPos);
    	 }
    	 
     }
    
 	public void setMessageWindow(Message messageWindow) 
 	{
 		_messageWindow = messageWindow;
 		_messageWindow.message("Select first point of centerline to measure DFM with left mouse button. \n"+
 					"Second mouse button selects and turns off a centerline.");
 		_p2pdfm.setMessageWindow(messageWindow);
 	}
 	public void message(String m)
 	{
 		if (_messageWindow != null)
 		{
 			_messageWindow.message(m);
 		}
 		else
 			System.out.println(m);
 	}
 	public void clear()
 	{
 		if (_messageWindow != null)
 			_messageWindow.clear();
 	}
}

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
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.util.List;

/** Find a centerline and turn it off. 
 * @author ktdiedrich@gmail.com */
public class CenterlineSelect 
{
	private List<CenterlineGraph> _centerlineGraphs;
	private TextWindow _messageWindow;
	private Colors _colors;
	private ImagePlus _image;
	private ImageProcessor[] _imageProcessors;
	public CenterlineSelect(ImagePlus image, List<CenterlineGraph> centerlineGraphs)
	{
		_centerlineGraphs = centerlineGraphs;
		_colors = Colors.getColors();
		_image = image;
		ImageStack stack = _image.getStack();
		int sSize = stack.getSize();
	    _imageProcessors = new ImageProcessor[sSize];
	    for (int i = 0; i < sSize; i++)
	    {
	    	_imageProcessors[i] = stack.getProcessor(i+1);
	    }
	}
	public TextWindow getMessageWindow() 
	{
		return _messageWindow;
	}
	public void setMessageWindow(TextWindow messageWindow) 
	{
		_messageWindow = messageWindow;
	}
	
	public void setPoint(Position position)
	{
		int x = position.getColumn();
		int y = position.getRow();
		int z = position.getZ();
		
		message("Searching for centerline point at: "+x+", "+y+", "+z);
		
		boolean isCenterline =  false;
		Centerline offCenterline = null;
		CenterlineGraph offGraph = null;
		int offI = 0;
		for (CenterlineGraph centGraph: _centerlineGraphs)
		{
			offI = 0;
	    	for (Centerline centerline: centGraph.getCenterlines())
	    	{
	    		
	    		for (GraphNode node: centerline.getCenterlineNodes())
	    		{
	    			//if (node.col == col && node.row == row && node.z == zDepth)
	    			if (node.col > x-2 && node.col < x+2 && 
	    			        node.row > y-2 && node.row < y+2 && 
	    			        node.z == z)
	    			{
	    				isCenterline = true;
	    				offCenterline = centerline;
	    				offGraph = centGraph;
    					highlightCenterline(centerline);
	    				break;
	    			}
	    		}
	    		if (isCenterline == true)
                {
                    break;
                }
	    		offI++;
	    	}
	    	if (isCenterline == true)
            {
                break;
            }
		}
		if (offCenterline != null)
		{
			List<Centerline> offCenterlines = offGraph.getCenterlines();
			offCenterlines.remove(offI);
		}
		
		if (isCenterline == false)
    	{
    		message("Centerline not found. Pick a centerline voxel to remove a centerline.");
    	}
	}
	public void highlightCenterline(Centerline centerline)
	{
		for (GraphNode node: centerline.getCenterlineNodes())
		{
			_imageProcessors[node.z].putPixel(node.col, node.row, _colors.off);
		}
		_image.updateAndDraw();
	}
	private void message(String m)
	{
		if (_messageWindow != null)
			_messageWindow.append(m);
		else
			System.out.println(m);
	}
}

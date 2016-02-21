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
import ij.text.TextWindow;

import java.awt.Point;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/** Display an image with a single selected artery. 
 * @author ktdiedrich@gmail.com 
 *  */
public class SingleArtery 
{
	private List<CenterlineGraph> _centerlineGraphs;
	private TextWindow _messageWindow;
	private int _width, _height, _zSize;
	private boolean _thickenOutput;
	
	public boolean isThickenOutput() 
	{
		return _thickenOutput;
	}
	public void setThickenOutput(boolean thickenOutput) 
	{
		_thickenOutput = thickenOutput;
	}
	public SingleArtery(List<CenterlineGraph> centerlines, int width, int height, int zSize)
	{
		_centerlineGraphs = centerlines;
		_width = width;
		_height = height;
		_zSize = zSize;
	}
	public TextWindow getMessageWindow() 
	{
		return _messageWindow;
	}
	public void setMessageWindow(TextWindow messageWindow) 
	{
		_messageWindow = messageWindow;
	}
	private List<CenterlineGraph> _singleCenterlineGraph;
	public ImagePlus setStartPosition(Position startPosition, String name) 
	{
		int startCol = startPosition.getColumn();
		int startRow = startPosition.getRow();
		int startZ = startPosition.getZ();
		
		GraphNode startNode = null;
		_singleCenterlineGraph = new LinkedList<CenterlineGraph>();
		if (_messageWindow != null)
		{
			_messageWindow.append("Searching for centerline at: "+startCol+", "+startRow+", "+startZ);
		}
		for (CenterlineGraph centGraph: _centerlineGraphs)
		{
	    	for (Centerline centerline: centGraph.getCenterlines())
	    	{
	    		for (GraphNode node: centerline.getCenterlineNodes())
	    		{
	    			if (node.col == startCol && node.row == startRow && node.z == startZ)
	    			{
	    				startNode = node;
	    				CenterlineGraph graph = new CenterlineGraph();
	    				graph.addCenterline(centerline);
	    				_singleCenterlineGraph.add(graph);
	    			}
	    			
	    		}
	    	}
		}
    	if (startNode == null)
    	{
    		if (_messageWindow != null)
    		{
    			_messageWindow.append("Pick a bifurcation or centerline voxel.");
    		}
    		return null;
    	}
    	else
    	{
    		if (_messageWindow != null)
    		{
    			_messageWindow.append("Found starting centerline: "+startCol+", "+startRow+", "+startZ);
    		}
    	}
        ImagePlus singleImage = Centerline.makeColorImage(_singleCenterlineGraph, _width, _height, _zSize, name, true);
        return singleImage;
	}
	public List<CenterlineGraph> getSingleCenterlineGraph() 
	{
		return _singleCenterlineGraph;
	}
	
}


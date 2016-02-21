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

/** Distance Factor Metric from a starting point
 * Calculates L/d ration from every point moving away from the start. */
public class DFMFromPoint 
{
	private float _colDis, _rowDis, _zDis, _colRowDis, _colZDis, _rowZDis, _colRowZDis;
	private List<CenterlineGraph> _centerlines;
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
	public DFMFromPoint(List<CenterlineGraph> centerlines, float xRes, float yRes, float zRes, 
			int width, int height, int zSize)
	{
		_centerlines = centerlines;
		_width = width;
		_height = height;
		_zSize = zSize;
		calculateDirectionCosts(xRes, yRes, zRes);
	}
	public TextWindow getMessageWindow() 
	{
		return _messageWindow;
	}
	public void setMessageWindow(TextWindow messageWindow) 
	{
		_messageWindow = messageWindow;
	}
	
	/** Calculate the costs of the moves in any direction using the x, y, z resolutions for 
     * measuring length of centerline. */
	public void calculateDirectionCosts(float xRes, float yRes, float zRes)
	{
		// calculate direction costs and round to short 
    	_colDis = (yRes);
    	_rowDis = (xRes);
    	_zDis = (zRes);
    	
    	_colRowDis = (float)( Math.sqrt(xRes*xRes + yRes*yRes) );
    	_colZDis = (float)( Math.sqrt(xRes*xRes + zRes*zRes) );
    	_rowZDis = (float)( Math.sqrt(yRes*yRes + zRes*zRes) );
    	
    	_colRowZDis = (float)( Math.sqrt( xRes*xRes + yRes*yRes + zRes*zRes ) );
    	
    	//System.out.println("col="+_colDis+" row="+_rowDis+" z="+_zDis+" colRow="+_colRowDis+
    		//	" colZ="+_colZDis+" rowZ="+_rowZDis+" colRowZ="+_colRowZDis);
	}
	 /** Calculate the distance traveled between two adjacent nodes using the (x,y,z) resolutions 
     * and the directions moved. 
	@param a starting node
	@param b ending node 
    @return distance traveled between nodes.*/
    public float measureCenterline(GraphNode a, GraphNode b)
    {
		float dis = 0.0f;
		int rowStep = Math.abs(a.row - b.row);
		int colStep = Math.abs(a.col - b.col);
		int zStep = Math.abs(a.z - b.z);
		if (rowStep > 1 || colStep > 1 || zStep > 1)
	    {
			System.err.println("Not neighbors: rowStep="+rowStep+" colStep="+colStep+" zStep="+zStep);
			dis = -1.0f;
	    }
		if (rowStep == 1 && colStep == 1 && zStep == 1)
	    {
			dis =  _colRowZDis;
	    }
		else if (rowStep == 1 && colStep == 1)
	    {
			dis =  _colRowDis;
	    }
		else if (colStep == 1 && zStep == 1)
	    {
			dis = _colZDis;
	    }
		else if (rowStep == 1 && zStep == 1)
	    {
			dis =  _rowZDis;
	    }
		else if (colStep == 1)
	    {
			dis =  _colDis;
	    }
		else if (rowStep == 1)
	    {
			dis = _rowDis;
	    }
		else if (zStep == 1)
	    {
			dis = _zDis;
	    }
		// _twin.append("rowStep="+rowStep+" colStep="+colStep+" zStep="+zStep+": "+dis);
		return dis;
    }
    /** Measure the straight line distance from 2 nodes. */
    float measureStraight(GraphNode head, GraphNode tail)
    {
    	double dis = 0.0f;
    	// d2 = x2 + y2 + z2
    	dis = Math.sqrt(Math.pow((head.col - tail.col)*_colDis, 2.0) + 
    			Math.pow((head.row - tail.row)*_rowDis, 2.0) + 
    			Math.pow((head.z - tail.z)*_zDis, 2.0)*_zDis );
		
    	
    	return (float)dis;
    }
	
	public ImagePlus setStartPosition(Position startPosition) 
	{
		int startCol = startPosition.getColumn();
		int startRow = startPosition.getRow();
		int startZ = startPosition.getZ();
		
		GraphNode startNode = null;
		List<GraphNode> allNodes = new LinkedList<GraphNode>();
		if (_messageWindow != null)
		{
			_messageWindow.append("Searching for centerline at: "+startCol+", "+startRow+", "+startZ);
		}
		for (CenterlineGraph centGraph: _centerlines)
		{
	    	for (Centerline centerline: centGraph.getCenterlines())
	    	{
	    		for (GraphNode node: centerline.getCenterlineNodes())
	    		{
	    			if (node.col == startCol && node.row == startRow && node.z == startZ)
	    			{
	    				startNode = node;
	    			}
	    			else
	    			{
	    				allNodes.add(node);
	    				node.graphed = false;
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
        Centerline tortuosityLine = new Centerline();
        tortuosityLine.addNode(startNode);
        Queue<GraphNode> queue = new LinkedList<GraphNode>();
        queue.add(startNode);
        startNode.graphed = true;
        
        while (queue.isEmpty() == false)
        {
            GraphNode node = queue.remove();
            for (GraphNode adjNode: node.adjacents)
            {
                if (adjNode.graphed == false)
                {
                	float Lpiece = measureCenterline(adjNode, node);
                	adjNode.cumDist = node.cumDist+Lpiece;
                	float d = measureStraight(startNode, adjNode);
                	// L/d
                	adjNode.dfm = adjNode.cumDist/d;
                    tortuosityLine.addNode(adjNode);
                    allNodes.remove(adjNode);
                    queue.add(adjNode);
                    adjNode.graphed = true;
                }
            }
        }
        List<Centerline> list = new LinkedList<Centerline>();
        list.add(tortuosityLine);
        return Centerline.makeDfmImage(list, _width, _height, _zSize, _thickenOutput, "DFM_bifurcation");
	}
}

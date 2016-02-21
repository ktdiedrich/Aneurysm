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

import ij.IJ;

/** Measures lines. Floating point accuracy. 
 * @author ktdiedrich@gmail.com */
public class LineMeasure 
{
	private float _xRes, _yRes, _zRes;
	private float _colDis, _rowDis, _zDis, _colRowDis, _colZDis, _rowZDis, _colRowZDis;
	public LineMeasure(float xRes, float yRes, float zRes)
	{
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
		IJ.log("xRes: "+_xRes+" yRes: "+_yRes+" zRes:"+_zRes);
		calculateDirectionCosts();
	}
	/** Find the average x, y, z position of the node with its adjacent centerlines. 
	 * @return [x, y, z] length 3 double array. */
	private static double[] adjAveXYZ(GraphNode n)
	{
		double[] xyz = new double[3];
		
		double sumX = n.col;
		double sumY = n.row;
		double sumZ = n.z;
		int cnt  = 1;
		for (GraphNode adj: n.adjacents)
		{
			if (adj.isCenterline)
			{
				sumX += adj.col;
				sumY += adj.row;
				sumZ += adj.z;
				cnt++;
			}
		}
		double dcnt  = (double)cnt;
		IJ.log("Smooth adjacents: "+cnt);
		xyz[0] = sumX/dcnt;
		xyz[1] = sumY/dcnt;
		xyz[2] = sumZ/dcnt;
		
		return xyz;
	}
	public double measure3AveAdj(GraphNode a, GraphNode b)
	{
		double dis = 0;
		double[] xyzA = adjAveXYZ(a);
		double[] xyzB = adjAveXYZ(b);
		
		double xDis = (xyzA[0] - xyzB[0]) * _colDis;
		double yDis = (xyzA[1] - xyzB[1]) * _rowDis;
		double zDis = (xyzA[2] - xyzB[2]) * _zDis;
		
		dis = Math.sqrt(xDis*xDis + yDis*yDis + zDis*zDis);
		
		return dis;
	}
	/** Measure the straight line distance from 2 nodes. */
    public double measure3AveStraight(GraphNode head, GraphNode tail)
    {
    	double dis = 0.0f;
    	double[] xyzHead = adjAveXYZ(head);
    	double[] xyzTail = adjAveXYZ(tail);
    	
    	double xDis = (xyzHead[0] - xyzTail[0]) * _colDis;
    	double yDis = (xyzHead[1] - xyzTail[1]) * _rowDis;
    	double zDis = (xyzHead[2] - xyzTail[2]) *_zDis;

    	dis = Math.sqrt(xDis*xDis + yDis*yDis + zDis*zDis);
		
    	return dis;
    }
	
	/** Calculate the distance traveled between two adjacent nodes using the (x,y,z) resolutions 
     * and the directions moved. 
	@param a starting node
	@param b ending node 
    @return distance traveled between nodes.*/
    public float measureAdjacent(GraphNode a, GraphNode b)
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
    public float measureStraight(GraphNode head, GraphNode tail)
    {
    	double dis = 0.0f;
    	// d2 = x2 + y2 + z2
    	
    	dis = Math.sqrt(Math.pow((head.col - tail.col)*_colDis, 2.0) + 
    			Math.pow((head.row - tail.row)*_rowDis, 2.0) + 
    			Math.pow((head.z - tail.z)*_zDis, 2.0) );
		
    	
    	return (float)dis;
    }
    /** Calculate the costs of the moves in any direction using the x, y, z resolutions for 
     * measuring length of centerline. */
	private void calculateDirectionCosts()
	{
		// calculate direction costs and round to short 
    	_colDis = (_yRes);
    	_rowDis = (_xRes);
    	_zDis = (_zRes);
    	
    	_colRowDis = (float)( Math.sqrt(_xRes*_xRes + _yRes*_yRes) );
    	_colZDis = (float)( Math.sqrt(_xRes*_xRes + _zRes*_zRes) );
    	_rowZDis = (float)( Math.sqrt(_yRes*_yRes + _zRes*_zRes) );
    	
    	_colRowZDis = (float)( Math.sqrt( _xRes*_xRes + _yRes*_yRes + _zRes*_zRes ) );
    }
}

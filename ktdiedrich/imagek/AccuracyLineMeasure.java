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

import java.util.Iterator;
import java.util.List;

/** Measure the length of a line using the accuracy defined in VoxelDistance.
 * Similar to the LineMeasure class  
 * @author ktdiedrich@gmail.com 
 * */
public class AccuracyLineMeasure 
{
	private short _col, _row, _z;
    private short _colRow, _colZ, _rowZ;
    private short _colRowZ;
    
	public AccuracyLineMeasure(float xRes, float yRes, float zRes)
	{
		VoxelDistance voxDis = new VoxelDistance(xRes, yRes, zRes);
		_col = voxDis.getCol();
        _row = voxDis.getRow();
        _z = voxDis.getZ();
        
        _colRow = voxDis.getColRow();
        _colZ = voxDis.getColZ();
        _rowZ = voxDis.getRowZ();
        
        _colRowZ = voxDis.getColRowZ();
	}
	public int measureLine(List<GraphNode> line)
	{
		if (line.size() < 2) return 0;
		int L = 0;
		
		GraphNode head  = line.get(0);
		GraphNode prev = head;
        Iterator<GraphNode> itr = line.iterator();
		GraphNode lineNode = null;
		while (itr.hasNext())
        {
		    lineNode = itr.next();
		    L += this.measureAdjacent(lineNode, prev);
            prev = lineNode;
        }
		return (L);
	}
	/** Calculate the distance traveled between two adjacent nodes using the (x,y,z) resolutions 
     * and the directions moved. 
	@param a starting node
	@param b ending node 
    @return distance traveled between nodes.*/
    public short measureAdjacent(GraphNode a, GraphNode b)
    {
		short dis = 0;
		int rowStep = Math.abs(a.row - b.row);
		int colStep = Math.abs(a.col - b.col);
		int zStep = Math.abs(a.z - b.z);
		if (rowStep > 1 || colStep > 1 || zStep > 1)
	    {
			System.err.println("Not neighbors: rowStep="+rowStep+" colStep="+colStep+" zStep="+zStep);
			dis = -1;
	    }
		if (rowStep == 1 && colStep == 1 && zStep == 1)
	    {
			dis =  _colRowZ;
	    }
		else if (rowStep == 1 && colStep == 1)
	    {
			dis =  _colRow;
	    }
		else if (colStep == 1 && zStep == 1)
	    {
			dis = _colZ;
	    }
		else if (rowStep == 1 && zStep == 1)
	    {
			dis =  _rowZ;
	    }
		else if (colStep == 1)
	    {
			dis =  _col;
	    }
		else if (rowStep == 1)
	    {
			dis = _row;
	    }
		else if (zStep == 1)
	    {
			dis = _z;
	    }
		// _twin.append("rowStep="+rowStep+" colStep="+colStep+" zStep="+zStep+": "+dis);
		return dis;
    }
}

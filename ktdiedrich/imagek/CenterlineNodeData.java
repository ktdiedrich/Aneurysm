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

import java.util.LinkedList;
import java.util.List;

/** Data attached to a centerline image graph node 
 * @author ktdiedrich@gmail.com */
public class CenterlineNodeData 
{
	private List<GraphNode> _belongNodes;
	
	CenterlineNodeData()
	{
		_belongNodes = new LinkedList<GraphNode>();
	}
	/** Add Voxel node belonging to this centerline voxel node. */
	public void addBelongNode(GraphNode node)
	{
		_belongNodes.add(node);
	}
	public short getMeanDFC()
	{
		short dfcSum = 0;
		for (GraphNode n: _belongNodes)
		{
			dfcSum += n.dfc;
		}
		return (short)Math.round( (double)dfcSum / (double)_belongNodes.size() );
	}
	public List<GraphNode> getBelongingNodes() {
		return _belongNodes;
	}
	/** @return the highest DFC among the GraphNodes belonging to the Centerline node. */
	public short getMaxDfc() {
		short maxDFC =0;
		for (GraphNode n: _belongNodes)
		{
			if (n.dfc > maxDFC)
				maxDFC = n.dfc;
		}
		return maxDFC;
	}
	public static void assignClosestBelongNodes(List<GraphNode> voxelNodes, List<GraphNode> centerlineNodes,
			float xRes, float yRes, float zRes)
	{
		VoxelDistance vd = new VoxelDistance(xRes, yRes, zRes);
		for (GraphNode cn: centerlineNodes)
		{
			cn.centerlineNodeData = new CenterlineNodeData();
		}
		for (GraphNode vn: voxelNodes)
		{
			double voxelD = Double.MAX_VALUE;
			GraphNode nearestCenter = null;
			for (GraphNode cn: centerlineNodes)
			{
				double d = vd.distance(vn, cn);
				// double d = GraphNode.distance(vn, cn);
				if (d < voxelD)
				{
					voxelD = d;
					nearestCenter = cn;
				}
			}
			vn.centerlineNode = nearestCenter;
			if (nearestCenter != null)
			{
				if (nearestCenter.centerlineNodeData != null)
				{
					nearestCenter.centerlineNodeData.addBelongNode(vn);
				}
			}
		}
	}
}

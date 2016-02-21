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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Connected centerlines
 * @author ktdiedrich@gmail.com 
 * */
public class CenterlineGraph 
{
	public CenterlineGraph()
	{
		_centerlines = new LinkedList<Centerline>();
		_treeEnds = new HashSet<GraphNode>();
		_cycles = new LinkedList<CenterlineCycle>();
	}
	private Set<GraphNode> _treeEnds;
	private Set<GraphNode> _goodEnds;
	private GraphNode _sourceNode;
	private List<Centerline> _centerlines;
	private List<CenterlineCycle> _cycles;
	
	public GraphNode getSourceNode() 
	{
		return _sourceNode;
	}
	/** The tip ends of the centerline graph tree. These can be used as source/goal nodes for recalculating
	 * the centerlines with new goal nodes. */
	public Set<GraphNode> getTreeEnds()
	{
		return _treeEnds;
	}
	public void addTreeEnd(GraphNode treeEnd)
	{
		if (treeEnd.isCenterline == false)
		{
			// IJ.log("TreeEnd: "+treeEnd.coordinateString()+" not centerline.");
			treeEnd.isCenterline = true;
		}
		_treeEnds.add(treeEnd);
	}
	public void setSourceNode(GraphNode sourceNode) 
	{
		_sourceNode = sourceNode;
		// IJ.log("Centerline Graph Source node: "+GraphNode.toString(_sourceNode));
	}
	public List<Centerline> getCenterlines() 
	{
		return _centerlines;
	}
	public void setCenterlines(List<Centerline> centerlines) 
	{
		_centerlines = centerlines;
		for (Centerline c: centerlines)
		{
			c.setCenterlineGraph(this);
		}
	}
	public void addCenterline(Centerline centerline)
	{
		_centerlines.add(centerline);
		centerline.setCenterlineGraph(this);
	}
	/** All nodes in all the centerlines of this graph */
	public List<GraphNode> getCenterlineNodes()
	{
		List<GraphNode> all = new LinkedList<GraphNode>();
		for (Centerline centerline: _centerlines)
		{
			for (GraphNode node: centerline.getCenterlineNodes())
			{
				all.add(node);
			}
		}
		return all;
	}
	/** The best set of ends available _goodEnds if not empty or _treeEnds if _goodEnds is empty. */
	public Set<GraphNode> getGoodEnds() {
		if (_goodEnds != null && _goodEnds.size() > 0)
			return _goodEnds;
		else
			return _treeEnds;
	}
	public void setGoodEnds(Set<GraphNode> goodEnds) {
		_goodEnds = goodEnds;
	}
	public void addCycle(CenterlineCycle cycle)
	{
		_cycles.add(cycle);
	}
	public List<CenterlineCycle> getCycles()
	{
		return _cycles;
	}
}

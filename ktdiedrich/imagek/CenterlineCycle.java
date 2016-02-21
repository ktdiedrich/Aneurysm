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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Represents one cycle in a centerline. 
 * @author ktdiedrich@gmail.com */
public class CenterlineCycle 
{
	private Set<GraphNode> _cycle;
	private Set<GraphNode> _breakFront;
	private Set<GraphNode> _cycleFront;
	private Set<GraphNode> _erase;
	private short _dfeThreshold;
	private float _xRes, _yRes, _zRes;
	public CenterlineCycle(Set<GraphNode> cycle, float xRes, float yRes, float zRes)
	{
		_cycle = cycle;
		
		_breakFront =  new HashSet<GraphNode>();
		_cycleFront = new HashSet<GraphNode>();
		_erase = new HashSet<GraphNode>();
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
	}
	public void setProximalNode(GraphNode node)
	{
		
		_erase  = new HashSet<GraphNode>();
		_erase.add(node);
		
		List<GraphNode> belong  = null;
		IJ.log("Proximal node: "+node.coordinateString());
		if (node.centerlineNodeData != null)
		{
			IJ.log("Has belong nodes: "+node.centerlineNodeData.getBelongingNodes().size());
			belong = node.centerlineNodeData.getBelongingNodes();
		}
		else
		{
			IJ.log("Centerline surround nodes: "+node.centerline.getSurroundNodes().size());
			belong = node.centerline.getSurroundNodes();
		}
		
		for (GraphNode s: belong)
		{
			if (s.centerlineNode == node)
			{
				_erase.add(s);
			}
		}
	}
	public Set<GraphNode> getCycle() {
		return _cycle;
	}
	public void addBreakFront(GraphNode node)
	{
		_breakFront.add(node);
	}
	public void addCycleFront(GraphNode node)
	{
		_cycleFront.add(node);
	}
	public Set<GraphNode> getBreakFront() {
		return _breakFront;
	}
	public Set<GraphNode> getCycleFront() {
		return _cycleFront;
	}
	public boolean removeBreakFront(GraphNode node)
	{
		return _breakFront.remove(node);
	}
	public boolean removeCycleFront(GraphNode node)
	{
		return _cycleFront.remove(node);
	}
	public Map<GraphNode, GraphNode> pairConnect()
	{
		Map<GraphNode, GraphNode> pairs = new HashMap<GraphNode, GraphNode>();
		
		int cLen = _cycleFront.size();
		int bLen = _breakFront.size();
		IJ.log("CycleFront: "+cLen+" BreakFront: "+bLen);
		if (cLen < 2 || bLen < 2)
			return pairs;
		
		// double[] four = new double[4];
		GraphNode[] cf = _cycleFront.toArray(new GraphNode[cLen]);
		GraphNode[] bf = _breakFront.toArray(new GraphNode[bLen]);
		double ac = GraphNode.distance(cf[0], bf[0]);
		double ad = GraphNode.distance(cf[0], bf[1]);
		double bc = GraphNode.distance(cf[1], bf[0]);
		double bd = GraphNode.distance(cf[1], bf[1]);
		
		if (ac<ad && ac<bc && ac<bd)
		{
			pairs.put(cf[0], bf[0]);
			pairs.put(cf[1], bf[1]);
		}
		else if (bd<ad && bd<bc && bd<ac)
		{
			pairs.put(cf[0], bf[0]);
			pairs.put(cf[1], bf[1]);
		}
		else if (ad<ac && ad<bc && ad<bd)
		{
			pairs.put(cf[0], bf[1]);
			pairs.put(cf[1], bf[0]);
		}
		else if (bc<ac && bc<ad && bc<bd)
		{
			pairs.put(cf[0], bf[1]);
			pairs.put(cf[1], bf[0]);
		}
		/* 
		for (GraphNode cn: _cycleFront)
		{
			cn.predecessor=null;
			cn.pathCost = Float.MAX_VALUE;
		}
		for (GraphNode bn: _breakFront)
		{
			bn.predecessor=null;
			bn.pathCost = Float.MAX_VALUE;
		}
		for (GraphNode cn: _cycleFront)
		{
			for (GraphNode bn: _breakFront)
			{
				double d = GraphNode.distance(cn, bn);
				if (d < cn.pathCost && d < bn.pathCost)
				{
					cn.pathCost = (float)d;
					bn.pathCost = (float)d;
					cn.predecessor = bn;
				}
			}
		}
		for (GraphNode cn: _cycleFront)
		{
			if (cn.predecessor != null)
			{
				pairs.put(cn, cn.predecessor);
			}
		} */
		
		return pairs;
	}
	public Set<GraphNode> getErase() {
		return _erase;
	}
	public static void connectPairs(Map<GraphNode, GraphNode> pair, int zSize, CenterlineGraph centGraph)
	{
		Colors colors = Colors.getColors();
		for (GraphNode source: pair.keySet())
		{
			Graph g = source.centerline.getGraph();
			LinkedList<GraphNode> nodes = g.getNodes();
			ShortestPaths sp = new ShortestPaths();
			GraphNode back = pair.get(source);
			Graph shortest = sp.dijkstraLowestCostPathTarget(nodes, source, zSize, back);
			Centerline patchLine = Centerlines.backtrace(source, back);
			if (patchLine != null)
			{
				patchLine.setRgb(colors.magenta);
				centGraph.addCenterline(patchLine);
			}
		}
	}
	public short getDfeThreshold() {
		return _dfeThreshold;
	}
	public void setDfeThreshold(short dfeThreshold) {
		_dfeThreshold = dfeThreshold;
	}
	/** Narrowest node index 0 and adjacent nodes in a list. */
	public List<GraphNode> getNarrowNodeAdjs() {
		List<GraphNode> narrowNode = null;
		double narrow = Double.MAX_VALUE;
		for (GraphNode n: _cycle)
		{
			short sumDFE=n.dfe;
			int sz = 1;
			List<GraphNode> narAdjs = new ArrayList<GraphNode>(4);
			narAdjs.add(n);
			for (GraphNode a: n.adjacents)
			{
				if (a.isCenterline)
				{
					narAdjs.add(a);
					sumDFE+=a.dfe;
					sz++;
				}
			}
			double aveDFE = (double)sumDFE/(double)sz;
			//if (aveDFE < narrow)
			if (n.dfe < narrow)
			{
				// narrow = aveDFE;
				narrow = n.dfe;
				narrowNode = narAdjs;
			}
		}
		return narrowNode;
	}
	/** Find the nearest two bifurcations to the narrow[0] node in the loop. */
	public List<GraphNode> narrowBifurcations(List<GraphNode> narrow)
	{
		List<GraphNode> bifs = new LinkedList<GraphNode>();
		
		GraphNode s = narrow.get(0);
		
		if (s.isCenterline == false)
			return bifs;
		
		Graph graph = s.centerline.getGraph();
		List<GraphNode> g = graph.getNodes();
		
		for (GraphNode gn: g)
		{
			gn.pathLen = CenterlineCycles.WHITE;
			gn.pathCost = Float.MAX_VALUE;
			gn.predecessor = null;
			gn.graphed = false;
		}
		s.graphed = false;
		s.pathLen = CenterlineCycles.GRAY;
		s.pathCost = 0;
		s.predecessor = null;
		
		Queue<GraphNode> q = new LinkedList<GraphNode>();
		q.add(s);
		int bifFound = 0;
		while (!q.isEmpty() && bifFound < 2)
		{
			GraphNode u = q.poll();
			
			for (GraphNode v: u.adjacents)
			{
				if (v.isCenterline && v.graphed == false && v.pathLen == CenterlineCycles.WHITE) // only consider centerlines 
				{
					v.pathLen = CenterlineCycles.GRAY;
					v.pathCost = u.pathCost+1;
					v.predecessor = u;
					q.add(v);
					if (v.isBifurcation)
					{
						IJ.log("Found bifurcation: "+v.coordinateString());
						bifs.add(v);
						bifFound++;
					}
				}
			}
			u.pathLen = CenterlineCycles.BLACK;
		}
		
		return bifs;
	}
	public int narrowRadius()
	{
		GraphNode narrow = this.getNarrowNodeAdjs().get(0);
		short mRes = VoxelDistance.convert2short(VoxelDistance.maxRes(_xRes, _yRes, _zRes));
		short maxDFC = narrow.centerlineNodeData.getMaxDfc();
		int range = (int)Math.ceil( (double)(maxDFC)/(double)mRes );
		//if (narrow.centerlineNodeData != null)
		//{
			//range = (int)Math.ceil( (double)(narrow.centerlineNodeData.getMaxDfc())/(double)mRes );
		//}
		return range;
	}
	/** Generate the set of voxels to erode away the narrow in the loop. */
	public Set<GraphNode> erodeNarrow()
	{
		GraphNode narrow = this.getNarrowNodeAdjs().get(0);
		int range = this.narrowRadius();
		
		return contiguousVoxelSet(narrow, range);
	}
	public float getXRes() {
		return _xRes;
	}
	public void setXRes(float res) {
		_xRes = res;
	}
	public float getYRes() {
		return _yRes;
	}
	public void setYRes(float res) {
		_yRes = res;
	}
	public float getZRes() {
		return _zRes;
	}
	public void setZRes(float res) {
		_zRes = res;
	}
	/** Get a contiguous voxels set within the number of iterations of a breadth first search */
	public static Set<GraphNode> contiguousVoxelSet(GraphNode s, int iterations)
	{
		Set<GraphNode> nodeSet = new HashSet<GraphNode>();
		Graph graph = s.centerline.getGraph();
		List<GraphNode> g = graph.getNodes();
		
		for (GraphNode gn: g)
		{
			gn.pathLen = CenterlineCycles.WHITE;
			gn.pathCost = Float.MAX_VALUE;
			gn.predecessor = null;
			gn.graphed = false;
		}
		s.graphed = false;
		s.pathLen = CenterlineCycles.GRAY;
		s.pathCost = 0;
		s.predecessor = null;
		nodeSet.add(s);
		Queue<GraphNode> q = new LinkedList<GraphNode>();
		q.add(s);
		int i = 0;
		while (!q.isEmpty() && i < iterations)
		{
			GraphNode u = q.poll();
			for (GraphNode v: u.adjacents)
			{
				if (v.pathLen == CenterlineCycles.WHITE) // only consider centerlines 
				{
					v.pathLen = CenterlineCycles.GRAY;
					nodeSet.add(v);
					v.pathCost = u.pathCost+1;
					v.predecessor = u;
					q.add(v);
				}
			}
			u.pathLen = CenterlineCycles.BLACK;
			i++;
		}
		return nodeSet;
	}
	
	public List<GraphNode> narrowBifurcationsPath()
	{
		List<GraphNode> bifs = new LinkedList<GraphNode>();
		GraphNode s = this.getNarrowNodeAdjs().get(0);
		
		if (s.isCenterline == false)
			return bifs;
		
		Graph graph = s.centerline.getGraph();
		List<GraphNode> g = graph.getNodes();
		
		for (GraphNode gn: g)
		{
			gn.pathLen = CenterlineCycles.WHITE;
			gn.pathCost = Float.MAX_VALUE;
			gn.predecessor = null;
			gn.graphed = false;
		}
		s.graphed = false;
		s.pathLen = CenterlineCycles.GRAY;
		s.pathCost = 0;
		s.predecessor = null;
		
		Queue<GraphNode> q = new LinkedList<GraphNode>();
		q.add(s);
		int bifsFound = 0;
		while (!q.isEmpty() && bifsFound < 2)
		{
			GraphNode u = q.poll();
			if (u.isBifurcation)
			{
				bifs.add(u);
				bifsFound++;
			}
			
			for (GraphNode v: u.adjacents)
			{
				if (v.isCenterline && v.graphed == false && v.pathLen == CenterlineCycles.WHITE) // only consider centerlines 
				{
					v.pathLen = CenterlineCycles.GRAY;
					v.pathCost = u.pathCost+1;
					v.predecessor = u;
					q.add(v);
				}
			}
			u.pathLen = CenterlineCycles.BLACK;
		}
		if (bifs.size() == 2)
		{
			List<GraphNode> path = Centerline.pathBFS(bifs.get(0), bifs.get(1));
			// path.remove(0);	path.remove(path.size()-1);
			return path;
		}
		return bifs;
	}
}

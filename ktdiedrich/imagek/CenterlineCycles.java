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

import java.util.*;

/** Find cycles/loops on centerlines starting from a point. 
 * @author Karl Diedrich <ktdiedrich@gmail.com> 
 * */
public class CenterlineCycles 
{
	public static final int WHITE = 0;
	public static final int GRAY = 1;
	public static final int BLACK = 2;
	public static final int BACKUP = 60;
	private CenterlineCycles()
	{
		
	}
	/** Find centerline graph node cycle starting at a centerline graph node. 
	 * Uses Breadth First Search (BFS) using pathCost as distance and pathLen to store the 
	 * search status color and predecessor to store the predecessor.   
	 * @return empty set if not a cycle. */
	public static CenterlineCycle findCycle(List<GraphNode> patchNodes, int width, int height, int zSize, 
			float xRes, float yRes, float zRes)
	{
		
		GraphNode s = patchNodes.get(0);
		GraphNode end = patchNodes.get(patchNodes.size()-1);
		
		Set<GraphNode> cycle = new HashSet<GraphNode>();
		CenterlineCycle cc = new CenterlineCycle(cycle, xRes, yRes, zRes);
		if (s.isCenterline == false)
			return cc;
		
		Graph graph = s.centerline.getGraph();
		List<GraphNode> g = graph.getNodes();
		
		for (GraphNode gn: g)
		{
			gn.pathLen = WHITE;
			gn.pathCost = Float.MAX_VALUE;
			gn.predecessor = null;
			gn.graphed = false;
		}
		for (GraphNode pn: patchNodes)
		{
			pn.graphed = true;
		}
		s.graphed = false;
		s.pathLen = GRAY;
		s.pathCost = 0;
		s.predecessor = null;
		end.graphed = false;
		
		Queue<GraphNode> q = new LinkedList<GraphNode>();
		q.add(s);
		while (!q.isEmpty())
		{
			GraphNode u = q.poll();
			if (u == end)
			{
				IJ.log("Found end: "+end.coordinateString());
				break;
			}
			for (GraphNode v: u.adjacents)
			{
				if (v.isCenterline && v.graphed == false && v.pathLen == WHITE) // only consider centerlines 
				{
					v.pathLen = GRAY;
					v.pathCost = u.pathCost+1;
					v.predecessor = u;
					q.add(v);
				}
			}
			u.pathLen = BLACK;
		}
		cycle.add(s);
		
		int sumX=s.col, sumY=s.row, sumZ=s.z;
		GraphNode proximalNode = s, distalNode = s;
		GraphNode next = end;
		int midX = (int)Math.round((double)width/2.0);
		int midY = (int)Math.round((double)height/2.0);
		int midZ = (int)Math.round((double)zSize/2.0);
		double minR = Math.sqrt(  Math.pow(midX-s.col, 2) + Math.pow(midY-s.row, 2) + Math.pow(midZ-s.z, 2)  );
		double maxR = minR;
    	while (next != null && next != s)
    	{
    		double r = Math.sqrt(  Math.pow(midX-next.col, 2) + Math.pow(midY-next.row, 2) + Math.pow(midZ-next.z, 2)  );
    		if (r < minR)
    		{
    			minR = r;
    			proximalNode = next;
    		}
    		if (r > maxR)
    		{
    			distalNode = next;
    		}
    		sumX+=next.col;
    		sumY+=next.row;
    		sumZ+=next.z;
    		cycle.add(next);
    		next = next.predecessor;
    	}
    	// add the patch back to the cycle 
    	for (GraphNode pn: patchNodes)
		{
			cycle.add(pn);
		}
    	
    	// identify ICA loop at bottom of slab 
    	int cutOff = zSize - 2*zSize/3;
    	IJ.log("Loop proximal node: "+proximalNode.coordinateString()+" greater than: "+cutOff);
    	/*
    	cc.setProximalNode(proximalNode);
		
    	if (proximalNode.z > cutOff)
		{
			IJ.log("Brake loop from: "+proximalNode.coordinateString());
			backupCenterline(proximalNode, cc);
		}
		*/
		IJ.log("Cycle: "+cycle.size());
		
		return cc;
	}
	/** Backs up the cycle centerlines from cNode and removes them from cycle. Returns the set of
	 * nodes in the backing up front so they can be tied together. */
	public static  void backupCenterline(GraphNode cNode, CenterlineCycle cc)
	{
		Set<GraphNode> cycle = cc.getCycle();
		if (cNode.isCenterline == false)
		{
			return ;
		}
		Graph graph = cNode.centerline.getGraph();
		List<GraphNode> g = graph.getNodes();
		
		for (GraphNode gn: g)
		{
			gn.pathLen = WHITE;
			gn.pathCost = Float.MAX_VALUE;
			gn.predecessor = null;
			gn.graphed = false;
		}
		
		cNode.graphed = false;
		cNode.pathLen = GRAY;
		cNode.pathCost = 0;
		cNode.predecessor = null;
		
		Set<GraphNode> backedUpNodes = new HashSet<GraphNode>();
		List<GraphNode> cLine = cNode.centerline.getCenterlineNodes();
		boolean cRemoved = cLine.remove(cNode);
		cycle.remove(cLine);
		if (cRemoved == false)
		{
			IJ.log(cNode.coordinateString()+" not removed from breaking loop.");
		}
		backedUpNodes.add(cNode);
		Queue<GraphNode> q = new LinkedList<GraphNode>();
		q.add(cNode);
		int backUpCnt = 0;
		while (!q.isEmpty() && backUpCnt < BACKUP)
		{
			GraphNode u = q.poll();
			for (GraphNode v: u.adjacents)
			{
				if (v.isCenterline && v.graphed == false && v.pathLen == WHITE) // only consider centerlines 
				{
					v.isCenterline = false;
					List<GraphNode> line = v.centerline.getCenterlineNodes();
					boolean vRemoved = line.remove(v);
					
					if (vRemoved == false)
					{
						IJ.log(v.coordinateString()+" not removed from line.");
					}
					boolean cycRemov = cycle.remove(v);
					if (cycRemov == true)
					{
						// from cycle
						cc.addCycleFront(v);
					}
					else
					{
						// from the rest of the centerlines 
						cc.addBreakFront(v);
					}
					
					backedUpNodes.add(v);
					backUpCnt++;
					v.pathLen = GRAY;
					v.pathCost = u.pathCost+1;
					v.predecessor = u;
					q.add(v);
				}
			}
			u.pathLen = BLACK;
			cc.removeBreakFront(u);
			cc.removeCycleFront(u);
		}
		
		return;
	}
}

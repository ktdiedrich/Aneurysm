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
import java.util.List;
import java.util.Set;
import java.lang.Comparable;

/** For Djikstra's algorithm to find centerline. Data members are public so access 
 * functions aren't needed to save space this an object is kept for every artery voxel
 * in a linked list in the Graph.  
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class GraphNode implements Comparable<GraphNode>
{
    public float pathCost;
    public GraphNode predecessor;
    public int row, col, z;
    public float weight;
    public Set<GraphNode> adjacents;
    //public Set<GraphNode> adjacentCenterlines;
    public float mDFE; // modified distance from edge 
    public short dfe; // distance from edge 
    public short dfc; // distance from centerline 
    public short enhance; // enhancement for aneurysms 
    public boolean graphed;
    public Centerline centerline; // centerline the voxel belongs to 
    public int pathLen;
    public float dfm; // distance factor metric 
    public float cumDist;
    //public short intensity;
    public boolean isBifurcation;
    public boolean isCenterline;
    public CenterOfMass centerOfMass;
    public GraphNode centerlineNode; // the closest centerline node to this node 
    public CenterlineNodeData centerlineNodeData;
    public GraphNode(final Position position)
    {
        row = position.getRow();
        col = position.getColumn();
        z = position.getZ();
        adjacents = new HashSet<GraphNode>();
        //adjacentCenterlines = new HashSet<GraphNode>();
        pathCost = Float.MAX_VALUE;
        predecessor = null;
        pathLen = 0;
        centerOfMass = null;
    }
    /** Sort on pathCost */
    public int compareTo(GraphNode a)
    {   
        if (this.pathCost > a.pathCost)
            return 1;
        if (pathCost == a.pathCost)
            return 0;
        return -1;
    }
    public static void algorithmResetGraphs(List<Graph> graphs)
    {
    	for (Graph graph: graphs)
    	{
    		algorithmReset(graph);
    	}
    }
    public static void algorithmReset(Graph graph)
    {
    	List<GraphNode> nodes = graph.getNodes();
    	algorithmReset(nodes);
    }
    /** Reset a node to the initial state for algorithms to rerun on. */
    public static void algorithmReset(GraphNode node)
    {
    	node.pathCost = Float.MAX_VALUE;
    	node.predecessor = null;
    	node.graphed = false;
    	node.pathLen = 0;
    	node.dfm = 0.0f;
    	node.cumDist = 0.0f;
    }
    
    /** Reset a list of nodes to the starting algorithm state. */
    public static void algorithmReset(List<GraphNode> nodes)
    {
    	for (GraphNode node: nodes)
    	{
    		GraphNode.algorithmReset(node);
    	}
    }
    /** @return a string containing the col, row, z coordinates */
    public String coordinateString()
    {
    	return "("+this.col+", "+this.row+", "+this.z+")";
    }
    public static String toString(GraphNode node)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("Pos=");
        // TODO make Position(x, y, z)
        sb.append(new Position(node.col, node.row, node.z).toString());
        sb.append(" Path=");
        sb.append(node.pathCost);
        sb.append(" Len=");
        sb.append(node.pathLen);
        sb.append(" Wt=");
        sb.append(node.weight);
        sb.append(" DFE=");
        sb.append(node.dfe);
        sb.append(" MDFE=");
        sb.append(node.mDFE);
        sb.append(" Adj=");
        sb.append(node.adjacents.size());
        
        return sb.toString();
    } 
    /** The Euclidean distance between two graph nodes is their edge cost. Doesn't use resolution. */
	public static double distance(final GraphNode nodeA, final GraphNode nodeB)
	{
		double dCol = (double)(nodeA.col-nodeB.col);
		double dRow = (double)(nodeA.row-nodeB.row);
		double dZ = (double)(nodeA.z-nodeB.z);
		double d = Math.sqrt(dCol*dCol + dRow*dRow + dZ*dZ);
		return d;
	}
	// this may slow down algorithms using GraphNode 
	/*
	public boolean equals(Object obj)
	{
		GraphNode g = (GraphNode)obj;
		if (this.col == g.col && this.row == g.row && this.z == g.z)
		{
			return true;
		}
		return false;
	}
	*/
}

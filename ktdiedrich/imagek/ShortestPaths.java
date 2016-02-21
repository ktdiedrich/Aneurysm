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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import ktdiedrich.db.aneurysm.Inserts;

/** Calculates shortest paths using Dijkstra's algorithm
 * @author ktdiedrich@gmail.com
 * */
public class ShortestPaths 
{
	private int _pathLenLimit;
	private PhaseContrast _phaseContrast;
	private List<Cost> _costs;
	private double _pcWeight;
	private int _centerlineAlgorithm;
	public static String LABEL = "path\tpccost\tweight\n";
	
	private class Cost
	{
		private double _path;
		private double _pcCost;
		private double _weight;
		public Cost(double path, double pcCost, double weight)
		{
			_path = path;
			_pcCost = pcCost;
			_weight = weight;
		}
		public String toString()
		{
			return _path+"\t"+_pcCost+"\t"+_weight+"\n";
		}
	}
	public ShortestPaths()
	{
		_pathLenLimit = Integer.MAX_VALUE;
		_centerlineAlgorithm = Inserts.DFE_WEIGHTED_COM;
		_costs = new LinkedList<Cost>();
	}
	public void writeCosts(String fileName)
	{
		StringBuffer sb = new StringBuffer(LABEL);
		for (Cost c: _costs)
		{
			sb.append(c.toString());
		}
		PrintWriter out = null;
		try 
		{
			FileWriter outFile = new FileWriter(fileName);
			out = new PrintWriter(outFile);
			out.print(sb.toString());
			out.close();
		} 
		catch (IOException e)
		{
			StackTraceElement[] ste = e.getStackTrace();
			IJ.log("Writing file: "+fileName+": "+e.getMessage());
			for (int i=0; i < ste.length; i++)
			{
				IJ.log(ste[i].toString());
			}
			
		}
		finally
		{
			out.close();
		}
		if (_phaseContrast != null)
		{
			_phaseContrast.writeDots(fileName+".dots.txt");
		}
	}
	/** Sets a new path cost for node v if cost of u to v is lower than current cost of v. 
     * @return true if relax v, false if not */
    public boolean relax(GraphNode u, GraphNode v)
    {
        // IJ.log(""+v.weight);
    	float addCost = v.weight;
    	
    	double pcCost  = 0;
    	if (_phaseContrast != null)
    	{
    		if (_centerlineAlgorithm == Inserts.DFEWTCOM_MULT_PCCROSSNORM)
    		{
    			pcCost = _phaseContrast.velocityCostFunction(u, v);
    			addCost = (float)(addCost * pcCost);
    		}
    		else if (_centerlineAlgorithm == Inserts.VELOC_COST)
    		{
    			pcCost = _phaseContrast.velocityCostFunction(u, v);
    			addCost = (float)pcCost;
    		}
    	}
    	
    	float newCost = u.pathCost+addCost;
    	
        if (newCost < v.pathCost )
        {
            v.pathCost = newCost;
            _costs.add( new Cost(u.pathCost, pcCost, v.weight) );
            v.predecessor = u;
            v.pathLen = u.pathLen+1;
            return true;
        }
        
        return false;
    }
    public List<Graph> dijkstraLowestCostPaths(List<Graph> graphs, int zSize, VoxelDistance vd)
    {
        List<Graph> shortestPaths = new LinkedList<Graph>();
        
        for (Graph g: graphs)
        {
        	Graph shortest = dijkstraLowestCostPathsGraph(g, zSize, vd);
            shortestPaths.add(shortest);
        }
        return shortestPaths;
    }
    /** Find the lowest cost paths in the graph with Dijstra's algorithm with the Maximum Modified Distance From Edge point as the goal source node. */
    public Graph dijkstraLowestCostPathsGraph(Graph graph, int zSize, VoxelDistance vd)
    {
        graph.findBestSourceNode(vd);
        
        LinkedList<GraphNode> nodes = graph.getNodes();
        
        GraphNode srcNode = graph.getSourceNode();
        
        return dijkstraLowestCostPath(nodes, srcNode, zSize);
    }
    public Graph dijkstraLowestCostPath(LinkedList<GraphNode> queue, GraphNode source, int zSize)
    {
    	return dijkstraLowestCostPathTarget(queue,  source, zSize, null);
    }
    /** Find the lowest cost paths in the graph with Dijstra's algorithm. . 
     * @param source The source or goal node the lowest paths and heading towards. 
     * */
    public Graph dijkstraLowestCostPathTarget(LinkedList<GraphNode> queue, GraphNode source, int zSize, GraphNode target)
    {
    	IJ.log("Shortest path source node: "+source.coordinateString());
    	IJ.log("Shortest path nodes: "+queue.size());
    	IJ.log("ShortestPaths: Centerline algorithm ID: "+_centerlineAlgorithm);
    	GraphNode.algorithmReset(queue);
    	source.pathCost = (0.0F);
        PriorityQueue<GraphNode> pq = new PriorityQueue<GraphNode>();
        for (GraphNode n: queue)
        {
            pq.add(n);
        }
        Graph g = new Graph();
        
        g.setSourceNode(source);
        LinkedList<GraphNode> S = new LinkedList<GraphNode>();
        int zSizeM1 = zSize-1;
        boolean targetFound = false;
        //  short circuit when backtrace end found 
        while (!pq.isEmpty() && targetFound == false)
        {
            GraphNode u = pq.poll();
            
            // don't use the lowest plane in the image stack 
            if (u.z < zSizeM1)
            {
            	
            	S.add(u);
            	for (GraphNode v: u.adjacents)
            	{
            		if (relax(u, v)) // if v pathCost is changed 
            		{
            			// remove changed v and re-add to priority queue to reorder one element 
            			pq.remove(v);
            			pq.add(v);
            		}
            	}
            }
            if (target != null && u == target) // short circuit when target found 
            {  
            	targetFound = true;
            }
        }
        g.setNodes(S);
        return g;
    }
	public int getPathLenLimit() {
		return _pathLenLimit;
	}
	/** Don't overwrite path lengths over this limit so that they short to the beginning and are backtraced first. */
	public void setPathLenLimit(int pathLenLimit) {
		_pathLenLimit = pathLenLimit;
	}
	public PhaseContrast getPhaseContrast() {
		return _phaseContrast;
	}
	public void setPhaseContrast(PhaseContrast phaseContrast) {
		_phaseContrast = phaseContrast;
	}
	public double getPcWeight() {
		return _pcWeight;
	}
	public void setPcWeight(double pcWeight) {
		_pcWeight = pcWeight;
	}
	public int getCenterlineAlgorithm() {
		return _centerlineAlgorithm;
	}
	public void setCenterlineAlgorithm(int centerlineAlgorithm) {
		_centerlineAlgorithm = centerlineAlgorithm;
	}
}

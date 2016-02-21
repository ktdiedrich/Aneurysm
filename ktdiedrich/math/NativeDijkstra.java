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

package ktdiedrich.math;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ktdiedrich.imagek.Centerline;
import ktdiedrich.imagek.Centerlines;
import ktdiedrich.imagek.Graph;
import ktdiedrich.imagek.GraphNode;

/** Native code Djikstra algorithm
 * @author ktdiedrich@gmail.com 
 * */
public class NativeDijkstra 
{
	public native static void dijkstra(GraphNode[] nodes, int length);
	static 
	{
		System.loadLibrary("NativeDijkstra");
	}
	/* 
	protected static void testDFE(short[][][] cubicDFEs)
    {
        int zSize = cubicDFEs.length;
        int height = cubicDFEs[0].length;
        int width = cubicDFEs[0][0].length;
        short[][] dfes = MatrixUtil.cube2rect(cubicDFEs);
        
        List<Graph> graphs = Graph.makeGraphs(dfes, width, height, 0);
        
        
        Centerlines center = new Centerlines();
        center.setMinLineLength(4);
        center.modifyDFEs(graphs);
        
        center.weight(graphs);
        
        for (Graph g: graphs)
        {
            GraphNode sourceNode = g.findBestSourceNode(); // TODO needs VoxelDistance 
            sourceNode.pathCost = 0.0f;
        	List<GraphNode> nodes = g.getNodes();
        	GraphNode[] nodeList = new GraphNode[nodes.size()];
        	int i = 0;
        	for (GraphNode n: nodes)
        	{
        		nodeList[i] = n;
        		i++;
        	}
        	
        	NativeDijkstra.dijkstra(nodeList, nodeList.length);
        }
        
        //List<Centerline> centerlines = center.backTraceCenterlines(shortestPaths);
        
    }
    */
    public static void main(String[] args)
    {
    	// branching 
        short[][][] branching = {
        {
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
        },
        {
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,1,1,1,1,1,0,0,1,1,1,1,1,0}, 
            {0,0,0,0,0,0,1,1,1,1,0,0,0,1,0,0,0,0,0,0}, 
            {0,1,1,1,1,1,1,1,0,0,0,0,1,0,0,1,1,0,0,0}, 
            {0,1,1,1,1,1,1,1,1,1,1,1,0,0,1,0,0,0,0,0}, 
            {0,0,1,0,0,0,1,0,0,0,0,0,0,1,0,0,1,1,0,0}, 
            {0,0,1,0,0,0,0,1,0,0,0,1,1,0,0,1,0,0,0,0}, 
            {0,0,1,1,1,0,0,0,1,1,1,1,1,1,1,1,1,1,1,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
        },
        {
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
            {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}, 
        },
        };
        // testDFE(branching);
    	
    }
}

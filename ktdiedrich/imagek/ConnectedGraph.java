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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** Finds connected component graphs.
 * @author ktdiedrich@gmail.com
 * */
public class ConnectedGraph 
{
	private int _remaining;
	/** Make Graphs for voxel values 
     * @param lowThreshold Threshold out graphs below this number. If 0 don't threshold 
     * */
    public List<Graph> makeGraphs(short[][] voxels, int width, int height, int lowThreshold)
    {
        List<GraphNode> allNodes = makeNodeListDFE(voxels, width, height);
        return makeGraphs(allNodes, lowThreshold);
    }
    
    /** Make image voxel nodes into a list of connected component graphs. 
     * @return a separate Graph for each bunch of connected nodes. 
     * @param low threshold remove graphs below this count. */
    public List<Graph> makeGraphs(List<GraphNode> allNodes, int lowThreshold)
    {
    	
        List<Graph> graphs = new LinkedList<Graph>();
        _remaining = allNodes.size();
        GraphNode startNode = null;
        if (allNodes.size() > 0)
        {
        	startNode = allNodes.remove(0);
        }
        while ( _remaining > 0)
        {
            Graph sepGraph = this.separateGraphQ(startNode, allNodes);
            if (sepGraph.getNodes().size() > lowThreshold)
            {
            	graphs.add(sepGraph);
            }
            if (_remaining > 0)
            {
            	for (GraphNode gn: allNodes)
            	{
            		if (gn.graphed==false)
            		{
            			startNode = gn;
            			break;
            		}
            	}
            }
        }
        return graphs;
    }
    
    
    /** @param dfes DFE function value for each voxel of a 3D image
     *  */
    public static List<GraphNode> makeNodeListDFE(short[][] dfes, int width, int height)
    {
        int zSize = dfes.length;
        Map<String, GraphNode> nodePos = new HashMap<String, GraphNode>();
        List<GraphNode> allNodes = new LinkedList<GraphNode>();
       
        for (int z=1; z<(zSize-1); z++)
        {
            for (int row=1; row<(height-1); row++)
            {
                for (int col=1; col<(width-1); col++)
                {
                	int p = 0xFFFF & dfes[z][row*width+col]; 
                	
                    if ( p > 0)
                    {
                    	// IJ.log("Found DFE: "+p);
                        findAdjacentsDFE(new Position(col, row, z), dfes, width, nodePos, allNodes);
                    }
                }
            }
        }
        return allNodes;
    }
    
    public static List<GraphNode> makeNodeListWeight(float[][] weights, int width, int height)
    {
        int zSize = weights.length;
        Map<String, GraphNode> nodePos = new HashMap<String, GraphNode>();
        List<GraphNode> allNodes = new LinkedList<GraphNode>();
       
        for (int z=1; z<(zSize-1); z++)
        {
            for (int row=1; row<(height-1); row++)
            {
                for (int col=1; col<(width-1); col++)
                {
                    if (weights[z][row*width+col] > 0.0F)
                    {
                        findAdjacents(new Position(col, row, z), weights, width, nodePos, allNodes);
                    }
                }
            }
        }
        return allNodes;
    }
    
    
    
    public static void findAdjacentsDFE(Position p, short[][] dfes, int width, Map<String, GraphNode> nodePos, List<GraphNode> allNodes)
    {      
        int col = p.getColumn();
        int row = p.getRow();
        int z = p.getZ();
        GraphNode node = null;
        if (nodePos.containsKey(p.key()))
        {
            node = nodePos.get(p.key());
        }
        else
        {
            node = new GraphNode(p);
            int pt = 0xFFFF & dfes[z][row*width+col];
            node.dfe = (short)(0xFFFF & pt);
            // IJ.log("Set  DFE: "+node.dfe);
            nodePos.put(p.key(), node);
            allNodes.add(node);
        }
        
        Queue<Position> checkNeighborPos = p.getNeighbors();
        
        // check neighbors 
        while (checkNeighborPos.isEmpty() == false)
        {
           Position nextPos = checkNeighborPos.remove();
           int r = nextPos.getRow();
           int c = nextPos.getColumn();
           int zz = nextPos.getZ();
           int dfePt = 0xFFFF & dfes[zz][r*width+c]; 
           if ( dfePt > 0)
           {
               GraphNode adjNode = null;
               if (nodePos.containsKey(nextPos.key()))
               {
                   adjNode = nodePos.get(nextPos.key());
               }
               else
               {
                   adjNode = new GraphNode(new Position(c, r, zz));
                   adjNode.dfe = (short)(0xFFFF & dfePt);
                   nodePos.put(nextPos.key(), adjNode); 
                   allNodes.add(adjNode);
               }
               // check position for adjacency 
               int colDis = Math.abs(node.col-adjNode.col);
               int rowDis = Math.abs(node.row-adjNode.row);
               int zDis = Math.abs(node.z-adjNode.z);
               if (colDis<=1 && rowDis<=1 && zDis<=1)
               {
            	   node.adjacents.add(adjNode);
               }
               else
               {
            	   // TODO shouldn't be any, this is a double check 
            	    IJ.log("Node: "+node.coordinateString()+" not adjacent to: "+adjNode.coordinateString());
               }
           }
        }
    }    
    
    /** Sets the weights of the nodes. */
    public static void findAdjacents(Position p, float[][] weights, int width, Map<String, GraphNode> nodePos, List<GraphNode> allNodes)
    {      
        int col = p.getColumn();
        int row = p.getRow();
        int z = p.getZ();
        GraphNode node = null;
        if (nodePos.containsKey(p.key()))
        {
            node = nodePos.get(p.key());
        }
        else
        {
            node = new GraphNode(p);
            node.weight = (weights[z][row*width+col]);
            nodePos.put(p.key(), node);
            allNodes.add(node);
        }
        
        Queue<Position> checkNeighborPos = p.getNeighbors();
        
        // check neighbors 
        while (checkNeighborPos.isEmpty() == false)
        {
           Position nextPos = checkNeighborPos.remove();
           int r = nextPos.getRow();
           int c = nextPos.getColumn();
           int zz = nextPos.getZ();
           if (weights[zz][r*width+c] > 0)
           {
               GraphNode adjNode = null;
               if (nodePos.containsKey(nextPos.key()))
               {
                   adjNode = nodePos.get(nextPos.key());
               }
               else
               {
                   adjNode = new GraphNode(new Position(c, r, zz));
                   adjNode.weight = (weights[zz][r*width+c]);
                   nodePos.put(nextPos.key(), adjNode); 
                   allNodes.add(adjNode);
               }
               // check position for adjacency 
               int colDis = Math.abs(node.col-adjNode.col);
               int rowDis = Math.abs(node.row-adjNode.row);
               int zDis = Math.abs(node.z-adjNode.z);
               if (colDis<=1 && rowDis<=1 && zDis<=1)
               {
            	   node.adjacents.add(adjNode);
               }
               else
               {
            	   // TODO shouldn't be any, this is a double check 
            	    IJ.log("Node: "+node.coordinateString()+" not adjacent to: "+adjNode.coordinateString());
               }
           }
        }
    }    
    
    /** Make graph nodes for the cost weights for Dijkstra's cost weights. */
    public static void findWeightAdjacents(Position p, float[][] costWeights, int width, Map<String, GraphNode> nodePos, List<GraphNode> allNodes)
    {      
        int col = p.getColumn();
        int row = p.getRow();
        int z = p.getZ();
        GraphNode node = null;
        if (nodePos.containsKey(p.key()))
        {
            node = nodePos.get(p.key());
        }
        else
        {
            node = new GraphNode(p);
            node.weight = (costWeights[z][row*width+col]);
            nodePos.put(p.key(), node);
            allNodes.add(node);
        }
        
        Queue<Position> checkNeighborPos = p.getNeighbors();
        
        // check neighbors 
        while (checkNeighborPos.isEmpty() == false)
        {
           Position nextPos = checkNeighborPos.remove();
           int r = nextPos.getRow();
           int c = nextPos.getColumn();
           int zz = nextPos.getZ();
           if (costWeights[zz][r*width+c] > 0)
           {
               GraphNode adjNode = null;
               if (nodePos.containsKey(nextPos.key()))
               {
                   adjNode = nodePos.get(nextPos.key());
               }
               else
               {
                   adjNode = new GraphNode(new Position(c, r, zz));
                   adjNode.weight = (costWeights[zz][r*width+c]);
                   nodePos.put(nextPos.key(), adjNode); 
                   allNodes.add(adjNode);
               }
               // check position for adjacency 
               int colDis = Math.abs(node.col-adjNode.col);
               int rowDis = Math.abs(node.row-adjNode.row);
               int zDis = Math.abs(node.z-adjNode.z);
               if (colDis<=1 && rowDis<=1 && zDis<=1)
               {
            	   node.adjacents.add(adjNode);
               }
               else
               {
            	   // TODO shouldn't be any, this is a double check 
            	    IJ.log("Node: "+node.coordinateString()+" not adjacent to: "+adjNode.coordinateString());
               }
           }
        }
    }    
    /** @param costWeights Dijktra's cost weight input function value for each voxel of a 3D image
     *  */
    public static List<GraphNode> makeCostWeightNodeList(float[][] costWeights, int width, int height)
    {
        int zSize = costWeights.length;
        Map<String, GraphNode> nodePos = new HashMap<String, GraphNode>();
        List<GraphNode> allNodes = new LinkedList<GraphNode>();
       
        for (int z=1; z<(zSize-1); z++)
        {
            for (int row=1; row<(height-1); row++)
            {
                for (int col=1; col<(width-1); col++)
                {
                    if (costWeights[z][row*width+col] > 0.0F)
                    {
                        findWeightAdjacents(new Position(col, row, z), costWeights, width, nodePos, allNodes);
                    }
                }
            }
        }
        return allNodes;
    }
    /** Separate the nodes into connected component graphs using a queue to reduce memory from
     * the recursive implementation. 
     * @param codeCount passed is as a reference so function can update the count. */
    public Graph separateGraphQ(GraphNode startNode, List<GraphNode> allNodes)
    {
    	Queue<GraphNode> queue = new LinkedList<GraphNode>();
    	
        Graph graph = new Graph();
        graph.addNode(startNode);
        queue.add(startNode);
        startNode.graphed = true;
        _remaining--;
        
        while (queue.isEmpty() == false)
        {
            GraphNode node = queue.remove();
            
            for (GraphNode adjNode: node.adjacents)
            {
                if (adjNode.graphed == false)
                {
                    graph.addNode(adjNode);
                    queue.add(adjNode);
                    _remaining--;
                    adjNode.graphed = true;
                }
            }
        }
        return graph;
    }
}

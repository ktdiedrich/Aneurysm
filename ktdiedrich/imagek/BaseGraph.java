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

import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.text.TextWindow;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/** Generix Graph class. 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class BaseGraph<T extends Comparable<T> >
{
    
    private LinkedList<BaseNode<T> > _nodes;
    private BaseNode<T> _sourceNode;
    public BaseGraph()
    {
        _nodes = new LinkedList<BaseNode<T> >();
    }
    public BaseGraph(LinkedList<BaseNode<T> > nodes)
    {
        _nodes = nodes;
    }
    public BaseGraph(LinkedList<BaseNode<T> > nodes, BaseNode<T> sourceNode)
    {
        _nodes = nodes;
        _sourceNode = sourceNode;
    }
    public void addNode(BaseNode<T> node)
    {
        _nodes.add(node);
    }
    public LinkedList<BaseNode<T> > getNodes()
    {
        return _nodes;
    }
    public void setNodes(LinkedList<BaseNode<T> > nodes)
    {
        _nodes = nodes;
    }
    public BaseNode<T> getSourceNode()
    {
        return _sourceNode;
    }
    /** Set the source goal node of the graph. All paths will lead back to this node.  */
    public void setSourceNode(BaseNode<T> sourceNode)
    {
        _sourceNode = sourceNode;
    }
    
    // For short
    /** @param voxels Cost function value for each voxel of a 3D image 
     * @param cost is the weights if the nodes in the graph 
     * @return a list of nodes.with adjacency lists. */
    public List<BaseNode<Short> > makeAdjList(short[][] voxels, int width, int height)
    {
        int zSize = voxels.length;
        Map<String, BaseNode<Short> > nodePos = new HashMap<String, BaseNode<Short> >();
        List<BaseNode<Short> > allNodes = new LinkedList<BaseNode<Short> >();
       
        for (int z=1; z<(zSize-1); z++)
        {
            for (int row=1; row<(height-1); row++)
            {
                for (int col=1; col<(width-1); col++)
                {
                    if (voxels[z][row*width+col] > 0.0F)
                    {
                        findAdjacents(new Position(col, row, z), voxels, width, nodePos, allNodes);
                    }
                }
            }
        }
        return allNodes;
    }
    public void findAdjacents(Position p, short[][] voxels, int width, 
            Map<String, BaseNode<Short> > nodePos, 
            List<BaseNode<Short> > allNodes)
    {      
        int col = p.getColumn();
        int row = p.getRow();
        int z = p.getZ();
        BaseNode<Short> node = null;
        if (nodePos.containsKey(p.key()))
        {
            node = nodePos.get(p.key());
        }
        else
        {
            node = new BaseNode<Short>(voxels[z][row*width+col], p);
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
           if (voxels[zz][r*width+c] > 0.0F)
           {
               BaseNode<Short> adjNode = null;
               if (nodePos.containsKey(nextPos.key()))
               {
                   adjNode = nodePos.get(nextPos.key());
               }
               else
               {
            	   // TODO make Position(x, y, z)
                   adjNode = new BaseNode<Short>(voxels[zz][r*width+c], new Position(c, r, zz));
                   nodePos.put(nextPos.key(), adjNode); 
                   allNodes.add(adjNode);
               }
               node.addAdjacent(adjNode);
           }
        }
    }
    public List<BaseGraph<Short> > makeGraphs(short[][] voxels, int width, int height)
    {
        List<BaseNode<Short> > allNodes = makeAdjList(voxels, width, height);
        return makeGraphs(allNodes);
    }
    public List<BaseGraph<Short> > makeGraphs(List<BaseNode<Short> > allNodes)
    {
        List<BaseGraph<Short> > graphs = new LinkedList<BaseGraph<Short> >();
        while (allNodes.size() > 0)
        {
            BaseNode<Short> startNode = allNodes.remove(0);
            BaseGraph<Short> sepGraph = separateGraph(startNode, allNodes);
            graphs.add(sepGraph);
        }
        return graphs;
    }
    public BaseGraph<Short> separateGraph(BaseNode<Short> startNode, List<BaseNode<Short> > allNodes)
    {
        BaseGraph<Short> graph = new BaseGraph<Short>();
        addAdjacents(graph, startNode, allNodes);
        return graph;
    }
    
    public void addAdjacents(BaseGraph<Short> graph, BaseNode<Short> node, List<BaseNode<Short> > allNodes)
    {
        graph.addNode(node);
        allNodes.remove(node);
        node.setGraphed(true);
        for (BaseNode<Short> adjNode: node.getAdjacents())
        {
            if (adjNode.isGraphed()==false)
                addAdjacents(graph, adjNode, allNodes);
        }
    }
    
 // For float
    /** @param voxels Cost function value for each voxel of a 3D image 
     * @param cost is the weights if the nodes in the graph 
     * @return a list of nodes.with adjacency lists. */
    public List<BaseNode<Float> > makeAdjList(float[][] voxels, int width, int height)
    {
        int zSize = voxels.length;
        Map<String, BaseNode<Float> > nodePos = new HashMap<String, BaseNode<Float> >();
        List<BaseNode<Float> > allNodes = new LinkedList<BaseNode<Float> >();
       
        for (int z=1; z<(zSize-1); z++)
        {
            for (int row=1; row<(height-1); row++)
            {
                for (int col=1; col<(width-1); col++)
                {
                    if (voxels[z][row*width+col] > 0.0F)
                    {
                        findAdjacents(new Position(col, row, z), voxels, width, nodePos, allNodes);
                    }
                }
            }
        }
        return allNodes;
    }
    public void findAdjacents(Position p, float[][] voxels, int width, 
            Map<String, BaseNode<Float> > nodePos, 
            List<BaseNode<Float> > allNodes)
    {      
        int col = p.getColumn();
        int row = p.getRow();
        int z = p.getZ();
        BaseNode<Float> node = null;
        if (nodePos.containsKey(p.key()))
        {
            node = nodePos.get(p.key());
        }
        else
        {
            node = new BaseNode<Float>(voxels[z][row*width+col], p);
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
           if (voxels[zz][r*width+c] > 0.0F)
           {
               BaseNode<Float> adjNode = null;
               if (nodePos.containsKey(nextPos.key()))
               {
                   adjNode = nodePos.get(nextPos.key());
               }
               else
               {
            	   // TODO make Position(x, y, z)
                   adjNode = new BaseNode<Float>(voxels[zz][r*width+c], new Position(c, r, zz));
                   nodePos.put(nextPos.key(), adjNode); 
                   allNodes.add(adjNode);
               }
               node.addAdjacent(adjNode);
           }
        }
    }
    public List<BaseGraph<Float> > makeGraphs(float[][] voxels, int width, int height)
    {
        List<BaseNode<Float> > allNodes = makeAdjList(voxels, width, height);
        return makeFloatGraphs(allNodes);
    }
    public List<BaseGraph<Float> > makeFloatGraphs(List<BaseNode<Float> > allNodes)
    {
        List<BaseGraph<Float> > graphs = new LinkedList<BaseGraph<Float> >();
        while (allNodes.size() > 0)
        {
            BaseNode<Float> startNode = allNodes.remove(0);
            BaseGraph<Float> sepGraph = separateFloatGraph(startNode, allNodes);
            graphs.add(sepGraph);
        }
        return graphs;
    }
    public BaseGraph<Float> separateFloatGraph(BaseNode<Float> startNode, List<BaseNode<Float> > allNodes)
    {
        BaseGraph<Float> graph = new BaseGraph<Float>();
        addFloatAdjacents(graph, startNode, allNodes);
        return graph;
    }
    
    public void addFloatAdjacents(BaseGraph<Float> graph, BaseNode<Float> node, List<BaseNode<Float> > allNodes)
    {
        graph.addNode(node);
        allNodes.remove(node);
        node.setGraphed(true);
        for (BaseNode<Float> adjNode: node.getAdjacents())
        {
            if (adjNode.isGraphed()==false)
                addFloatAdjacents(graph, adjNode, allNodes);
        }
    }
    
    public static short[][] makeShort(BaseGraph<Short> graph, int width, int height, int zSize)
    {
        LinkedList<BaseNode<Short> > nodes = graph.getNodes();
        short[][] voxels = new short[zSize][width*height];
        for (BaseNode<Short> n: nodes)
        {
            Position p = n.getPosition();
            voxels[p.getZ()][p.getRow()*width+p.getColumn()] = n.getValue();
        }
        return voxels;
    }
    
    public static float[][] makeFloat(BaseGraph<Float> graph, int width, int height, int zSize)
    {
        LinkedList<BaseNode<Float> > nodes = graph.getNodes();
        float[][] voxels = new float[zSize][width*height];
        for (BaseNode<Float> n: nodes)
        {
            Position p = n.getPosition();
            voxels[p.getZ()][p.getRow()*width+p.getColumn()] = n.getValue();
        }
        return voxels;
    }
    
    public static short[][] makeShort(List<BaseGraph<Short> > graphs, int width, int height, int zSize)
    {
        short[][] voxels = new short[zSize][width*height];
        for (BaseGraph<Short> graph: graphs)
        {
            LinkedList<BaseNode<Short> > nodes = graph.getNodes();
            for (BaseNode<Short> n: nodes)
            {
                Position p = n.getPosition();
                voxels[p.getZ()][p.getRow()*width+p.getColumn()] = n.getValue();
            }
        }
        return voxels;
    }
    public static float[][] makeFloat(List<BaseGraph<Float> > graphs, int width, int height, int zSize)
    {
        float[][] voxels = new float[zSize][width*height];
        for (BaseGraph<Float> graph: graphs)
        {
            LinkedList<BaseNode<Float> > nodes = graph.getNodes();
            for (BaseNode<Float> n: nodes)
            {
                Position p = n.getPosition();
                voxels[p.getZ()][p.getRow()*width+p.getColumn()] = n.getValue();
            }
        }
        return voxels;
    }
    public short[][] makeAdjacentSize(BaseGraph<T> graph, int width, int height, int zSize)
    {
        LinkedList<BaseNode<T> > nodes = graph.getNodes();
        short[][] voxels = new short[zSize][width*height];
        for (BaseNode<T> n: nodes)
        {
            Position p = n.getPosition();
            voxels[p.getZ()][p.getRow()*width+p.getColumn()] = (short)(n.getAdjacents().size());
        }
        return voxels;
    }
    public short[][] makeAdjacentSize(List<BaseGraph<T> > graphs, int width, int height, int zSize)
    {
        short[][] voxels = new short[zSize][width*height];
        
        for (BaseGraph<T> graph: graphs)
        {
            LinkedList<BaseNode<T> > nodes = graph.getNodes();
        
            for (BaseNode<T> n: nodes)
            {
                Position p = n.getPosition();
                voxels[p.getZ()][p.getRow()*width+p.getColumn()] = (short)n.getAdjacents().size();
            }
        }
        return voxels;
    }
}

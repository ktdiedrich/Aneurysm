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
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/** Connected component Graph of a 3D image in adjacency list form. Multiple 
 * connected components are kept in a List<Graph>. 
 * Each node in the Graph contains it's (x, y,z) position. 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Graph
{
    
    private LinkedList<GraphNode> _nodes;
    private GraphNode _sourceNode;
    private int _carotidScore;
    public Graph()
    {
        this(new LinkedList<GraphNode>(), null);
    }
    public Graph(LinkedList<GraphNode> nodes, GraphNode sourceNode)
    {
        _nodes = nodes;
        _sourceNode = sourceNode;
        _carotidScore = 0;
    }
    public void addNode(GraphNode node)
    {
        _nodes.add(node);
    }
    public void addNodes(List<GraphNode> nodes)
    {
    	for (GraphNode node: nodes)
    	{
    		_nodes.add(node);
    	}
    }
    public LinkedList<GraphNode> getNodes()
    {
        return _nodes;
    }
    public void setNodes(LinkedList<GraphNode> nodes)
    {
        _nodes = nodes;
    }
    public GraphNode getSourceNode()
    {
        return _sourceNode;
    }
    /** Set the source goal node of the graph. All paths will lead back to this node.  */
    public void setSourceNode(GraphNode sourceNode)
    {
        _sourceNode = sourceNode;
        // IJ.log("Set graph Source node: "+GraphNode.toString(_sourceNode));
    }
    /** Find max MDFE source node or minimum weight source node 
     * @return reference to source node */
    public GraphNode findBestSourceNode(VoxelDistance vd)
    {
    	List<GraphNode> bestTies = new LinkedList<GraphNode>();
    	boolean hasMDFE = false;
    	for (GraphNode n: _nodes)
    	{
    		if (n.mDFE > 0)
    		{
    			hasMDFE= true;
    			break;
    		}
    	}
    	if (!hasMDFE)
        {
        	IJ.log("Finding minimum non zero weight root node. ");
        	float minWeight = Float.MAX_VALUE;
        	for (GraphNode node: _nodes)
        	{
        		if (node.weight!=0 && node.weight<= minWeight && node.adjacents.size() > 0)
        		{
        			if (node.weight < minWeight)
        			{
        				bestTies.clear();	
        			}
        			minWeight = node.weight;
        			bestTies.add(node);
        		}
        	}
        }
    	else
    	{
    		IJ.log("Finding max MDFE root node ");
	        float maxMDFE = 0.0F;
	        
	        for (GraphNode node: _nodes)
	        {
	            float mdfe = node.mDFE;
	            if (mdfe >= maxMDFE && node.adjacents.size() > 0)
	            {
	            	if (mdfe > maxMDFE)
	            	{
	            		bestTies.clear();
	            	}
	            	maxMDFE = mdfe;
	            	bestTies.add(node);
	            }
	        }
    	}
    	int btLen = bestTies.size(); 
    	IJ.log("Best ties: "+btLen);
    	if (btLen == 1)
    	{
    		_sourceNode = bestTies.get(0);
    	}
    	else
    	{
    		int sumX=0, sumY=0, sumZ=0;
    		for (GraphNode b: bestTies)
    		{
    			sumX+=b.col;
    			sumY+=b.row;
    			sumZ+=b.z;
    		}
    		double aveX = (double)sumX/(double)btLen;
    		double aveY = (double)sumY/(double)btLen;
    		double aveZ = (double)sumZ/(double)btLen;
    		double closestAve=Double.MAX_VALUE;
    		for (GraphNode b: bestTies)
    		{
    			double d = vd.distance(b, aveX, aveY, aveZ);
    			if (d < closestAve)
    			{
    				closestAve = d;
    				_sourceNode = b;
    			}
    		}
    	}
        IJ.log("Graph size: "+_nodes.size()+" Source Node: "+GraphNode.toString(_sourceNode));
        return _sourceNode;
    }
    /** @return number of nodes in graph */
    public int nodeCount()
    {
        return _nodes.size();
    }
    /** @return the maximum Z value (deepest value) in the graph. */
    public int maxZ()
    {
    	int maxZ = 0;
    	for (GraphNode gn: _nodes)
    	{
    		if (gn.z > maxZ)
    			maxZ = gn.z;
    	}
    	return maxZ;
    }
    /** Lower score is more likely carotid artery */
    public int getCarotidScore()
    {
        return _carotidScore;
    }
    public void setCarotidScore(int carotidScore)
    {
        _carotidScore = carotidScore;
    }
    /** Add to the carotid Score, lower is more likely a carotid artery. */
    public void addCarotidScore(int add)
    {
        _carotidScore+=add;
    }
    
    /** Return the GraphNode at the position or null if not found. */
    public GraphNode findNode(Position pos)
    {
    	GraphNode found = null;
    	for (GraphNode gn: this.getNodes())
    	{
    		if (gn.col == pos.getColumn() && gn.row == pos.getRow() && gn.z == pos.getZ())
    		{
    			return gn;
    		}
    	}
    	
    	return found;
    }
    
    // Static algorithm methods
    
    public static void findBestSourceNodes(List<Graph> graphs, VoxelDistance vd)
    {
        for (Graph graph: graphs)
        {
            graph.findBestSourceNode(vd);
        }
    }
    
    public static void makeWeightImage(Graph graph, int width, int height, int zSize, String name)
    {
        LinkedList<GraphNode> nodes = graph.getNodes();
        float[][] voxels = new float[zSize][width*height];
        for (GraphNode n: nodes)
        {
            voxels[n.z][n.row*width+n.col] = n.weight;
        }
        ImageProcess.display(voxels, width, height, name);
    }
    public static void displayAdjacentSize(Graph graph, int width, int height, int zSize, String name)
    {
        LinkedList<GraphNode> nodes = graph.getNodes();
        short[][] voxels = new short[zSize][width*height];
        for (GraphNode n: nodes)
        {
            voxels[n.z][n.row*width+n.col] = (short)n.adjacents.size();
        }
        ImageProcess.display(voxels, width, height, name);
    }
    public static ImagePlus pathCostImage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        float[][] voxels = new float[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            IJ.log("Drawing path cost image size: "+nodes.size());
            int drawn  = 0;
            int maxVals = 0;
            for (GraphNode n: nodes)
            {
            	if (n.pathCost == Float.MAX_VALUE) maxVals++;
                if (n.pathCost < Float.MAX_VALUE)
                {
                    voxels[n.z][n.row*width+n.col] = n.pathCost;
                    drawn++;
                }
            }
            IJ.log("Values drawn: "+drawn+" max values: "+maxVals);
        }
        
        ImagePlus image = ImageProcess.makeImage(voxels, width, height, name);
        
        return image;
    }
    
    public static ImagePlus pathLengthImage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        short[][] voxels = new short[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            
            for (GraphNode n: nodes)
            {
                if (n.pathCost < Float.MAX_VALUE)
                {
                    voxels[n.z][n.row*width+n.col] = (short)n.pathLen;
                }
            }
        }
        ImagePlus image = ImageProcess.makeImage(voxels, width, height, name);
        
        return image;
    }
    
    public static ImagePlus makeWeightImage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        float[][] voxels = new float[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.weight;
            }
        }
        return ImageProcess.makeImage(voxels, width, height, name);
    }
    public static ImagePlus makeDFEimage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        short[][] voxels = new short[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] =  (short) (0xFFFF & n.dfe);
            }
        }
        ImagePlus image = ImageProcess.makeImage(voxels, width, height, name);
        return image;
    }
    
    /** Fill in missing grayscale values to a color image graph. Used to make a grayscale image with significance
     *  colors. 
     *  @param image color image 
     *  @param sourceImageGraphs image graphs to fill in */
    public static void fillImage(ImagePlus image, List<Graph> sourceImageGraphs)
    {
    	int zSize = image.getStackSize();
    	ImageStack stack = image.getImageStack();
    	ImageProcessor[] ips = new ImageProcessor[zSize];
    	for (int i=0; i < zSize; i++)
    	{
    		ips[i] = stack.getProcessor(i+1);
    	}
    	Colors colors = Colors.getColors();
    	
        for (Graph graph: sourceImageGraphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                if (ips[n.z].getPixel(n.col, n.row) == 0)
                {
                	ips[n.z].putPixel(n.col, n.row, colors.gray);
                }
            }
        }
    }
    /** Sets all voxels to value one; especially useful for displaying the centerline. */
    public static ImagePlus displayOne(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        short[][] voxels = new short[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = 1;
            }
        }
        return ImageProcess.display(voxels, width, height, name);
    }
    
    public static ImagePlus makeMDFEImage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        float[][] voxels = new float[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.mDFE;
            }
        }
        return ImageProcess.makeImage(voxels, width, height, name);
    }
    /** Display Distance Factor Metric tortuosity measure from centerline graphs. */
    public static void displayDFM(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        float[][] voxels = new float[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.dfm;
            }
        }
        ImageProcess.display(voxels, width, height, name);
    }
    public static void displayAdjacentSize(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        short[][] voxels = new short[zSize][width*height];
        
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
        
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = (short)n.adjacents.size();
            }
        }
        ImageProcess.display(voxels, width, height, name);
    }
    /* Sort graph greatest/deepest Z to least/shallowest Z. */
    public static class ZDeeperComparator implements Comparator<Graph> 
    {
        public int compare (Graph a, Graph b)
        {
            if (a.maxZ() < b.maxZ())
                return 1;
            if (a.maxZ() == b.maxZ())
                return 0;
            return -1;
        }
    }
    /* Sort graph most nodes to least. */
    public static class MoreNodesComparator implements Comparator<Graph>
    {
        public int compare (Graph a, Graph b)
        {
            if (a.nodeCount() < b.nodeCount())
                return 1;
            if (a.nodeCount() == b.nodeCount())
                return 0;
            return -1;
        }
    }
    /* Sort graphs lowest to highest carotid score. */
    public static class LowestCarotidScoreComparator implements Comparator<Graph>
    {
        public int compare (Graph a, Graph b)
        {
            if (a.getCarotidScore() < b.getCarotidScore())
                return -1;
            if (a.getCarotidScore() == b.getCarotidScore())
                return 0;
            return 1;
        }
    }
    
    public static ImagePlus makeColorImage(List<CenterlineGraph> centGraphs, int width, int height, int zSize, 
    		String title, int showLines, boolean colorBelong)
    {
    	
    	ImageStack stack = new ImageStack(width, height);
    	ImageProcessor[] procs = new ColorProcessor[zSize];
    	Colors colors = Colors.getColors();
        
    	for (int i=0; i< zSize; i++)
    	{
    		ImageProcessor proc = new ColorProcessor(width, height);
    		procs[i] = proc;
    		stack.addSlice(""+i, proc);
    	}
    	int ln = 0;
    	for (CenterlineGraph g: centGraphs)
    	{
    		
    		for (Centerline c: g.getCenterlines())
    		{
    			 
    			List<GraphNode> sur = c.getSurroundNodes();
    			for (GraphNode sn: sur)
    			{
    				procs[sn.z].putPixel(sn.col, sn.row, colors.surround);
    				// if (sn.weight==0) procs[sn.z].putPixel(sn.col, sn.row, colors.orchid);
    			}
    			
    			int[] lineColor = c.getRgb();
    			
    			if (lineColor == null)
    			{
    				lineColor = colors.centerline;
    			}
				 
    			for (GraphNode cn: c.getCenterlineNodes())
				{
					if (colorBelong)
					{
						int[] clr = colors.randomColor(80);
						int [] sClr = Colors.strengthen(clr, 40);
						for (GraphNode bn: cn.centerlineNodeData.getBelongingNodes())
						{
							procs[bn.z].putPixel(bn.col, bn.row, clr);
						}
						//procs[cn.z].putPixel(cn.col, cn.row, colors.centerline);
						procs[cn.z].putPixel(cn.col, cn.row, sClr);
					}
					else
					{
						procs[cn.z].putPixel(cn.col, cn.row, lineColor);
					}
					// if (cn.weight==0) procs[cn.z].putPixel(cn.col, cn.row, colors.orchid);
				}
				
    			GraphNode bif = c.getBifurcationNode();
    			if (bif != null && bif.isCenterline == true)
    			{
    				procs[bif.z].putPixel(bif.col, bif.row, colors.bifurcation);
    			}
    			
    			ln++;
    		}
    		List<CenterlineCycle> cycles = g.getCycles();
    		
    		for (CenterlineCycle c: cycles)
    		{
    			List<GraphNode> narrowAdjs = c.getNarrowNodeAdjs();
    			
    			/*  
    			for (GraphNode nn: narrowAdjs)
    			{
    				int[] narClr = colors.randomColor(180);
    				int[] narSclr = Colors.strengthen(narClr, 60);
    				if (nn.centerlineNodeData != null)
    				{
    					for (GraphNode b: nn.centerlineNodeData.getBelongingNodes())
    					{
    						procs[b.z].putPixel(b.col, b.row, narClr);
    					}
    				}
    				procs[nn.z].putPixel(nn.col, nn.row, narSclr);
    			}
    			*/
    			for (GraphNode sn: c.narrowBifurcationsPath())
    			{
    				procs[sn.z].putPixel(sn.col, sn.row, colors.orange);
    			}
    			GraphNode narrowest = narrowAdjs.get(0);
    			procs[narrowest.z].putPixel(narrowest.col, narrowest.row, colors.mark);
    			//List<GraphNode> narrowBifs = c.narrowBifurcationsPath();
    			//for (GraphNode nb: narrowBifs)
    			//{
    				//procs[nb.z].putPixel(nb.col, nb.row, colors.orange);
    			//}
    			//GraphNode narPt = narrow.get(0);
    			//procs[narPt.z].putPixel(narPt.col, narPt.row, colors.mark);
    		}
    		// label the source/goal node 
    		GraphNode srcNode = g.getSourceNode();
    		/*
    		if (srcNode.centerlineNodeData!=null)
    		{
    			for (GraphNode n: srcNode.centerlineNodeData.getBelongingNodes())
    			{
    				procs[n.z].putPixel(n.col, n.row, colors.surround);
    			}
    		}*/
    		if (srcNode != null)
    		{
    			procs[srcNode.z].putPixel(srcNode.col, srcNode.row, colors.source);
    		}
    		
    		// label the ends of the centerline tree 
    		Set<GraphNode> treeEnds = g.getTreeEnds();
    		for (GraphNode end: treeEnds)
    		{
    			procs[end.z].putPixel(end.col, end.row, colors.cyan);
    		}
    		
    	}
    	ImagePlus im = new ImagePlus(title, stack);
    	return im;
    }
    /** Make a path from the end to the source following the predecessor links  */
	public static List<GraphNode> backtracePred(GraphNode source, GraphNode end)
	{
		List<GraphNode> path = new LinkedList<GraphNode>();
		//path.add(end);
    	GraphNode next = end.predecessor;
    	while (next != null && next != source)
    	{
    		path.add(next);
    		next = next.predecessor;
    	}
    	//path.add(source);
    	return path;
	}
}

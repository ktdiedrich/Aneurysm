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
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Random;

/** The Node in addNode() and getNodes() are the centerline nodes.
 * Surround Node represents the other artery voxels that surround that centerline.  
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Centerline 
{
	public Centerline()
	{
		_centerlineNodes = new LinkedList<GraphNode>();
		_surroundNodes = new LinkedList<GraphNode>();
	}
	// Location to check the width of the artery around the centerline
	// looking for the ICA it is the thickest artery.
	// Can't check DFE at the true end because the artery often has holes at the end 
    public static final int CHECK_DFE_POS = 5; 
    public static final int WINDOW_SIZE = 4;
	private List<GraphNode> _centerlineNodes;
	private GraphNode _bifurcationNode;
	private int _carotidScore;
	private CenterlineGraph _centerlineGraph;
	private Graph _graph;
	private List<GraphNode> _surroundNodes;
	private float _averageX, _averageY, _averageZ, _averageDFE;
	private int _minZ, _maxZ, _minX, _maxX, _minY, _maxY;
	private GraphNode _minZnode, _maxZnode, _minXnode, _maxXnode, _minYnode, _maxYnode;
	private int[] _rgb;
	// private GraphNode _currentNode;
	/** Actual distance from edge measurement converted to mm. */
	public float getAverageDFE()
	{
		return _averageDFE;
	}
	/** Greatest DFE for the line in DISTANCE_PRECISION units. */
	public static short getMaxDFE(List<GraphNode> centerline)
	{
		short max = 0;
		for (GraphNode node: centerline)
		{
			if (node.dfe > max)
				max = node.dfe;
		}
		return max;
	}
	/** Average DFE for the line in DISTANCE_PRECISION units. */
	public static float getAverageDFE(List<GraphNode> centerline)
	{
		int sum = 0;
		for (GraphNode node: centerline)
		{
			sum += node.dfe;
		}
		double s = (double)sum;
		double l = (double)centerline.size();
		// return (float)(s/l)/ VoxelDistance.DISTANCE_ACCURACY;
		return (float)(s/l);
	}
	/** Finds the radius normalized DFE difference between the ends of a window around the node of the attached centerline or returns 0
	 * if no centerline is attached. */
	public static double getWindowDFEchange(GraphNode node, int windowRadius)
	{
		
		if (node.centerline == null) return 0;
		List<GraphNode> nodes = node.centerline.getCenterlineNodes();
		
		double windowMean = 0;
		int index = nodes.indexOf(node);
		if (index < 0)
		{
			IJ.log(node.coordinateString()+" removed from line");
			return 0;
		}
		ListIterator<GraphNode> fitr = nodes.listIterator(index);
		ListIterator<GraphNode> bitr = nodes.listIterator(index);
		int windowDfeSum = 0;
		int windowLen = 0;
		List<Short> windowDFEs = new LinkedList<Short>();
		List<GraphNode> windowCentNodes = new LinkedList<GraphNode>();
		GraphNode forwardEnd = null;
		GraphNode reverseEnd = null;
		for (int i=0; i < windowRadius; i++)
		{
			if (fitr.hasNext())
			{
				GraphNode n = fitr.next();
				n.graphed = false;
				for (GraphNode a: n.adjacents)
				{
					a.graphed = false;
				}
				windowCentNodes.add(n);
				forwardEnd = n;
				if (reverseEnd == null)
					reverseEnd = n;
			}
			if (bitr.hasPrevious())
			{
				GraphNode n = bitr.previous();
				n.graphed = false;
				for (GraphNode a: n.adjacents)
				{
					a.graphed = false;
				}
				windowCentNodes.add(n);
				reverseEnd = n;
				if (forwardEnd == null)
					forwardEnd = n;
			}
		}
		for (GraphNode winCentNode: windowCentNodes)
		{
			short dfe = winCentNode.dfe; 
			windowDfeSum += dfe;
			windowLen++;
			windowDFEs.add(dfe);
			winCentNode.graphed = true;
			// include adjacent centerline voxels from separate centerlines  
			
			for (GraphNode a: winCentNode.adjacents)
			{
				if (!a.graphed && a.isCenterline)
				{
					dfe = a.dfe; 
					windowDfeSum += dfe;
					windowLen++;
					windowDFEs.add(dfe);
					a.graphed = true;
				}
			}
			
		}
		windowMean = windowDfeSum / (double)windowLen;
		
		int windowEndDfeDelta = 0;
		if (forwardEnd != null && reverseEnd != null)
			windowEndDfeDelta = Math.abs(forwardEnd.dfe - reverseEnd.dfe);
		
		return ((double)windowEndDfeDelta / windowMean); 
	}
	
	/** Calculate descriptive parameters of the Centerline: minimum and maximum x, y & z, 
	 * average x, y, z and DFE. These are used for determining arteries: ICA left, right, ACA, basilar. */
	private void calculateParameters()
	{
		int sumDFE = 0; 
		int sumX = 0, sumY = 0, sumZ = 0;
		_minX = Integer.MAX_VALUE;
		_minY = Integer.MAX_VALUE;
		_minZ = Integer.MAX_VALUE;
		
		_maxX = 0;
		_maxY = 0;
		_maxZ = 0;
		
		for (GraphNode n: _centerlineNodes)
		{
			sumX += n.col;
			sumY += n.row;
			sumZ += n.z;
			sumDFE += n.dfe;
			if (n.col < _minX)
			{ 
				_minX = n.col;
				_minXnode = n;
			}
			if (n.col > _maxX)
			{
				_maxX = n.col;
				_maxXnode = n;
			}
			if (n.row < _minY)
			{ 
				_minY = n.row;
				_minYnode = n;
			}
			if (n.row > _maxY)
			{ 
				_maxY = n.row;
				_maxYnode = n;
			}
			if (n.z < _minZ) 
			{
				_minZ = n.z;
				_minZnode = n;
			}
			if (n.z > _maxZ)
			{   
				_maxZ = n.z;
				_maxZnode = n;
			}
		}
		float nodes = (float)_centerlineNodes.size();
		_averageDFE = ((float)sumDFE) / VoxelDistance.DISTANCE_PRECISION / nodes ;
		_averageX = ((float)sumX) / nodes;
		_averageY = ((float)sumY) / nodes;
		_averageZ = ((float)sumZ) / nodes;
	}
	public void addNode(GraphNode node)
    {
        _centerlineNodes.add(node);
        node.centerline = this;
        node.isCenterline = true;
        calculateParameters();
    }
   
    public List<GraphNode> getCenterlineNodes()
    {
        return _centerlineNodes;
    }
    /** Replaces any existing nodes with these. */
    public void setCenterlineNodes(LinkedList<GraphNode> nodes)
    {
        _centerlineNodes = nodes;
        for (GraphNode node: nodes)
    	{
    		node.centerline = this;
    		node.isCenterline = true;
    	}
        calculateParameters();
    }
    /** Centerline source node is the last node, it is a potential bifurcation point. */
    public GraphNode getBifurcationNode()
    {
        return _bifurcationNode;
    }
    /** Source node of the centerline is the the last node added. It is a potential bifurcation point  */
    public void setBifurcationNode(GraphNode node)
    {
        _bifurcationNode = node;
        node.isBifurcation = true;
    }
	/** Add a graph node surrounding the centerline. Graph Nodes represent voxels of artery. */
	public void addSurroundNode(GraphNode node)
	{
		_surroundNodes.add(node);
		node.centerline = this;
	}
	public void addSurroundNodes(List<GraphNode> nodes)
	{
		for (GraphNode n: nodes)
		{
			_surroundNodes.add(n);
			n.centerline = this;
		}
	}
	public List<GraphNode> getSurroundNodes()
	{
		return _surroundNodes;
	}
	
	
	/** Display Colorized centerlines */
	public static ImagePlus makeColorImage(List<CenterlineGraph> centerlines, int width, int height, int zSize, 
			String name)
	{
		return makeColorImage(centerlines, width, height, zSize, name, false);
	}
	
	/** Create a 3-D stack with colorized centerlines 
	 * @param dispaySurround display surrounding artery voxels Each centerline and its surrounding voxels 
	 * are the same color but with the centerline more intense. */
	public static ImagePlus makeColorImage(List<CenterlineGraph> centerlineGraphs, int width, int height, int zSize, 
			String name, boolean displaySurround)
    {
    	ImageStack colorStack = new ImageStack(width, height);
    	ImageProcessor[] colorProcs = new ImageProcessor[zSize];
    	for (int i=1; i <=zSize; i++)
    	{
    		ImageProcessor colorProc = new ColorProcessor(width, height);
    		colorStack.addSlice(""+i, colorProc);
    		colorProcs[i-1] = colorProc;
    	}
    	
        Colors colors = Colors.getColors();
        Random random = new Random();
        int[] surroundColor = new int[3]; 
        int highSurround = 80;
        
    	for (CenterlineGraph graph: centerlineGraphs)
    	{
	    	for (Centerline centerline: graph.getCenterlines())
	        {
	    		if (displaySurround)
	    		{
	    			surroundColor[0] = random.nextInt(highSurround);
	    			surroundColor[1] = random.nextInt(highSurround);
	    			surroundColor[2] = random.nextInt(highSurround);
		    		List<GraphNode> surround = centerline.getSurroundNodes();
		    		
		    		for (GraphNode sn: surround)
		    		{
		    			colorProcs[sn.z].putPixel(sn.col, sn.row, surroundColor );
		    		}
	    		}
	            List<GraphNode> centerNodes = centerline.getCenterlineNodes();
	            // centerline color stronger 
	            
	            for (GraphNode cn: centerNodes)
	            {
	                colorProcs[cn.z].putPixel(cn.col, cn.row, colors.centerline);
	            }
	            GraphNode bifNode  = centerline.getBifurcationNode();
	            if (bifNode != null)
	            {
	            	colorProcs[bifNode.z].putPixel(bifNode.col, bifNode.row, colors.bifurcation);
	            }
	        }
    	}
    	
    	ImagePlus colorImage = new ImagePlus();
    	colorImage.setStack(name, colorStack);
    	
    	return colorImage;
    }
	/** Display Distance Factor Metric tortuosity measure from centerline graphs. 
	 * @param thicken make the DFM line 3 voxels wide to make it easier to see scores. */
    public static ImagePlus makeDfmImage(List<Centerline> centerlines, int width, int height, 
    		int zSize, boolean thicken, String name)
    {
        float[][] voxels = new float[zSize][width*height];
        for (Centerline centerline: centerlines)
        {
            List<GraphNode> nodes = centerline.getCenterlineNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.dfm;
                if (thicken)
                {
	                // fill 26 neighbors
	                Position pos = new Position(n.col, n.row, n.z);
	                Queue<Position> neighbors = pos.getNeighbors();
	                for (Position neighbor: neighbors)
	                {
	                	int nrow = neighbor.getRow();
	                	int ncol = neighbor.getColumn();
	                	int nz = neighbor.getZ();
	                	// keep neighbor in image bounds 
	                	if (nrow < 0) nrow = 0;
	                	if (ncol < 0) ncol = 0;
	                	if (nz < 0) nz = 0;
	                	if (nrow >= height) nrow = height - 1;
	                	if (ncol >= width) ncol = width - 1;
	                	if (nz >= zSize) nz = zSize - 1;
	                	if (voxels[nz][nrow*width+ncol] == 0.0f)
	                	{
	                		voxels[nz][nrow*width+ncol] = n.dfm;
	                	}
	                }
                }
            }
        }
        return ImageProcess.makeImage(voxels, width, height, name);
    }
    /* Sort graph greatest/deepest Z to least/shallowest Z. */
    public static class ZDeeperComparator implements Comparator<Centerline> 
    {
        public int compare (Centerline a, Centerline b)
        {
            if (a.getMaxZ() < b.getMaxZ())
                return 1;
            if (a.getMaxZ() == b.getMaxZ())
                return 0;
            return -1;
        }
    }
    /* Sort graph most nodes to least. */
    public static class MoreNodesComparator implements Comparator<Centerline>
    {
        public int compare (Centerline a, Centerline b)
        {
            if (a.nodeCount() < b.nodeCount())
                return 1;
            if (a.nodeCount() == b.nodeCount())
                return 0;
            return -1;
        }
    }
    /* Sort graphs lowest to highest carotid score. */
    public static class LowestCarotidScoreComparator implements Comparator<Centerline>
    {
        public int compare (Centerline a, Centerline b)
        {
            if (a.getCarotidScore() < b.getCarotidScore())
                return -1;
            if (a.getCarotidScore() == b.getCarotidScore())
                return 0;
            return 1;
        }
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
    /** @return number of nodes in graph */
    public int nodeCount()
    {
        return _centerlineNodes.size();
    }

	public CenterlineGraph getCenterlineGraph() {
		return _centerlineGraph;
	}

	public void setCenterlineGraph(CenterlineGraph centerlineGraph) {
		_centerlineGraph = centerlineGraph;
	}

	public float getAverageX() {
		return _averageX;
	}

	public float getAverageY() {
		return _averageY;
	}

	public float getAverageZ() {
		return _averageZ;
	}

	public int getMinZ() {
		return _minZ;
	}

	public int getMaxZ() {
		return _maxZ;
	}

	public int getMinX() {
		return _minX;
	}

	public int getMaxX() {
		return _maxX;
	}

	public int getMinY() {
		return _minY;
	}

	public int getMaxY() {
		return _maxY;
	}
	public GraphNode getMinZnode() {
		return _minZnode;
	}
	public GraphNode getMaxZnode() {
		return _maxZnode;
	}
	public GraphNode getMinXnode() {
		return _minXnode;
	}
	public GraphNode getMaxXnode() {
		return _maxXnode;
	}
	public GraphNode getMinYnode() {
		return _minYnode;
	}
	public GraphNode getMaxYnode() {
		return _maxYnode;
	}
	public Graph getGraph() {
		return _graph;
	}
	public void setGraph(Graph graph) {
		_graph = graph;
	}
	public int[] getRgb() {
		return _rgb;
	}
	public void setRgb(int[] rgb) {
		_rgb = rgb;
	}
	/** Breadth First search of centerline nodes from s to end
	 * @return Path between the nodes. */
	public static List<GraphNode> pathBFS(GraphNode s, GraphNode end)
    {
    	List<GraphNode> path = new LinkedList<GraphNode>();
		
		if (s.isCenterline == false)
			return path;
		
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
		while (!q.isEmpty())
		{
			GraphNode u = q.poll();
			if (u == end)
			{
				break;
			}
			else
			{
				path.add(u);
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
		return Graph.backtracePred(s, end);
    }
	
}

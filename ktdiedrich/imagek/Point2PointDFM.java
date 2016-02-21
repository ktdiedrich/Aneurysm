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
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ktdiedrich.db.aneurysm.Inserts;

/** Point to point Distance Factor metric tortuosity measurement.
 * @author ktdiedrich@gmail.com
 *  */
public class Point2PointDFM implements Message
{
    private ImagePlus _image;
	private List<CenterlineGraph> _centerlineGraphs;
	private Message _messageWindow;
	private float _xRes;
	private float _yRes;
	private float _zRes;
	private GraphNode _nodeA;
	private GraphNode _nodeB;
	private List<GraphNode> _markedNodes;
	private List<int[]> _prevColors;
	
	private Colors _colors;
	private ImageProcessor[] _imageProcessors;
	private CenterlineGraph _graphA;
	private List<DFM> _forwardDfms; 
	private RecordDFMTortuosityPanel _recordPanel;
	private IdentifyArteries _identifyArteries;
	private int _zSize;
	private int _tortAlg;
	public Point2PointDFM(ImagePlus image, List<CenterlineGraph> centerlineGraphs, 
			float xRes, float yRes, float zRes)
	{
	    
	    _image = image;
	    
	    ImageStack stack = _image.getStack();
	    _zSize = stack.getSize();
	    
	    _imageProcessors = new ImageProcessor[_zSize];
	    for (int i = 0; i < _zSize; i++)
	    {
	    	_imageProcessors[i] = stack.getProcessor(i+1);
	    }
		_centerlineGraphs = centerlineGraphs;
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
		_markedNodes = new LinkedList<GraphNode>();
		_prevColors = new ArrayList<int[]>();
		_colors = Colors.getColors();
		_identifyArteries = new IdentifyArteries(centerlineGraphs, image.getWidth(), 
				image.getHeight(), _zSize);
	}
	public void setPoint(Position position)
	{
		int col = position.getColumn();
		int row = position.getRow();
		int zDepth = position.getZ();
		
		message("Searching for centerline point at: "+col+", "+row+", "+zDepth);
		
		boolean isCenterline =  false;
		
		if (_nodeA == null || (_nodeA != null && _nodeB != null))
		{
			isCenterline = searchA(col, row, zDepth);
		}
		else if (_nodeB == null)
		{
			isCenterline = searchB(col, row, zDepth);
		}
		if (isCenterline == false)
    	{
    		message("Centerline not found. Pick a bifurcation or centerline voxel.");
    	}
	}
	/** Find the first end point. Distance will measure from this point. */
	public boolean searchA(int x, int y, int z)
	{
		int[] iArray = new int[3];
		boolean isCenterline = false;
		
		for (CenterlineGraph centGraph: _centerlineGraphs)
		{
	    	for (Centerline centerline: centGraph.getCenterlines())
	    	{
	    		List<GraphNode> nodes = centerline.getCenterlineNodes();
	    		Iterator<GraphNode> itr = nodes.iterator();
	    		while (itr.hasNext())
	    		{
	    			GraphNode node = itr.next();
	    			if (node.col == x && node.row == y && node.z == z)
	    			{
	    				isCenterline = true;
	    				_nodeA = node;
	    				_nodeB = null;
	    				// only display centerlines in the
	    				// same CenterlineGraph as A 
	    				_graphA = centGraph;
	    				IJ.log("Set A ("+node.col+", "+node.row+", "+node.z+")");
	    				recolorCenterlines(_centerlineGraphs, true);
	    				
	    				ImageProcessor proc = _imageProcessors[node.z]; 
	    				_markedNodes.add(node);
	    				_prevColors.add(proc.getPixel(node.col, node.row, iArray));
	    				//message("mark ("+node.col+", "+node.row+", "+node.z+") color ("+_colors.mark[0]+", "+_colors.mark[1]+", "+_colors.mark[2]+")");
	    				proc.putPixel(_nodeA.col, _nodeA.row, _colors.mark);
	    				
	    				highlightGraph(_centerlineGraphs, _graphA);
	    				// only display centerlines in the
	    				// same CenterlineGraph as A 
	    			}
	    		}
	    		if (isCenterline) break;
	    	}
	    	if (isCenterline) break;
		}
		if (!isCenterline)
		{
			for (CenterlineGraph centGraph: _centerlineGraphs)
			{
		    	for (Centerline centerline: centGraph.getCenterlines())
		    	{
		    		List<GraphNode> nodes = centerline.getCenterlineNodes();
		    		Iterator<GraphNode> itr = nodes.iterator();
		    		while (itr.hasNext())
		    		{
		    			GraphNode node = itr.next();
		    			if (node.col > x-2 && node.col < x+2 && 
		    			        node.row > y-2 && node.row < y+2 && 
		    			        node.z == z)
		    			{
		    				isCenterline = true;
		    				_nodeA = node;
		    				_nodeB = null;
		    				// only display centerlines in the
		    				// same CenterlineGraph as A 
		    				_graphA = centGraph;
		    				IJ.log("Set A ("+node.col+", "+node.row+", "+node.z+")");
		    				recolorCenterlines(_centerlineGraphs, true);
		    				ImageProcessor proc = _imageProcessors[node.z]; 
		    				_markedNodes.add(node);
		    				_prevColors.add(proc.getPixel(node.col, node.row, iArray));
		    				//message("mark ("+node.col+", "+node.row+", "+node.z+") color ("+_colors.mark[0]+", "+_colors.mark[1]+", "+_colors.mark[2]+")");
		    				proc.putPixel(_nodeA.col, _nodeA.row, _colors.mark);
		    				highlightGraph(_centerlineGraphs, _graphA);
		    				// only display centerlines in the
		    				// same CenterlineGraph as A 
		    				break;
		    			}
		    		}
		    		if (isCenterline == true)
	                {
	                    break;
	                }
		    	}
		    	if (isCenterline == true)
	            {
	                break;
	            }
			}
		}
		return isCenterline;
	}
	/** Find the second end point. Distances are measured from end point A out to end point B. */
	public boolean searchB(int x, int y, int z)
	{
		int[] iArray = new int[3];
		boolean isCenterline = false;
		
		for (Centerline centerline : _graphA.getCenterlines())
		{
			List<GraphNode> nodes = centerline.getCenterlineNodes();
			Iterator<GraphNode> itr = nodes.iterator();
			while(itr.hasNext())
			{
				GraphNode node = itr.next();
				if (node.col == x && node.row == y && node.z == z)
				{
					isCenterline = true;
					_nodeB = node;
					IJ.log("Set B ("+node.col+", "+node.row+", "+node.z+")");
					
					recolorCenterlines(_centerlineGraphs, false);
					ImageProcessor proc = _imageProcessors[node.z]; 
					_markedNodes.add(node);
					_prevColors.add(proc.getPixel(node.col, node.row, iArray));
					proc.putPixel(_nodeB.col, _nodeB.row, _colors.mark);
					_imageProcessors[_nodeA.z].putPixel(_nodeA.col, _nodeA.row, _colors.mark);
					_image.updateAndDraw();
					// List<GraphNode> aNodes = _graphA.getCenterlineNodes();
					this.clear();
					message("Forward");
					
					_forwardDfms = path(_nodeB.centerline.getGraph().getNodes(), _nodeB, _nodeA);
					break;
				}
			}
			if (isCenterline) break;
		}
		if (!isCenterline)
		{
			for (Centerline centerline : _graphA.getCenterlines())
			{
				List<GraphNode> nodes = centerline.getCenterlineNodes();
				Iterator<GraphNode> itr = nodes.iterator();
				while(itr.hasNext())
				{
					GraphNode node = itr.next();
					if (node.col > x-2 && node.col < x+2 && 
    			        node.row > y-2 && node.row < y+2 && 
    			        node.z == z)
					{
						isCenterline = true;
						_nodeB = node;
						IJ.log("Set B ("+node.col+", "+node.row+", "+node.z+")");
						
						recolorCenterlines(_centerlineGraphs, false);
						ImageProcessor proc = _imageProcessors[node.z]; 
						_markedNodes.add(node);
						_prevColors.add(proc.getPixel(node.col, node.row, iArray));
						proc.putPixel(_nodeB.col, _nodeB.row, _colors.mark);
						_imageProcessors[_nodeA.z].putPixel(_nodeA.col, _nodeA.row, _colors.mark);
						_image.updateAndDraw();
						this.clear();
						message("Forward");
						_forwardDfms = path(_nodeB.centerline.getGraph().getNodes(), _nodeB, _nodeA);
						break;
					}
				}
				if (isCenterline) break;
			}
		}
		return isCenterline;
	}
	
	/** After node A is selected node B must be from the same graph. Gray out other graphs  */
	public void highlightGraph(List<CenterlineGraph> centerlineGraphs, CenterlineGraph graphA )
	{
		for (CenterlineGraph graph: centerlineGraphs)
		{
			if (graph != graphA)
			{
				for (Centerline centerline: graph.getCenterlines())
				{
					for (GraphNode node: centerline.getCenterlineNodes())
					{
						_imageProcessors[node.z].putPixel(node.col, node.row, _colors.off);
					}
				}
			}
		}
		_image.updateAndDraw();
		
	}
	public void recolorCenterlines(List<CenterlineGraph> centerlineGraphs, boolean recolorSurround)
	{
		message("Recolor all centerline graphs");
		
		Overlay.drawCenterlines(centerlineGraphs, _imageProcessors, recolorSurround);
		_markedNodes.clear();
		_prevColors.clear();
		
		Set<GraphNode> leftICAMCA = _identifyArteries.getLeftICAMCABifurcation();
		Set<GraphNode> rightICAMCA = _identifyArteries.getRightICAMCABifurcation();
		Colors c = Colors.getColors();
		for (GraphNode l: leftICAMCA)
		{
			markNode(l, c.source);
		}
		for (GraphNode r: rightICAMCA)
		{
			markNode(r, c.cyan);
		}
		GraphNode lowerLeftIca = _identifyArteries.getLeftICAend();
		GraphNode lowerRightIca = _identifyArteries.getRightICAend();
		markNode(lowerLeftIca, c.source);
		markNode(lowerRightIca, c.cyan);
		
		_image.updateAndDraw();
	}
	public void markNode(GraphNode node, int[] c)
	{
		if (node != null)
		{
			_imageProcessors[node.z].putPixel(node.col, node.row, c);
			_image.updateAndDraw();
		}
	}
	public void markCenterline(Centerline line)
	{
	    //TODO clip centerline to measurable portion and set _nodeA and _nodeB & measure DFM
		message("mark centerline");
	    this.recolorCenterlines(_centerlineGraphs, false);
	    List<GraphNode> lineNodes = line.getCenterlineNodes();
	    Colors colors = Colors.getColors();
	    int[] iArray = new int[3];
        for (GraphNode ln: lineNodes)
        {
        	// TODO change to mark color
            _imageProcessors[ln.z].putPixel(ln.col, ln.row, colors.magenta);
            _markedNodes.add(ln);
            ImageProcessor proc = _imageProcessors[ln.z];
            _prevColors.add(proc.getPixel(ln.col, ln.row, iArray));
        }
        GraphNode bifurcation = line.getBifurcationNode();
        if (bifurcation != null)
            _imageProcessors[bifurcation.z].putPixel(bifurcation.col, bifurcation.row, colors.bifurcation);
        _image.updateAndDraw();
	}
	
	/** measure DFMs both ways
	 * Plots the path from the end node back to the source node. */
	public List<DFM> path(List<GraphNode> aNodes, GraphNode sourceNode, GraphNode endNode)
	{
		ShortestPaths sp = new ShortestPaths();
		GraphNode.algorithmReset(aNodes);
		LinkedList<GraphNode> llnodes = (LinkedList<GraphNode>)aNodes;
		Graph shortestPaths = sp.dijkstraLowestCostPathTarget(llnodes, sourceNode, _zSize, endNode);
		// TODO remove display of shortest paths after debugging 
		/* 
		List<Graph> sps = new LinkedList<Graph>();
		sps.add(shortestPaths);
		
		ImagePlus pathCostIm = Graph.pathCostImage(sps, _image.getWidth(), _image.getHeight(), 
				_zSize, "ShortestPaths"+sourceNode.coordinateString());
		pathCostIm.show();
		pathCostIm.updateAndDraw();
		*/ 
		List<DFM> dfms = new LinkedList<DFM>();
		GraphNode g = endNode;
		int[] iArray = new int[3];
		LineMeasure lineMeasure = new LineMeasure(_xRes, _yRes, _zRes);
		float L = 0.0f;
		float L3 = 0.0f;
		IJ.log("Tortuosity algorithm_id: "+_tortAlg);
		if (_tortAlg == Inserts.TORT_DFM_ALG)
		{
			message("L3      L      d        DFM         DFE  x  y  z");
		}
		
		IJ.log("EndNode: "+endNode.coordinateString()+ " sourceNode: "+sourceNode.coordinateString());
		
		DFM metric = new DFM(0, 0, 0, 0, endNode.col, endNode.row, endNode.z);
		if (_tortAlg  == Inserts.TORT_DFM_ALG)
    	{
    		dfms.add(metric);
    		message(metric.toString());
    	}
		float maxDFM = 0;
		while (g != null && g != sourceNode)
		{
			ImageProcessor pr = _imageProcessors[g.z]; 
		    _markedNodes.add(g);
		    _prevColors.add(pr.getPixel(g.col, g.row, iArray));
		    pr.putPixel(g.col, g.row, _colors.mark);
		    
		    if (g.predecessor != null)
		    {
		    	float d = lineMeasure.measureStraight(endNode, g.predecessor);
	    		L += lineMeasure.measureAdjacent(g, g.predecessor);
	    		L3 += lineMeasure.measure3AveAdj(g, g.predecessor);
	    		float dfe = ((float)g.dfe) / VoxelDistance.DISTANCE_PRECISION;
	    		float dfm = L/d;
	    		if (dfm > maxDFM) maxDFM = dfm;
	    		metric = new DFM(L, d, dfm, dfe, g.predecessor.col, g.predecessor.row, 
	    				g.predecessor.z);
	    		
	    		
		    	if (_tortAlg  == Inserts.TORT_DFM_ALG)
		    	{
		    		dfms.add(metric);
		    		message(L3+" "+metric.toString());
		    	}
		    }
		    g = g.predecessor;
		}
		message("maxDFM="+maxDFM);
		
		_image.updateAndDraw();
		return dfms;
	}
	
	public void message(String m)
	{
		if (_messageWindow != null)
			_messageWindow.message(m);
		else
			System.out.println(m);
	}
	public void clear()
	{
		if (_messageWindow != null)
			_messageWindow.clear();
	}
	public void setMessageWindow(Message messageWindow) 
	{
		_messageWindow = messageWindow;
	}
	public List<DFM> getForwardDfms() 
	{
		return _forwardDfms;
	}
	public void setRecordPanel(RecordDFMTortuosityPanel recordPanel) 
	{
		_recordPanel = recordPanel;
	}
	public RecordDFMTortuosityPanel getRecordPanel() 
	{
		return _recordPanel;
	}
	public GraphNode getNodeA() 
	{
		return _nodeA;
	}
	public GraphNode getNodeB() 
	{
		return _nodeB;
	}
    public IdentifyArteries getIdentifyArteries()
    {
        return _identifyArteries;
    }
	public int getTortAlg() {
		return _tortAlg;
	}
	public void setTortAlg(int tortAlg) {
		_tortAlg = tortAlg;
	}
}

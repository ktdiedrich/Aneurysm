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

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

/** Calculates the distance from the centerline of each voxel in the image graph.
 * @author ktdiedrich@gmail.com 
 * */
public class DistanceFromCenterline
{
    public static float THRESHOLD  = 0.5f;
    public static double ENHANCEMENT_PRECISION = 100.0;
	private short _col, _row, _z, _colRow, _colZ, _rowZ, _colRowZ;
    private int _windowSize;
    private int _enhancementHistogramTotal = 0;
    private SortedMap<Short, Integer> _enhancementHistogram;
    private double _intensityThreshold;
    private double _dfcDfeRatioThreshold;
    public DistanceFromCenterline(float xRes, float yRes, float zRes)
    {
    	_intensityThreshold = THRESHOLD;
    	// calculate direction costs and round to short same as DistanceFromEdge
    	VoxelDistance voxDis = new VoxelDistance(xRes, yRes, zRes);
        _col = voxDis.getCol();
        _row = voxDis.getRow();
        _z = voxDis.getZ();
        
        _colRow = voxDis.getColRow();
        _colZ = voxDis.getColZ();
        _rowZ = voxDis.getRowZ();
        
        _colRowZ = voxDis.getColRowZ();
    }
    
    /** Set the dfc values of the GraphNodes in the graph. */
    public void setNodesDFC(List<CenterlineGraph> centerlineGraphs, List<Graph> sourceImageGraphs, int zSize)
    {
    	int countC = 0;
        int countNonC = 0;
        for (Graph g: sourceImageGraphs)
        {
        	for (GraphNode n: g.getNodes())
        	{
        		n.dfc = 0;
        		n.graphed = false;
        	}
        }
        //IJ.log("Setting CenterlineData");
        for (CenterlineGraph g: centerlineGraphs)
        {
        	List<GraphNode> cnodes = g.getCenterlineNodes();
        	for (GraphNode n: cnodes)
        	{
        		n.centerlineNode = n;
        		//if (n.centerlineNodeData==null)
        		//{
        		//	n.centerlineNodeData = new CenterlineNodeData();
        		//}
        	}
        }
        for (CenterlineGraph g: centerlineGraphs)
        {
            GraphNode source = g.getSourceNode();
            
            // find nearest centerline GraphNode using direction 
            // dependent distances 
            Queue<GraphNode> queue = new LinkedList<GraphNode>();
            
            queue.clear();
            queue.add(source);
            
            if (source.centerline != null) // TODO source not set by loose end closing  
            	countC++;
            else
            	countNonC++;
            
            source.graphed = true;
            // assign centerlineNode 
            while (queue.isEmpty() == false)
            {
                GraphNode node = queue.remove();
                
                for (GraphNode adjNode: node.adjacents)
                {
                	short newDFC = (short)(node.dfc + 1);
                	// calculate Euclidean distance as in DFE
                	// move col 
                	if (node.col != adjNode.col && node.row == adjNode.row && node.z == adjNode.z )
                	{
                		newDFC = (short)(node.dfc + _col);
                	}
                	// row move
                	if (node.col == adjNode.col && node.row != adjNode.row && node.z == adjNode.z)
                	{
                		newDFC = (short)(node.dfc + _row);
                	}
                	// z move 
                	if (node.col == adjNode.col && node.row == adjNode.row && node.z != adjNode.z)
                	{
                		newDFC = (short)(node.dfc + _z);
                	}
                	// col row move
                	if (node.col != adjNode.col && node.row != adjNode.row && node.z == adjNode.z)
                	{
                		newDFC = (short)(node.dfc + _colRow);
                	}
                    // col Z move
                	if (node.col != adjNode.col && node.row == adjNode.row && node.z != adjNode.z)
                	{
                		newDFC = (short)(node.dfc + _colZ);
                	}
                	// row Z move
                	if (node.col == adjNode.col && node.row != adjNode.row && node.z != adjNode.z)
                	{
                		newDFC = (short)(node.dfc + _rowZ);
                	}
                	// col row Z move 
                	if (node.col != adjNode.col && node.row != adjNode.row && node.z != adjNode.z)
                	{
                		newDFC = (short)(node.dfc + _colRowZ);
                	}
                	
                    if (adjNode.isCenterline == false)
                    {
                        if (adjNode.dfc == 0 || newDFC < adjNode.dfc)
                        {
                           adjNode.dfc = newDFC;
                           if (node.centerlineNode != null)
                           {
                        	   adjNode.centerlineNode = node.centerlineNode;
                        	   //adjNode.centerlineNode.centerlineNodeData.addBelongNode(adjNode);
                           }
                        }
                    }
                    
                    // don't add nodes to queue more than once
                    if (adjNode.graphed == false)
                    {
                    	if (adjNode.isCenterline) countC++;
                    	else countNonC++;
                        queue.add(adjNode);
                        adjNode.graphed = true;
                    }
                }
            }
        }
        
    }
    
    public void enhanceAneurysm(List<CenterlineGraph> centerlineGraphs, int zSize)
    {
    	_enhancementHistogram = new TreeMap<Short, Integer>();
        
        for (CenterlineGraph g: centerlineGraphs)
        {
        	for (GraphNode centNode: g.getCenterlineNodes())
        	{        		
        		// round window DFE change to a short 
        		short windowDFEvar = (short)Math.round(Centerline.getWindowDFEchange(centNode, _windowSize)*ENHANCEMENT_PRECISION);
        		// histogram and threshold of window DFE difference
        		if (windowDFEvar > 0)
        		{
        			if (_enhancementHistogram.containsKey(windowDFEvar))
        			{
        				Integer count = _enhancementHistogram.get(windowDFEvar);
        				count++;
        				_enhancementHistogram.put(windowDFEvar, count);
        			}
        			else
        			{
        				_enhancementHistogram.put(windowDFEvar, 1);
        			}
        			_enhancementHistogramTotal++;
        		}
        		centNode.enhance = windowDFEvar;
        		// project the enhancement on belonging nodes, check DFC/DFE ratio here
        		short maxDFC = centNode.centerlineNodeData.getMaxDfc();
        		double dfcDfeRatio = ((double)maxDFC)/((double)centNode.dfe);
        		// IJ.log("Max DFC: "+maxDFC+" centerline DFE: "+centNode.dfe+" ratio: "+dfcDfeRatio);
        		if (_dfcDfeRatioThreshold == 0 || dfcDfeRatio < _dfcDfeRatioThreshold)
        		{
	        		for (GraphNode n: centNode.centerlineNodeData.getBelongingNodes())
	        		{
	        			// various enhancement
	        			// don't color the bottom edge of the image it's usually noise. 
	        			if (n.z < zSize-1)
	        			{
	        				n.enhance = windowDFEvar;
	        			}
	        		}
        		}
        	}
        	
        }
        int enhanceCutoffCount = (int)Math.round((double)_enhancementHistogramTotal * _intensityThreshold);
        int enhanceSum = 0;
        short enhanceValCutoff = Short.MAX_VALUE;
        boolean setEnhanceCut = false;
        for (short enhanceVal: _enhancementHistogram.keySet())
        {
        	
        	int enCount = _enhancementHistogram.get(enhanceVal);
        	enhanceSum += enCount;
        	// IJ.log("enhanceVal="+enhanceVal+" enCount="+enCount+" enhanceSum="+enhanceSum+" faction="+((double)enhanceSum/(double)_enhancementHistogramTotal) );
        	if (! setEnhanceCut && enhanceSum > enhanceCutoffCount)
        	{
        		enhanceValCutoff = enhanceVal;
        		setEnhanceCut = true;
        		break;
        	}
        	
        }
        //IJ.log("Enhancement cutoff value: "+enhanceValCutoff);
        // threshold aneurysm enhancement 
        for (CenterlineGraph g: centerlineGraphs)
        {
        	for (GraphNode centNode: g.getCenterlineNodes())
        	{
        		if (centNode.enhance < enhanceValCutoff)
        		{
        			centNode.enhance = 0;
        			for (GraphNode n: centNode.centerlineNodeData.getBelongingNodes())
            		{
        				n.enhance = 0;
            		}
        		}
        	}
        }
        // TODO record remaining enhancement intensities in a database histogram 
    }
    
	public int getWindowSize() {
		return _windowSize;
	}
	public void setWindowSize(int windowSize) {
		_windowSize = windowSize;
	}
	
	/** Make a short valued image of image graph node enhance values. */
    public static ImagePlus makeEnhancedImage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        short[][] voxels = new short[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.enhance;
            }
        }
        return ImageProcess.makeImage(voxels, width, height, name);
    }
    /** Make a short valued image of image graph node DFC values. */
    public static ImagePlus makeDFCImage(List<Graph> graphs, int width, int height, int zSize, String name)
    {
        short[][] voxels = new short[zSize][width*height];
        for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.dfc;
            }
        }
        return ImageProcess.makeImage(voxels, width, height, name);
    }
    
	public double getIntensityThreshold() {
		return _intensityThreshold;
	}
	public void setIntensityThreshold(double threshold) {
		_intensityThreshold = threshold;
	}
	/** Extend the aneurysm enhancement color out along the centerline for added visibility.
	 * @param centerlineGraphs Graphs of the centerline node.
	 * @param enhanceImage The image of the enhancement without drawn centerline or gray filling.
	 * @param windowRadius The distance in both directions along the centerline to extend the enhancement color. */
	public static void extendImageEnhance(List<CenterlineGraph> centerlineGraphs, ImagePlus enhanceImage, int windowRadius)
	{
		for (CenterlineGraph cg: centerlineGraphs)
		{
			for (Centerline cent: cg.getCenterlines())
			{
				for (GraphNode gn: cent.getCenterlineNodes())
				{
					extendNodeEnhance(gn, enhanceImage, windowRadius);
				}
			}
		}
	}
	/** Extends the enhanced score of a centerline graph node along the centerline where no color is present. 
     * Colors voxels belong to the centerline nodes. Stops when it reaches a color. Won't overwrite existing colors.
     * @param enhanceImage has the current enhancement values after thresholding 
     * @param windowRadius The number of centerline voxels to extend along.
     * @param intensityFactor reduce the intensity by multiplying by this factor.
     * */
    public static void extendNodeEnhance(GraphNode node, ImagePlus enhanceImage, int windowRadius)
	{
		// TODO extend enhancement in all directions not just along centerline 
		if (node.centerline == null) return ;
		ImageStack stack  =  enhanceImage.getImageStack();
		int width = stack.getWidth();
		short[][] curEnhanceVoxels = ImageProcess.getShortStackVoxels(stack);
		short nodeEnhance  = curEnhanceVoxels[node.z][node.row*width+node.col];
		
		short extendVal = nodeEnhance;
		List<GraphNode> nodes = node.centerline.getCenterlineNodes();
		if (nodes.size() >=2)
		{
			int index = nodes.indexOf(node);
			if (index > -1)
			{
				ListIterator<GraphNode> fitr = nodes.listIterator(index+1);
				ListIterator<GraphNode> bitr = nodes.listIterator(index);
				
				boolean movingFoward = true;
				boolean movingBackward = true;
				for (int i=0; i < windowRadius; i++)
				{
					if (movingFoward && fitr.hasNext())
					{
						GraphNode n = fitr.next();
						short nEnhance = curEnhanceVoxels[n.z][n.row*width+n.col];
						if (nEnhance == 0)
						{
							for (GraphNode bn: n.centerlineNodeData.getBelongingNodes())
			        		{
			        			curEnhanceVoxels[bn.z][bn.row*width+bn.col] = extendVal;
			        		}
						}
						else
						{
							movingFoward = false;
						}
					}
					if (movingBackward && bitr.hasPrevious())
					{
						GraphNode n = bitr.previous();
						short nEnhance = curEnhanceVoxels[n.z][n.row*width+n.col];
						if (nEnhance == 0)
						{
							for (GraphNode bn: n.centerlineNodeData.getBelongingNodes())
			        		{
			        			curEnhanceVoxels[bn.z][bn.row*width+bn.col] = extendVal;
			        		}
						}
						else
						{
							movingBackward = false;
						}
					}
				}
			}
			else
			{
				IJ.log(node.coordinateString()+" removed from line.");
			}
		}
	}

	public double getDfcDfeRatioThreshold() {
		return _dfcDfeRatioThreshold;
	}

	/** 0 is no thresholding */
	public void setDfcDfeRatioThreshold(double dfcDfeRatioThreshold) {
		_dfcDfeRatioThreshold = dfcDfeRatioThreshold;
	}
}


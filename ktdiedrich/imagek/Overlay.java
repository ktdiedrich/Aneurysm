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

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/** Overlays points on an image. Identifies artery centerlines. 
 * @author Karl T. Diedrich <ktdiedrich@gmail.com>
 * */
public class Overlay 
{
    public Overlay()
    {
        
    }
	/** writes the short values in the graph over the image. 
	 * */
    public void overlay(ImagePlus image, List<Graph> graphs, short value)
    {
        overlay(image.getStack(), graphs, value);
    }
    /** Overlay graph image on a short valued image. */
	public void overlay(ImageStack stack, List<Graph> graphs, short value)
	{
    	int width = stack.getWidth();
    	int zSize = stack.getSize();
		short[][] voxels = ImageProcess.getShortStackVoxels(stack);
    	for (Graph graph: graphs)
    	{
    		for (GraphNode node: graph.getNodes())
    		{
    			voxels[node.z][width*node.row + node.col] = value;
    		}
    	}	
	}
	/** Overlay a color graph on a image with stack. 
     * @return a color image with an ImageStack inside */
    public ImagePlus overlay(ImagePlus image, List<Graph> graphs, Color color, String name)
    {
    	ImageStack stack = image.getStack();
        return overlay(stack, graphs, color, name);
    }
    public ImagePlus overlay(ImageStack stack, List<Graph> graphs, Color color, String name)
    {
    	int height = stack.getHeight();
    	int width = stack.getWidth();
    	int zSize = stack.getSize();
    	ImageStack colorStack = new ImageStack(width, height);
    	ImageProcessor[] colorProcs = new ImageProcessor[zSize];
    	for (int i=1; i <=zSize; i++)
    	{
    		ImageProcessor colorProc = (stack.getProcessor(i)).convertToRGB();
    		colorStack.addSlice(""+i, colorProc);
    		colorProcs[i-1] = colorProc;
    	}
    	
    	for (Graph graph: graphs)
        {
            LinkedList<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                int row = n.row;
                int col = n.col;
                int z = n.z;
                int[] rgb = new int[3];
                rgb[0] = color.getRed();
                rgb[1] = color.getGreen();
                rgb[2] = color.getBlue();
                colorProcs[z].putPixel(col, row, rgb);
            }
        }
    	ImagePlus colorImage = new ImagePlus();
    	colorImage.setStack(name, colorStack);
    	return colorImage;
    }
    
    /** writes the value in the graph over the image. 
     * */
    public void overlayCenterlines(ImagePlus image, List<CenterlineGraph> graphs, short value)
    {
        overlayCenterlines(image.getStack(), graphs, value);
    }
    public void overlayCenterlines(ImageStack stack, List<CenterlineGraph> graphs, short value)
    {
        int width = stack.getWidth();
        int zSize = stack.getSize();
        short[][] voxels = ImageProcess.getShortStackVoxels(stack);
        for (CenterlineGraph graph : graphs)
        {
	        for (Centerline cent: graph.getCenterlines())
	        {
	            for (GraphNode node: cent.getCenterlineNodes())
	            {
	                voxels[node.z][width*node.row + node.col] = value;
	            }
	        }  
        }
    }
    public ImagePlus overlayCenterlinesOnWeights(List<CenterlineGraph> centerlineGraphs, 
    		List<Graph> imageGraphs, int width, int height, int zSize, String name)
    {
        float[][] voxels = new float[zSize][width*height];
        float maxWeight = 0.0f;
        for (Graph graph: imageGraphs)
        {
            List<GraphNode> nodes = graph.getNodes();
            for (GraphNode n: nodes)
            {
                voxels[n.z][n.row*width+n.col] = n.weight;
                if (n.weight > maxWeight) maxWeight = n.weight;
            }
        }
        maxWeight = maxWeight + (maxWeight*0.5f);
        for (CenterlineGraph cg: centerlineGraphs)
        {
        	List<GraphNode> nodes = cg.getCenterlineNodes();
        	for (GraphNode n: nodes)
        	{
        		voxels[n.z][n.row*width+n.col] = maxWeight;
        	}
        }
        return ImageProcess.makeImage(voxels, width, height, name);
    }
    public ImagePlus overlayColorCenterlines(ImagePlus image, List<CenterlineGraph> centerlineGraphs, String name)
    {
    	return overlayColorCenterlines(image.getStack(), centerlineGraphs, name);
    }
    /** Overlay a color centerline on a image with stack. 
     * red centerline and green bifurcation. Colors all voxels dim blue in the first step and then overwrites 
     * other colors. 
     * @return a new color image with an ImageStack inside */
    public ImagePlus overlayColorCenterlines(ImageStack stack, List<CenterlineGraph> centerlineGraphs, String name)
    {
        int height = stack.getHeight();
        int width = stack.getWidth();
        int zSize = stack.getSize();
        ImageStack colorStack = new ImageStack(width, height);
        ImageProcessor[] colorProcs = new ImageProcessor[zSize];
        // dim the artery voxels so the centerlines and other marks show 
        for (int i=0; i <zSize; i++)
        {
            ImageProcessor colorProc = (stack.getProcessor(i+1)).convertToRGB();
            colorStack.addSlice(""+i, colorProc);
            colorProcs[i] = colorProc;
            int[] pix = new int[3]; 
            int[] dimPix = new int[3]; 
    		for (int w=0; w < width; w++)
    		{
    			for (int h=0; h < height; h++)
    			{
    				pix = colorProc.getPixel(w, h, pix);
    				// color all pixels a dim blue 
    				dimPix[2] = pix[2]/4;
    				colorProc.putPixel(w, h, dimPix);
    			}
    		}
        }
        Overlay.drawCenterlines(centerlineGraphs, colorProcs, true);
        ImagePlus colorImage = new ImagePlus();
        colorImage.setStack(name, colorStack);
        return colorImage;
    }
    /** Draws the centerline and colors the surrounding segmentation voxels attached to the centerline a random color. 
     * Thresholded (by the DFE threshold) out edge voxels are left a dim blue color. */
    public static void drawCenterlines(List<CenterlineGraph> centerlineGraphs, ImageProcessor[] colorProcs, boolean surrond)
    {
    	Colors colors = Colors.getColors();
        int[] sColor = new int[3];
        Random random = new Random();
        int high = 83;
        
        for (CenterlineGraph centGraph: centerlineGraphs)
        {
	        for (Centerline line: centGraph.getCenterlines())
	        {
	        	// IJ.log("Line size"+line.getCenterlineNodes().size());
	        	int[] lineColor = line.getRgb();
	        	// color voxels surrounding an artery randomly 
	        	if (surrond)
	        	{
	        		sColor[0] = random.nextInt(high);
	        		sColor[1] = random.nextInt(high);
	        		sColor[2] = random.nextInt(high);
	        	
	        		List<GraphNode> surroundNodes = line.getSurroundNodes();
	        		for (GraphNode surNode: surroundNodes)
	        		{
	        			colorProcs[surNode.z].putPixel(surNode.col, surNode.row, sColor);
	        		}
	        	}
	        	List<GraphNode> lineNodes = line.getCenterlineNodes();
	        	if (lineColor == null)
	        	{
	        		for (GraphNode ln: lineNodes)
	        		{	
	        			colorProcs[ln.z].putPixel(ln.col, ln.row, colors.centerline);
	        		}
	        	}
	        	else
	        	{
	        		for (GraphNode ln: lineNodes)
	        		{	
	        			colorProcs[ln.z].putPixel(ln.col, ln.row, lineColor);
	        		}
	        	}
	            GraphNode bifurcation = line.getBifurcationNode();
	            if (bifurcation != null)
	            	colorProcs[bifurcation.z].putPixel(bifurcation.col, bifurcation.row, colors.bifurcation);
	        }
	    }
    }
    
    /** @todo Overlay red  centerline on image.
     * @return New color image. */
    public ImagePlus overlayColorCenterline(ImagePlus image, List<CenterlineGraph> graphs, int[] color)
    {
    	
    	ImageStack stack = image.getStack();
        int width = stack.getWidth();
        int height = stack.getHeight();
        int zSize = stack.getSize();
        ImagePlus centImage = new ImagePlus();
    	ImageStack centStack = new ImageStack(width, height);
    	
    	ImageProcessor[] centProcs = new ImageProcessor[zSize];
    	for (int i=1; i<=zSize; i++)
    	{
    		ImageProcessor p = stack.getProcessor(i);
    		ImageProcessor cp = p.convertToRGB();
    		centStack.addSlice(""+i, cp);
    		centProcs[i-1] = cp;
    	}
    	float minDFM = Float.MAX_VALUE;
    	float maxDFM = 0;
    	for (CenterlineGraph graph : graphs)
        {
 	       for (Centerline cent: graph.getCenterlines())
 	       {
 	           for (GraphNode node: cent.getCenterlineNodes())
 	           {
 	        	   if (node.dfm > maxDFM) maxDFM = node.dfm;
 	        	   if (node.dfm < minDFM) minDFM = node.dfm;
 	           }
 	       }  
        }
    	//float factor = 255.0F/maxDFM;
    	
        for (CenterlineGraph graph : graphs)
        {
	        for (Centerline cent: graph.getCenterlines())
	        {
	            for (GraphNode node: cent.getCenterlineNodes())
	            {
	            	//int[] color = new int[3];
	            	//  convert dfm to 0 to 255 red
	            	//color[0] = (short)(node.dfm*factor);
	            	//color[1] = 0;
	            	//color[2] = 0;
	            	centProcs[node.z].putPixel(node.col, node.row, color);
	            }
	        }  
        }
        centImage.setStack(image.getShortTitle()+"AllTissueCent", centStack);
        return centImage;
    }
    /** Makes color copy of short image and assigns the color parameter. */
    public static ImagePlus makeRGBcopy(ImagePlus image, String title, int[] color)
    {
    	ImageStack colorStack = new ImageStack(image.getWidth(), image.getHeight());
    	ImageStack inStack = image.getImageStack();
    	int width = inStack.getWidth();
    	for (int i=1; i <= image.getStackSize(); i++)
    	{
    		ImageProcessor ip = inStack.getProcessor(i);
    		short[] pixels = (short[])ip.getPixels();
    		ImageProcessor cp = ip.convertToRGB();
    		for (int x=0; x < width; x++)
    		{
    			for (int y=0; y < cp.getHeight(); y++)
    			{
    				if (pixels[y*width+x] > 0)
    				{
    					cp.putPixel(x, y, color);
    				}
    			}
    		}
    		colorStack.addSlice((i-1)+"", cp);
    	}
    	ImagePlus colorIm = new ImagePlus(title, colorStack);
    	return colorIm;
    }
    /** Makes color copy of the short image and puts a color centerline over top. */
    public static ImagePlus makeColorCenterlineOnImage(ImagePlus image, List<CenterlineGraph> centerlineGraphs, String title)
    {
    	Colors colors = Colors.getColors();
    	ImagePlus colorIm = makeRGBcopy(image, title, colors.gray);
    	overlayCenterlineOnColorImage(colorIm, centerlineGraphs, colors.centerline);
    	
    	return colorIm;
    }
    /** Color the centerlines and source nodes for each graph. See Colors for the color of the source node. */
    public static void overlayCenterlineOnColorImage(ImagePlus image, List<CenterlineGraph> centerlineGraphs, int[] color)
    {
    	int zSize = image.getStackSize();
    	ImageStack stack = image.getStack();
    	ImageProcessor[] procs = new ColorProcessor[zSize];
    	Colors colors = Colors.getColors();
    	for (int i=0; i< zSize; i++)
    	{
    		procs[i] = stack.getProcessor(i+1);
    	}
    	for (CenterlineGraph g: centerlineGraphs)
    	{
    		for (Centerline c: g.getCenterlines())
    		{
    			int[] lineColor = c.getRgb();
    			if (lineColor == null)
    			{
    				for (GraphNode n: c.getCenterlineNodes())
    				{
    					procs[n.z].putPixel(n.col, n.row, color);
    				}
    			}
    			else
    			{
    				for (GraphNode n: c.getCenterlineNodes())
    				{
    					procs[n.z].putPixel(n.col, n.row, lineColor);
    				}
    			}
    			GraphNode bif = c.getBifurcationNode();
    			if (bif != null)
    			{
    				procs[bif.z].putPixel(bif.col, bif.row, colors.bifurcation);
    			}
    		}
    		
    		// label the source/goal node 
    		GraphNode srcNode = g.getSourceNode();
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
    }
    /** Draw a value on each Position in a 3-D image  */
    public static void overlayPositionOnFloatImage(ImagePlus image, List<Position> poss, float value)
    {
    	ImageStack stack = image.getImageStack();
    	float[][] voxels = ImageProcess.getFloatStackVoxels(stack);
    	int width = image.getWidth();
    	for (Position p: poss)
    	{
    		voxels[p.getZ()][p.getRow()*width+p.getColumn()] = value;
    	}
    }
}

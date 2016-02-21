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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ij.*;
import ij.process.*;
import ktdiedrich.math.MatrixUtil;


/** Remove the scalp from a head MRI. Finds the outer edge of the head and scalps off a
 *  fixed depth set in the innerDist.
 * @author Karl Diedrich <ktdiedrich@gmail.com> 
 * */
public class Scalper
{
    public Scalper()
    {
        _edgeX = X_EDGE;
        _edgeY = Y_EDGE;
        _innerDist = INNER_DIST;
        _fitSize= FIT_SIZE;
        _jumpSize = JUMP_SIZE;
        _dilate = DILATE;
    }
    public static int Y_EDGE = 1;
    public static int X_EDGE = 1;
    public static int INNER_DIST = 40;
    public static int FIT_SIZE = 80;
    public static int JUMP_SIZE = 2;
    public static int DILATE = 10;
    private int _edgeY, _edgeX, _innerDist, _fitSize, _jumpSize, _dilate;
    private boolean _showSteps;
    
    public List<Edge> scalp(ImagePlus withScalp)
    {
        int height = withScalp.getHeight();
        int width = withScalp.getWidth();
        ImageProcessor withProc = withScalp.getProcessor();
        short[] pixels = (short[])withProc.getPixels();
        for (int r=0; r<height; r++)
        {
            pixels[r*width] = 0; 
            pixels[r*width+(width-1)] = 0; 
        }
        for (int c=0; c<width; c++)
        {
            pixels[c] = 0; 
            pixels[(height-1)*width+c] = 0; 
        }
        withScalp.updateAndDraw();
        
        ImageProcessor working = withProc.duplicate();
        short[] origPixels = (short[])working.getPixels();
        short[][] origSqr = MatrixUtil.array2Square(origPixels, height, width);
        short threshold = (short)Threshold.averageIntensity(withScalp);
        working.threshold(threshold);
        if (_showSteps)
        {
            ImagePlus thresholdImage = new ImagePlus("Threshold", working);
            thresholdImage.show();
            thresholdImage.updateAndDraw();
        }
        working.findEdges();
        if (_showSteps)
        {
            ImagePlus edgeImage = new ImagePlus("Edges", working);
            edgeImage.show();
            edgeImage.updateAndDraw();
        }
        short[] workingPixels = (short[])working.getPixels();
        short[][] workingSqr = MatrixUtil.array2Square(workingPixels, height, width);
        
        List<Edge> edges = getEdges(workingSqr, threshold);
        
        ImageProcessor scalpedProc = scalpEdges(withProc, edges);
        short[] scalpedPixels = (short[])scalpedProc.getPixels();
        if (_showSteps)
        {
            ImageProcessor proc = new ShortProcessor(width, height, scalpedPixels, null);
            ImagePlus image = new ImagePlus("Rough_scalp", proc);
            image.show();
            image.updateAndDraw();
        }
        short[][] scalpedSqr = MatrixUtil.array2Square(scalpedPixels, height, width);
        
        for (int d=0; d<_dilate; d++)
        {
            scalpedSqr = Morphology.dilate(scalpedSqr, origSqr);
        }
        if (_showSteps)
        {
            ImageProcessor dilatedProc = new ShortProcessor(width, height, MatrixUtil.square2array(scalpedSqr), null);
            ImagePlus dilatedImage = new ImagePlus("Dilated", dilatedProc);
            dilatedImage.show();
            dilatedImage.updateAndDraw();
        }
        edges = getEdges(scalpedSqr, threshold);
        List<Edge> fitScalp = Edge.curveFit(edges, _fitSize, width-1);
        List<Edge> inners = getInners(fitScalp, height, width);
        scalpEdgesRef(withProc, inners);
        
        return inners;
    }
    protected List<Edge> getEdges(short[][] sqr, short threshold)
    {
        int rows = sqr.length;
        int cols = sqr[0].length;
        List<Edge> edges = new ArrayList<Edge>();
        for (int r=_edgeY; r<=rows-_edgeY; r++)
        {
            Edge edge = null;
            for (int c=_edgeX; c<cols; c++)
            {
                if (sqr[r][c] > threshold)
                {
                    edge = new Edge();
                    edges.add(edge);
                    edge.setY(r);
                    edge.setLeftX(c);  
                    break;
                }
            }
            for (int c=cols-_edgeX; c>=_edgeX;c--)
            {
                if (sqr[r][c] > threshold)
                {
                    edge.setRightX(c);
                    break;
                }
            } 
        }
        return edges;
    }
    public List<Edge> getInners(List<Edge> outers, int rows, int cols)
    {
        short[][] scalpEdgeSqr = null;
        if (_showSteps)
        {
            scalpEdgeSqr = new short[rows][cols];
        }
        
        int midY = outers.get(outers.size()/2).getY();
        List<Edge> inners = new LinkedList<Edge>();
        
        for (Edge e : outers)
        {
            // System.out.println(e);
            int eY = e.getY();
            if (eY < midY-_innerDist)
                eY += _innerDist;
            else if (eY > midY+_innerDist)
                eY -= _innerDist;
            Edge inner = new Edge(eY, e.getLeftX()+_innerDist, e.getRightX()-_innerDist);
            inners.add(inner);
            if (_showSteps)
            {
                scalpEdgeSqr[e.getY()][e.getLeftX()] = 255;
                scalpEdgeSqr[e.getY()][e.getRightX()] = 255;
                scalpEdgeSqr[inner.getY()][inner.getLeftX()] = 255;
                scalpEdgeSqr[inner.getY()][inner.getRightX()] = 255;
            }
        }
        if (_showSteps)
        {
            ImageProcessor scalpEdgeProc = new ShortProcessor(cols, rows, MatrixUtil.square2array(scalpEdgeSqr), null);
            ImagePlus scalpEdge = new ImagePlus("Scalp_Edge", scalpEdgeProc);
            scalpEdge.show();
            scalpEdge.updateAndDraw();
        }
        return inners;
    }
    public static void scalpEdgesRef(ImageProcessor proc, List<Edge> edges)
    {
    	if (edges.size() == 0)
    		return;
        int rows = proc.getHeight();
        int cols = proc.getWidth();
        int startY = edges.get(0).getY();
        int stopY = edges.get(edges.size()-1).getY();
        for (int r=0; r < startY; r++)
            for (int c=0; c<cols; c++)
            proc.putPixel(c, r, 0);
        for (int r=stopY+1; r<rows; r++)
            for (int c=0; c<cols; c++)
                proc.putPixel(c, r, 0);
        for (Edge ie: edges)
        {
            int y = ie.getY();
            int lX = ie.getLeftX();
            int rX = ie.getRightX();
            for (int c=0; c < lX; c++)
            {
                proc.putPixel(c, y, 0);
            }
            for (int c = cols; c > rX; c--)
            {
                proc.putPixel(c, y, 0);
            }/** Fit the edge to polynomials of this size. */
        }
    }
    public static void scalpStack(ImageStack stack, List<Edge> edges)
    {
        for (int i=1; i<= stack.getSize(); i++)
        {
            ImageProcessor proc = stack.getProcessor(i);
            scalpEdgesRef(proc, edges);
        }
    }
    public static ImageProcessor scalpEdges(ImageProcessor withScalpProc, List<Edge> edges)
    {
        ImageProcessor proc = withScalpProc.duplicate();
        scalpEdgesRef(proc, edges);
        
        return proc;
    }
    /** The edge of the image often has artifacts. Start looking for the scalp a few pixels in. */
    public int getEdgeY()
    {
        return _edgeY;
    }
    /** The edge of the image often has artifacts. Start looking for the scalp a few pixels in. */
    public void setEdgeY(int edgeY)
    {
        _edgeY = edgeY;
    }
    /** The edge of the image often has artifacts. Start looking for the scalp a few pixels in. */
    public int getEdgeX()
    {
        return _edgeX;
    }
    /** The edge of the image often has artifacts. Start looking for the scalp a few pixels in. */
    public void setEdgeX(int edgeX)
    {
        _edgeX = edgeX;
    }
    /** @return the distance or depth of the scalp. */
    public int getInnerDist()
    {
        return _innerDist;
    }
    /** @param innerDist The distance or depth of the scalp. */
    public void setInnerDist(int innerDist)
    {
        _innerDist = innerDist;
    }
    /** Fit the edge to polynomials of this size. */
    public int getFitSize()
    {
        return _fitSize;
    }
    /** Fit the edge to polynomials of this size. */
    public void setFitSize(int fitSize)
    {
        _fitSize = fitSize;
    }
    public int getJumpSize()
    {
        return _jumpSize;
    }
    public void setJumpSize(int jumpSize)
    {
        _jumpSize = jumpSize;
    }
    public int getDilate()
    {
        return _dilate;
    }
    public void setDilate(int dilate)
    {
        _dilate = dilate;
    }
    public boolean isShowSteps()
    {
        return _showSteps;
    }
    public void setShowSteps(boolean showSteps)
    {
        _showSteps = showSteps;
    }
}

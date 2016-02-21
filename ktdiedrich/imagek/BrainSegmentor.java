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

import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class BrainSegmentor
{
    public static  short EDGE_THRESHOLD = 800;
    public static int INNER_DIST = 10;
    private boolean _showSteps;
    private short _edgeThreshold;
    private int _innerDist;
    private Point _startPoint;
    private ImageStack _origStack;
    
    public BrainSegmentor()
    {
        _edgeThreshold = EDGE_THRESHOLD;
        _innerDist = INNER_DIST;
    }
    
    
    public void findBrain()
    {
        int height = _origStack.getHeight();
        int width = _origStack.getWidth();
        ImageStack edgeStack = new ImageStack(width, height);
        ImageStack brainStack = new ImageStack(width, height);
        if (_startPoint == null)
        {
            _startPoint = new Point(width/2, height/2);
        }
        
        Scalper scalper = new Scalper();
        scalper.setInnerDist(_innerDist);
        for (int i=1; i<=_origStack.getSize(); i++)
        {
            ImageProcessor origProc = _origStack.getProcessor(i);
            short[] origPixels = (short[])origProc.getPixels();
            ImageProcessor edgeProc = origProc.duplicate();
            edgeProc.findEdges();
            short[] edgePixels = (short[])edgeProc.getPixels();
            ImageProcessor brainProc = new ShortProcessor(width, height);
            brainStack.addSlice(""+1, brainProc);
            short[] brainPixels = (short[])brainProc.getPixels();
            edgeStack.addSlice(""+i, edgeProc);
            
            LinkedList<Edge> edges = new LinkedList<Edge>();
            for (int row=_startPoint.y; row < height; row++)
            {
                Edge edge = new Edge();
                edges.add(edge);
                edge.setY(row);
                if (edgePixels[row*width+_startPoint.x] >= _edgeThreshold)
                {
                    edge.setLeftX(_startPoint.x);
                    edge.setRightX(_startPoint.x);
                    break;
                }
                for (int col=_startPoint.x; col < width; col++)
                {
                    if (edgePixels[row*width+col] < _edgeThreshold)
                    {
                        brainPixels[row*width+col] = origPixels[row*width+col];
                        
                    }
                    else
                    {
                        edge.setRightX(col);
                        break;
                    }
                }
                
                for (int col=_startPoint.x-1; col > 0; col--)
                {
                    if (edgePixels[row*width+col] < _edgeThreshold)
                    {
                        brainPixels[row*width+col] = origPixels[row*width+col];
                        
                    }
                    else
                    {
                        edge.setLeftX(col);
                        break;
                    }
                }
                
            }
            for (int row=_startPoint.y-1; row > 0; row--)
            {
                Edge edge = new Edge();
                edges.push(edge);
                edge.setY(row);
                if (edgePixels[row*width+_startPoint.x] >= _edgeThreshold)
                {
                    edge.setLeftX(_startPoint.x);
                    edge.setRightX(_startPoint.x);
                    break;
                }
                for (int col=_startPoint.x; col < width; col++)
                {
                    if (edgePixels[row*width+col] < _edgeThreshold)
                    {
                        brainPixels[row*width+col] = origPixels[row*width+col];
                        
                    }
                    else
                    {
                        edge.setRightX(col);
                        break;
                    }
                }
                
                for (int col=_startPoint.x-1; col > 0; col--)
                {
                    if (edgePixels[row*width+col] < _edgeThreshold)
                    {
                        brainPixels[row*width+col] = origPixels[row*width+col];
                        
                    }
                    else
                    {
                        edge.setLeftX(col);
                        break;
                    }
                }
               
            }
            brainProc.resetMinAndMax();
            List<Edge> inner = scalper.getInners(edges, height, width);
            Scalper.scalpEdgesRef(origProc, inner);
            // center point
            edgePixels[_startPoint.y*width + _startPoint.x] =  127;
            
        }
        if (_showSteps)
        {
            ImagePlus edgeImage = new ImagePlus();
            edgeImage.setStack("BoneBrainEdges", edgeStack);
            edgeImage.show();
            edgeImage.updateAndDraw();
            
            ImagePlus brainImage = new ImagePlus();
            brainImage.setStack("Brain", brainStack);
            brainImage.show();
            brainImage.updateAndDraw();
        }
            
    }
    public boolean isShowSteps()
    {
        return _showSteps;
    }
    public void setShowSteps(boolean showSteps)
    {
        _showSteps = showSteps;
    }


    public short getEdgeThreshold()
    {
        return _edgeThreshold;
    }


    public void setEdgeThreshold(short edgeThreshold)
    {
        _edgeThreshold = edgeThreshold;
    }


    public int getInnerDist()
    {
        return _innerDist;
    }


    public void setInnerDist(int innerDist)
    {
        _innerDist = innerDist;
    }


    public Point getStartPoint()
    {
        return _startPoint;
    }


    public void setStartPoint(Point startPoint)
    {
        _startPoint = startPoint;
    }


    public ImageStack getOrigStack()
    {
        return _origStack;
    }


    public void setOrigStack(ImageStack origStack)
    {
        _origStack = origStack;
    }
}

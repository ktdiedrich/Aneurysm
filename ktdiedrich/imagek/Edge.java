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
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Random;

import ij.measure.CurveFitter;

/** Location of an Edge pixel
 * @author Karl Diedrich <ktdiedrich@gmail.com> 
 * */
public class Edge
{
	private int _y, _leftX, _rightX;
    private short _leftValue, _rightValue;
    Edge()
    {
        
    }
    Edge(int y, int leftX, int rightX)
    {
        _y=y;
        _leftX=leftX;
        _rightX=rightX;
    }
    public String toString()
    {
        return "["+_leftX+", "+_rightX+"]";
    }
    /** @return Array of 2 of each y value as a double. 2 of each y value is 
     * needed since there is a left and a right x value. */
    public static double[] getY(List<Edge> edges)
    {
        double[] y = new double[edges.size()];
        
        for (int i=0; i < edges.size(); i++)
        {
            y[i] = (double)edges.get(i).getY();
        }
        return y;
    }
    /** @return Array of x values order left x. */
    public static double[] getleftX(List<Edge> edges)
    {
        double[] x = new double[edges.size()];
       
        for (int i=0; i < edges.size(); i++)
        {
            x[i] = edges.get(i).getLeftX();
        }
        return x;
    }
    public static double[] getRightX(List<Edge> edges)
    {
        double[] x = new double[edges.size()];
        
        for (int i=0; i < edges.size(); i++)
        {
            x[i] = edges.get(i).getRightX();
        }
        return x;
    }
    
    public static List<Edge> curveFit(List<Edge> edges, int fitSize)
    {
        return curveFit(edges, fitSize, 0);
    }
    
    /** @param max Set 0 for no maximum. Used to prevent curve fits from running off the edge of the image. 
     */
    public static List<Edge> curveFit(List<Edge> edges, int fitSize, int max)
    {
        List<Edge> fit = new LinkedList<Edge>();
        double[] rightX = Edge.getRightX(edges);
        double[] leftX = Edge.getleftX(edges);
        double[] allY = Edge.getY(edges);  
        int pos = 0;
        for (int k=0; k < allY.length; k+=fitSize)
        {
            int end = k+fitSize;
            if (end > allY.length)
                end = allY.length;
            double[] r = Arrays.copyOfRange(rightX, k, end);
            double[] l = Arrays.copyOfRange(leftX, k, end);
            double[] y = Arrays.copyOfRange(allY, k, end);
            CurveFitter leftCurve = new CurveFitter(y, l);
            CurveFitter rightCurve = new CurveFitter(y, r);
            leftCurve.doFit(CurveFitter.POLY4);
            rightCurve.doFit(CurveFitter.POLY4);
            //System.out.println(leftCurve.getResultString());
            //System.out.println(rightCurve.getResultString());
        
            double[] leftParam = leftCurve.getParams();
            double[] rightParam = rightCurve.getParams();
            
            for (int i=0; i < y.length; i++)
            {
                if (pos < allY.length)
                {
                    Edge e = new Edge();
                    e.setY((int)y[i]);
                    // left 
                    int fitVal = (int) (leftParam[0]  + leftParam[1]*y[i] + leftParam[2]*Math.pow(y[i], 2) + 
                            leftParam[3]*Math.pow(y[i], 3)+ leftParam[4]*Math.pow(y[i], 4));
                    if (fitVal < 0)
                        fitVal = 0;
                    e.setLeftX( fitVal );
                    
                    // right
                    fitVal = (int) (rightParam[0] + rightParam[1]*y[i] + rightParam[2]*Math.pow(y[i], 2) +
                            rightParam[3]*Math.pow(y[i], 3) + rightParam[4]*Math.pow(y[i], 4));
                    if (max > 0 && fitVal > max)
                        fitVal = max;
                    e.setRightX(fitVal);
                    fit.add(e);
                }
                pos++;
            }
        }   
        return fit;
    }
    
    
    int getY()
    {
        return _y;
    }
    void setY(int y)
    {
        _y = y;
    }
    int getLeftX()
    {
        return _leftX;
    }
    void setLeftX(int leftX)
    {
        _leftX = leftX;
    }
    int getRightX()
    {
        return _rightX;
    }
    void setRightX(int rightX)
    {
        _rightX = rightX;
    }
    public short getLeftValue()
    {
        return _leftValue;
    }
    public void setLeftValue(short value)
    {
        _leftValue = value;
    }
    public short getRightValue()
    {
        return _rightValue;
    }
    public void setRightValue(short rightValue)
    {
        _rightValue = rightValue;
    }
}
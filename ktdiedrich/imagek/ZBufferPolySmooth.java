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

import java.util.LinkedList;
import java.util.Queue;
import java.util.Map;
import java.util.HashMap;

import ij.*;
import ij.measure.CurveFitter;
import ij.process.*;
import ktdiedrich.math.*;

/** Smooth a zbuffer image using ploynomials in 4 directions 
  * @author Karl Diedrich <ktdiedrich@gmail.com>
  */
public class ZBufferPolySmooth
{
    public static final int K_LENGTH = 2;
    public static final double A = 5000.0;
    public static final double B = 1.0;
    public static final double MAX_CHISQ = 1.0;
    public static final short Z_DIFF = 2;
    public static final int CLUSTER_SIZE = 30;
    
    private int _kLength;
    private double _a;
    private double _b;
    private double[][] _chisqr;
    private Map<Short, Cluster> _clusters;
    private double _maxChisq;
    private short _zDiff;
    private int _clusterSize;
    private Queue<Position> _checkPos;
    private short _clusterCount;
    private short[][] _clusterSizes;
    private short[][] _zSqr;
    private boolean _showSteps;
    
    public ZBufferPolySmooth()
    {
        _kLength = K_LENGTH;
        _a = A;
        _b = B;
        _maxChisq = MAX_CHISQ;
        _zDiff =Z_DIFF;
        _clusterSize = CLUSTER_SIZE;
    }
    
    protected void checkNeighbor(short[][] clusterSqr, int c, int r, Cluster cluster, short compare)
    {
        if (clusterSqr[r][c] == 0 && (Math.abs(_zSqr[r][c] - compare) < _zDiff)  )
        {
            clusterSqr[r][c] = cluster.getLabel();
            cluster.addOneSize();
            // TODO make Position(x, y, z)
            _checkPos.add(new Position(c, r));
        }
     }
    
    protected void clusterNeighbors(short[][] clusterSqr, int c, int r)
    {
       
        int lastCol = _zSqr[0].length;
        int lastRow = _zSqr.length;
        
        Cluster curCluster = null;
        
        if (_zSqr[r][c] > 0 && _chisqr[r][c] < _maxChisq && r > 0 && c > 0 && r < lastRow-1 && c < lastCol-1 )
        {
            if (clusterSqr[r][c] > 0)
            {
                curCluster = _clusters.get(clusterSqr[r][c]);
            }
            else
            {
                curCluster = new Cluster(_clusterCount);
                _clusters.put(_clusterCount, curCluster);
                clusterSqr[r][c] = _clusterCount;
                _clusterCount++;
            }
            checkNeighbor(clusterSqr, c-1, r-1, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c-1, r, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c-1, r+1, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c, r-1, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c, r+1, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c+1, r-1, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c+1, r, curCluster, _zSqr[r][c]);
            checkNeighbor(clusterSqr, c+1, r+1, curCluster, _zSqr[r][c]);
        }
    }
    
    
    protected ImagePlus cluster()
    {
        _checkPos = new LinkedList<Position>();
        int height = _zSqr.length;
        int width = _zSqr[0].length;
        short[][] clusterSqr = new short[height][width];
        _clusters = new HashMap<Short, Cluster>();
        _clusterCount = 1001;
        for (int r=1; r<height-1; r++)
        {
            for (int c=1; c<width-1; c++)
            {
                clusterNeighbors(clusterSqr, c, r);
                while (_checkPos.isEmpty() == false)
                {
                   Position pos = _checkPos.remove();
                   clusterNeighbors(clusterSqr, pos.getColumn(), pos.getRow());
                }
            }
        }
        if (_showSteps)
        {
            ImageProcessor numberProc = new ShortProcessor(width, height, MatrixUtil.square2array(clusterSqr), null);
            ImagePlus numbers = new ImagePlus("Cluster_Numbers_No_threshold", numberProc);
            numbers.show();
            numbers.updateAndDraw();
        }
        _clusterSizes = new short[height][width];
        for (int r=1; r<height-1; r++)
        {
            for (int c=1; c<width-1; c++)
            {
                if ((clusterSqr[r][c] != 0))
                {
                    Cluster curClus = _clusters.get(clusterSqr[r][c]);
                    int clusMembers = curClus.getSize();
                    if (clusMembers > _clusterSize)
                    {
                        _clusterSizes[r][c] = (short)(clusMembers+20);
                    }
                }
            }
        }
        ImageProcessor sizesProc = new ShortProcessor(width, height, MatrixUtil.square2array(_clusterSizes), null);
        ImagePlus sizes = new ImagePlus("Cluster_Size_Clus_"+_clusterSize+"_Chisq_"+_maxChisq+"_Zdiff_"+_zDiff, 
                sizesProc);
        
        return sizes;
    }
    public ImagePlus smooth(ImagePlus zImage)
    {
        int height = zImage.getHeight();
        int width = zImage.getWidth();
        ImageProcessor ziproc = zImage.getProcessor();
        _zSqr = MatrixUtil.array2Square((short[])ziproc.getPixels(), height, width);
        short[][] adjSqr = smoothSqr(_zSqr, height, width);
        
        ImageProcessor smoothProc = new ShortProcessor(width, height, MatrixUtil.square2array(adjSqr), null);
        ImagePlus smoothed = new ImagePlus("Polynomial_Smoothed_Z_Image_A_"+_a+"_B_"+_b, smoothProc);
        
        ImagePlus clustered = cluster();
        if (_showSteps)
        {
            clustered.show();
            clustered.updateAndDraw();
        }
        
        return smoothed;
    }
    protected double fitScore(short[] line)
    {
        int len = line.length;
        double[] xData = new double[len];
        double[] yData = new double[len];
        for (int i=0; i<len; i++)
        {
            xData[i]=(double)i;
            yData[i] = (double)line[i];
        }
        double diffSum2=0.0;
        CurveFitter curve = new CurveFitter(xData, yData);
        curve.doFit(CurveFitter.POLY2);
        double[] params = curve.getParams();
        double[] expLine = new double[len];
        
        for (int i=0; i<len; i++)
        {
             expLine[i] = (params[0]  + params[1]*xData[i] + 
                    params[2]*Math.pow(xData[i], 2)  );
             
             diffSum2 += Math.pow((line[i]-expLine[i]), 2.0);
        }
        
        return diffSum2;
    }
    protected short[][] smoothSqr(short[][] sqr, int height, int width)
    {
        _chisqr = new double[height][width];
        short[][] adjSqr = new short[height][width];
        int end = (_kLength*2)+1;
        for (int r=_kLength; r < height-_kLength; r++)
        {
            for (int c=_kLength; c<width-_kLength; c++)
            {
                if (sqr[r][c] == 0)
                    continue;
                double minChij = 0.0;
                double score = 0.0;
                short[] lineJ0 = new short[end];
                //j=0
                int m=0;
                for (int k=-_kLength; k<=_kLength; k++)
                {
                    lineJ0[m] = sqr[r+k][c];
                    m++;
                }
                minChij = fitScore(lineJ0);
                
                // j=1
                m=0;
                short[] lineJ1=new short[end];
                for (int k=-_kLength; k<=_kLength; k++)
                {
                    lineJ1[m] = sqr[r][c+k];
                    m++;
                }
                score = fitScore(lineJ1);
                if (score < minChij)
                    minChij = score;
                
                // j=2
                m=0;
                short[] lineJ2 = new short[end];
                for (int k=-_kLength; k<=_kLength; k++)
                {
                    lineJ2[m] = sqr[r-k][c+k];
                    m++;
                }
                score = fitScore(lineJ2);
                if (score < minChij)
                    minChij = score;
                
                // j=3
                m=0;
                short[] lineJ3 = new short[end];
                for (int k=-_kLength; k<=_kLength; k++)
                {
                    lineJ3[m] = sqr[r+k][c+k];
                    m++;
                }
                score = fitScore(lineJ3);
                if (score < minChij)
                    minChij = score;
                _chisqr[r][c] = minChij;
                short adjVal = (short)(_a/(_b+minChij));
                adjSqr[r][c] =  adjVal;
            }
            
        }
        return adjSqr;
    }
    /** 
     * @param args
     */
    public static void main(String[] args)
    {
        ZBufferPolySmooth smoother = new ZBufferPolySmooth();
        short[][] zBuf = new short[6][6];
        int top = 9;
        short val = 1;
        for (int i=0; i<6; i++)
        {
            for (int j=0; j<6;j++)
            {
                zBuf[i][j] = val;
                val++;
                if (val > top)
                    val = 1;
            }
        }
        short[][] adjSqr = smoother.smoothSqr(zBuf, 6, 6);
    }
    public int getKLength()
    {
        return _kLength;
    }

    public void setKLength(int length)
    {
        _kLength = length;
    }
    
    public double getA()
    {
        return _a;
    }

    public void setA(double a)
    {
        _a = a;
    }
    public double getB()
    {
        return _b;
    }
    public void setB(double b)
    {
        _b = b;
    }
    public double getMaxChisq()
    {
        return _maxChisq;
    }
    public void setMaxChisq(double maxChisq)
    {
        _maxChisq = maxChisq;
    }
    public short getZDiff()
    {
        return _zDiff;
    }
    public void setZDiff(short diff)
    {
        _zDiff = diff;
    }

    public int getClusterSize()
    {
        return _clusterSize;
    }

    public void setClusterSize(int clusterSize)
    {
        _clusterSize = clusterSize;
    }

    public short[][] getZSqr()
    {
        return _zSqr;
    }

    public short[][] getClusterSizes()
    {
        return _clusterSizes;
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


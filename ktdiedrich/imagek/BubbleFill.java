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
import java.util.*;

import ij.*;

/** Fill completely surrounded bubbles in as artery using region labeling
 * of 0 value voxels. 
 * @author ktdiedrich@gmail.com
 * */
public class BubbleFill 
{
	// Bubble fill algorithm IDs defined in aneurysm.algorithm table 
	public static final int MAX_BUBBLE_SIZE = 512000;
	public static final int BUBBLE_FILL_3D = 9;
	public static final int BUBBLE_FILL_2D_PLANES = 10;
	private int _width3D, _height3D, _zSize3D;
	private short[][] _labels3D;
	private int _maxSize2D;
	private short _maxLabel2D;
	private int _maxSize3D;
	private short _maxLabel3D;
	private Set<Short> _large3D;
	
	public BubbleFill()
	{
		
	}
	
	/** Iterates through 2D slices in X, Y and Z planes ands fill 2D bubbles in segmentation */
	private void fill2Dslices(short[][] unsegVoxels, short[][] segVoxels)
	{
		// x
		IJ.log("Bubble fill 2D slices");
		for (int x=0; x < _width3D; x++)
		{
			short[][] segXplane = new short[_zSize3D][_height3D];
			short[][] unsegXplane = new short[_zSize3D][_height3D];
			for (int z=0; z < _zSize3D; z++)
			{
				for (int y=0; y < _height3D; y++)
				{
					segXplane[z][y] = segVoxels[z][y*_width3D+x];
					unsegXplane[z][y] = unsegVoxels[z][y*_width3D+x];
				}
			}
			fill2Dbubbles(unsegXplane, segXplane);
			for (int z=0; z < _zSize3D; z++)
			{
				for (int y=0; y < _height3D; y++)
				{
					segVoxels[z][y*_width3D+x] = segXplane[z][y];
				}
			}
		}
		
		// y
		for (int y=0; y < _height3D; y++)
		{
			short[][] segYplane = new short[_width3D][_zSize3D];
			short[][] unsegYplane = new short[_width3D][_zSize3D];
			for (int x=0; x < _width3D; x++)
			{
				for (int z=0; z < _zSize3D; z++)
				{
					segYplane[x][z] = segVoxels[z][y*_width3D+x];
					unsegYplane[x][z] = unsegVoxels[z][y*_width3D+x];
				}
			}
			fill2Dbubbles(unsegYplane, segYplane);
			for (int x=0; x < _width3D; x++)
			{
				for (int z=0; z < _zSize3D; z++)
				{
					segVoxels[z][y*_width3D+x] = segYplane[x][z];
				}
			}
		}
		
		// z
		for (int z=0; z < _zSize3D; z++)
		{
			short[][] segZplane =  new short[_width3D][_height3D];
			short[][] unsegZplane =  new short[_width3D][_height3D];
			for (int x=0; x < _width3D; x++)
			{
				for (int y=0; y < _height3D; y++)
				{
					segZplane[x][y] = segVoxels[z][y*_width3D+x];
					unsegZplane[x][y] = unsegVoxels[z][y*_width3D+x];
				}
			}
			fill2Dbubbles(unsegZplane, segZplane);
			for (int x=0; x < _width3D; x++)
			{
				for (int y=0; y < _height3D; y++)
				{
					segVoxels[z][y*_width3D+x] = segZplane[x][y];
				}
			}
		}
		
	}
	/** Fills bubbles in a 2D plane */
	private void fill2Dbubbles(short[][] unsegPlane, short[][] segPlane)
	{
		int w = segPlane.length;
		int h = segPlane[0].length;
		_maxSize2D = 0;
		_maxLabel2D = (short)0;
		short label = 2;
		short[][] labels2D = new short[segPlane.length][segPlane[0].length];
		for (int x=0; x<w; x++)
        {
            for (int y=0; y<h; y++)
            {
            	
                if (segPlane[x][y] == 0  && labels2D[x][y] == 0)
                {
                    floodFill2D(segPlane, x, y, label, labels2D);
                    label++;
                }
            }
        }
        // remove the label of the largest outside region 
        
        for (int x=0; x<w; x++)
        {
            for (int y=0; y<h; y++)
            {
                if (labels2D[x][y] == _maxLabel2D)
                {
                    labels2D[x][y] = 0;
                }
        		else
        		{
        			segPlane[x][y] = unsegPlane[x][y];
        		}
            }
        }
        // ImageProcess.display(labels2D, w, h, "bubbleLabel2D");
	}
	public void fillBubbles(ImageStack unsegStack, ImageStack segStack, int algorithm)
    {
        _zSize3D = segStack.getSize();
        _height3D = segStack.getHeight();
        _width3D = segStack.getWidth();
        short[][] segVoxels = new short[_zSize3D][];
        short[][] unsegVoxels = new short[_zSize3D][];
        _labels3D = new short[_zSize3D][_width3D*_height3D];
        _maxSize3D = 0;
        _maxLabel3D = 0;
        _large3D = new HashSet<Short>();
        for (int z=0; z<_zSize3D; z++ )
        {
            segVoxels[z] = (short[])segStack.getPixels(z+1);
            unsegVoxels[z] = (short[])unsegStack.getPixels(z+1);
        }
        if (algorithm == BUBBLE_FILL_2D_PLANES)
        {
        	fill2Dslices(unsegVoxels, segVoxels);
        }
        else if (algorithm == BUBBLE_FILL_3D)
        {
        	fillBubbles3D(unsegVoxels, segVoxels);
        }
        // ImageProcess.display(_labelVoxels, _width, _height, "bubbleLabel");
    }
	private void fillBubbles3D(short[][] unsegVoxels, short[][] segVoxels)
	{
		IJ.log("Bubble fill 3D. ");
		short label = 2;
        for (int x=0; x<_width3D; x++)
        {
            for (int y=0; y<_height3D; y++)
            {
            	for (int z=0; z<_zSize3D; z++ )
                {
                    if (segVoxels[z][y*_width3D+x] == 0  && _labels3D[z][y*_width3D+x] == 0)
                    {
                        floodFill3D(segVoxels, x, y, z, label);
                        label++;
                    }
                }
            }
        }
        // remove the label of the largest outside region 
        
        for (int x=0; x<_width3D; x++)
        {
            for (int y=0; y<_height3D; y++)
            {
            	for (int z=0; z<_zSize3D; z++ )
                {
                    //if (_labelVoxels[z][y*_width+x] == _maxLabel)
                    //{
                    //    _labelVoxels[z][y*_width+x] = 0;
                    //}
            		short l = _labels3D[z][y*_width3D+x];
            		if (_large3D.contains(l))
            		{
            			_labels3D[z][y*_width3D+x] = 0;
            		}
            		else
            		{
            			short o = unsegVoxels[z][y*_width3D+x];
            			segVoxels[z][y*_width3D+x] = o;
            			//if (o > HoleFill.MIN_FILL_VALUE)
            			//	segVoxels[z][y*_width3D+x] = o;
            			//else
            			//	segVoxels[z][y*_width3D+x] = HoleFill.MIN_FILL_VALUE;
            		}
                }
            }
        }
	}
	public void floodFill3D(short[][] voxels, int x, int y, int z, short label)
	{
		LinkedList<Point> q = new LinkedList<Point>();
		q.addFirst(new Point(x, y, z));
		int size = 0;
		while (!q.isEmpty())
		{
			Point n = q.removeLast();
			if ((n.x>=0) && (n.x<_width3D) && (n.y>=0) && (n.y<_height3D) && (n.z>=0) && (n.z<_zSize3D) 
					&& voxels[n.z][n.y*_width3D+n.x] == 0 && _labels3D[n.z][n.y*_width3D+n.x]==0)
			{
				_labels3D[n.z][n.y*_width3D+n.x] = label;
				size++;
				q.addFirst(new Point(n.x+1, n.y, n.z));
				q.addFirst(new Point(n.x, n.y+1, n.z));
				q.addFirst(new Point(n.x, n.y, n.z+1));
				q.addFirst(new Point(n.x-1, n.y, n.z));
				q.addFirst(new Point(n.x, n.y-1, n.z));
				q.addFirst(new Point(n.x, n.y, n.z-1));
				
				q.addFirst(new Point(n.x+1, n.y+1, n.z));
				q.addFirst(new Point(n.x, n.y+1, n.z+1));
				q.addFirst(new Point(n.x+1, n.y, n.z+1));
				q.addFirst(new Point(n.x-1, n.y-1, n.z));
				q.addFirst(new Point(n.x, n.y-1, n.z-1));
				q.addFirst(new Point(n.x-1, n.y, n.z-1));
				q.addFirst(new Point(n.x+1, n.y-1, n.z));
				q.addFirst(new Point(n.x-1, n.y+1, n.z));
				q.addFirst(new Point(n.x, n.y+1, n.z-1));
				q.addFirst(new Point(n.x, n.y-1, n.z+1));
				q.addFirst(new Point(n.x+1, n.y, n.z-1));
				q.addFirst(new Point(n.x-1, n.y, n.z+1));
				
				
				q.addFirst(new Point(n.x+1, n.y+1, n.z+1));
				q.addFirst(new Point(n.x+1, n.y+1, n.z-1));
				q.addFirst(new Point(n.x+1, n.y-1, n.z+1));
				q.addFirst(new Point(n.x-1, n.y+1, n.z+1));
				q.addFirst(new Point(n.x-1, n.y-1, n.z+1));
				q.addFirst(new Point(n.x+1, n.y-1, n.z-1));
				q.addFirst(new Point(n.x-1, n.y+1, n.z-1));
				q.addFirst(new Point(n.x-1, n.y-1, n.z-1));
			}
		}
		//System.out.println("Region size: "+size);
		if (size > _maxSize3D)
		{
			_maxSize3D = size;
			_maxLabel3D = label;
			// System.out.println("Max label: "+_maxLabel+ " size: "+_maxSize);
		}
		if (size > MAX_BUBBLE_SIZE)
		{
			_large3D.add(label);
		}
	}	
	
	public void floodFill2D(short[][] voxels, int x, int y, short label, short[][] labels2D)
	{
		LinkedList<Point> q = new LinkedList<Point>();
		q.addFirst(new Point(x, y));
		int size = 0;
		int w = voxels.length;
		int h = voxels[0].length;
		while (!q.isEmpty())
		{
			Point n = q.removeLast();
			if ((n.x >= 0) && (n.x < w) && (n.y >= 0) && (n.y < h)
					&& voxels[n.x][n.y] == 0 && labels2D[n.x][n.y]==0)
			{
				labels2D[n.x][n.y] = label;
				size++;
				
				q.addFirst(new Point(n.x+1, n.y));
				q.addFirst(new Point(n.x, n.y+1));
				q.addFirst(new Point(n.x-1, n.y));
				q.addFirst(new Point(n.x, n.y-1));
				q.addFirst(new Point(n.x+1, n.y+1));
				q.addFirst(new Point(n.x-1, n.y-1));
				q.addFirst(new Point(n.x+1, n.y-1));
				q.addFirst(new Point(n.x-1, n.y+1));
			}
		}
		//System.out.println("Region size: "+size);
		if (size > _maxSize2D)
		{
			_maxSize2D = size;
			_maxLabel2D = label;
			// System.out.println("Max label: "+_maxLabel+ " size: "+_maxSize);
		}
	}	
}


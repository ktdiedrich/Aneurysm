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

import java.util.*;

// in BubbleFill file 
import ktdiedrich.imagek.Point;

/** Find clusters in a 3-D image and thresholds based on relative cluster size 
 * @author ktdiedrich@gmail.com 
 * */
public class Clusters 
{
	private int _width, _height, _zSize;
	private short[][] _labelVoxels;
	private short[][] _voxels;
	private LinkedList<Cluster> _imageClusters;
	private ImagePlus _image;
	public Clusters(ImagePlus image)
	{
		_image = image;
		ImageStack stack = image.getImageStack();
    	
    	_zSize = stack.getSize();
        _height = stack.getHeight();
        _width = stack.getWidth();
        
        _voxels = new short[_zSize][];
        _labelVoxels = new short[_zSize][_width*_height];
        for (int z=0; z<_zSize; z++ )
        {
            _voxels[z] = (short[])stack.getPixels(z+1);
        }	
        _imageClusters = imageClusters();
	}
	/** Remove clusters below a percentile in size. */
	public void thresholdClusters(double belowPercentile)
	{
		int clusLen = _imageClusters.size();
		SortedSet<Short> delLabels = new TreeSet<Short>();
		int thres = (int)Math.floor(((double)clusLen)*belowPercentile);
		
		IJ.log("cluster "+thres+" size threshold: "+_imageClusters.get(thres).toString());
		// TODO record remaining clusters after removing 
		for (int i=0; i < thres; i++)
		{
			Cluster clst  = _imageClusters.removeFirst();
			short label = clst.getLabel();
			delLabels.add(label);
		}
		
		for (int z=0; z < _zSize; z++)
		{
			for (int i=0; i < _voxels[z].length; i++)
			{
				short lab = _labelVoxels[z][i];
				if (delLabels.contains(lab))
				{
					_voxels[z][i] = 0;
				}
			}
		}
		_image.updateAndDraw();
	}
	/** Remove clusters below a voxel size. */
	public void thresholdClusters(int size)
	{
		SortedSet<Short> delLabels = new TreeSet<Short>();
		// TODO remove from current cluster list 
		int s = 0;
		while (s < size)
		{
			Cluster c = _imageClusters.peekFirst();
			if (c != null)
			{
				s = c.getSize();
				short label = c.getLabel();
				if (s < size)
				{
					delLabels.add(label);
					_imageClusters.removeFirst();
				}
				else
				{
					break;
				}
			}
		}
		
		for (int z=0; z < _zSize; z++)
		{
			for (int i=0; i < _voxels[z].length; i++)
			{
				short lab = _labelVoxels[z][i];
				if (delLabels.contains(lab))
				{
					_voxels[z][i] = 0;
				}
			}
		}
		_image.updateAndDraw();
	}
	/** Make a list of the voxel clusters in an image from smallest to largest. */
    private LinkedList<Cluster> imageClusters()
    {
    	LinkedList<Cluster> clusters  = new LinkedList<Cluster>();
    	
        short label = 2;
        Cluster curCluster = new Cluster(label);
        clusters.add(curCluster);
        for (int x=0; x<_width; x++)
        {
            for (int y=0; y<_height; y++)
            {
            	for (int z=0; z<_zSize; z++ )
                {
                    if (_voxels[z][y*_width+x] > 0  && _labelVoxels[z][y*_width+x] == 0)
                    {
                    	// IJ.log("x: "+x+" y: "+y+" z: "+z+" value: "+_voxels[z][y*_width+x]);
                        floodFill(_voxels, x, y, z, curCluster);
                        label++;
                        curCluster = new Cluster(label);
                        clusters.add(curCluster);
                    }
                }
            }
        } 
        Collections.sort(clusters);
        
    	return clusters;
    }
    public ImagePlus getClusterImage()
    {
    	ImagePlus clusterImage = ImageProcess.makeImage(_labelVoxels, _width, _height, _image.getShortTitle()+"Clusters");
        
    	return clusterImage;
    }
    public void floodFill(short[][] voxels, int x, int y, int z, Cluster cluster)
	{
    	short label = cluster.getLabel();
		LinkedList<Point> q = new LinkedList<Point>();
		q.addFirst(new Point(x, y, z));
		
		while (!q.isEmpty())
		{
			Point n = q.removeLast();
			// IJ.log("("+n.x+", "+n.y+", "+n.z+")");
			if ((n.x>=0) && (n.x<_width) && (n.y>=0) && (n.y<_height) && (n.z>=0) && (n.z<_zSize) 
					&& voxels[n.z][n.y*_width+n.x] > 0 && _labelVoxels[n.z][n.y*_width+n.x]==0)
			{
				_labelVoxels[n.z][n.y*_width+n.x] = label;
				cluster.addOneSize();
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
	}

	public List<Cluster> getImageClusters() {
		return _imageClusters;
	}
    
}

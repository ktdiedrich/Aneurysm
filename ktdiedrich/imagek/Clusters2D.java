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
import ij.process.ShortProcessor;

import java.util.*;
import ktdiedrich.imagek.Point;

/** The the centers of 2-D weight costs image (float image) slices for the distal start points for generating centerlines of 
 * arteries that run off the bottom of the image stack.
 * @author Karl.Diedich@utah.edu 
 * */
public class Clusters2D 
{
	private int _width, _height;
	private short[] _labelVoxels;
	private float[] _voxels;
	private LinkedList<Cluster> _imageClusters;
	private ImageProcessor _imageProc;
	private int _zPosition;
	public Clusters2D(ImageProcessor imageProc)
	{
		this(imageProc, 0);
	}
	/** Z position is not used in this class, it is only stored in the Positions for later use. */
	public Clusters2D(ImageProcessor imageProc, int zPosition)
	{
		_imageProc = imageProc;
        _zPosition = zPosition;
		_height = _imageProc.getHeight();
        _width = _imageProc.getWidth();
        
        _labelVoxels = new short[_width*_height];
        _voxels = (float[])imageProc.getPixels();
       
        _imageClusters = imageClusters();
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
                if (_voxels[y*_width+x] > 0  && _labelVoxels[y*_width+x] == 0)
                {
                	// IJ.log("x: "+x+" y: "+y+" z: "+z+" value: "+_voxels[z][y*_width+x]);
                    floodFill(_voxels, x, y, curCluster);
                    label++;
                    curCluster = new Cluster(label);
                    clusters.add(curCluster);
                }
            }
        } 
        Collections.sort(clusters);
        
    	return clusters;
    }
    public ImagePlus getClusterImage(String name)
    {
    	ImageProcessor iProc = new ShortProcessor(_width, _height);
    	iProc.setPixels(_labelVoxels);
    	ImagePlus clusterImage = new ImagePlus(name, iProc);
    	
    	return clusterImage;
    }
    public ImagePlus getInputImage(String name)
    {
    	return new ImagePlus(name, _imageProc);
    }
    
    public void floodFill(float[] voxels, int x, int y, Cluster cluster)
	{
    	short label = cluster.getLabel();
		LinkedList<Point> q = new LinkedList<Point>();
		q.addFirst(new Point(x, y, 0));
		
		while (!q.isEmpty())
		{
			Point n = q.removeLast();
			// IJ.log("("+n.x+", "+n.y+", "+n.z+")");
			if ((n.x>=0) && (n.x<_width) && (n.y>=0) && (n.y<_height) 
					&& voxels[n.y*_width+n.x] > 0 && _labelVoxels[n.y*_width+n.x]==0)
			{
				_labelVoxels[n.y*_width+n.x] = label;
				cluster.addPosition(new Position(n.x, n.y, _zPosition));
//				cluster.addOneSize();
				q.addFirst(new Point(n.x+1, n.y, _zPosition));
				q.addFirst(new Point(n.x, n.y+1, _zPosition));
				q.addFirst(new Point(n.x-1, n.y, _zPosition));
				q.addFirst(new Point(n.x, n.y-1, _zPosition));
				
				q.addFirst(new Point(n.x+1, n.y+1, _zPosition));
				q.addFirst(new Point(n.x-1, n.y-1, _zPosition));
				q.addFirst(new Point(n.x+1, n.y-1, _zPosition));
				q.addFirst(new Point(n.x-1, n.y+1, _zPosition));
			}
		}
	}
    /** Find the minimum valued position of each cluster. 
     * @return List of the the Positions of the minimum value at each position, sorted larget to smallest. */
    public List<Position> getClusterMiddle()
    {
    	List<Position> minPoss = new LinkedList<Position>();
    	Collections.sort(_imageClusters, new ClusterLargerComp());
    	for (Cluster c: _imageClusters)
    	{
    		Position minP = null;
    		float minWeight = Float.MAX_VALUE;
    		List<Position> ps = c.getPositions();
    		for (Position p: ps)
    		{
    			float weight = _voxels[p.getRow()*_width+p.getColumn()];
    			if (weight < minWeight)
    			{
    				minWeight = weight;
    				minP = p;
    			}
    		}
    		if (minP != null)
    		{
    			minPoss.add(minP);
    		}
    	}
    	return minPoss;
    }
	public List<Cluster> getImageClusters() {
		return _imageClusters;
	}
    
}

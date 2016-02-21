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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import ktdiedrich.math.VectorMath;
import ij.IJ;
import ij.ImagePlus;


/** Phase Contrast functions. 
 * @author ktdiedrich@gmail.com 
 * TODO reduce replication between functions. */
public class PhaseContrast 
{

	public static final double PHASE_CONTRAST_ZERO = 2048;
    private short[][] _xPC;
    private short[][] _yPC;
    private short[][] _zPC;
    private int _width, _height, _zSize;
    private List<Double> _dots;
    private VoxelDistance _voxelDistance;
    private double _velocityDotSigma;
    private double _velocityPower;
    public PhaseContrast(ImagePlus xPCimage, ImagePlus yPCimage, ImagePlus zPCimage)
    {
    	this.setXPCimage(xPCimage);
    	this.setYPCimage(yPCimage);
    	this.setZPCimage(zPCimage);
    	_dots = new LinkedList<Double>();
    	_velocityPower = 1;
    }
    public void writeDots(String fileName)
    {
    	StringBuffer sb = new StringBuffer("dot\n");
		for (Double d: _dots)
		{
			sb.append(d.toString());
			sb.append("\n");
		}
		PrintWriter out = null;
		try 
		{
			FileWriter outFile = new FileWriter(fileName);
			out = new PrintWriter(outFile);
			out.print(sb.toString());
			out.close();
		} 
		catch (IOException e)
		{
			StackTraceElement[] ste = e.getStackTrace();
			IJ.log("Writing file: "+fileName+": "+e.getMessage());
			for (int i=0; i < ste.length; i++)
			{
				IJ.log(ste[i].toString());
			}
			
		}
		finally
		{
			out.close();
		}
		IJ.log("Wrote: "+fileName);
    }
    public double velocityCostFunction(GraphNode n, GraphNode a)
    {
    	double c = 0;
    	double[] velocVec = this.getPCvelocityVector(n.col, n.row, n.z);
    	double[] adjMoveVec = this.adjacentDirectionVector(n, a);
    	double[] crossVec = VectorMath.crossProduct3(velocVec, adjMoveVec);
    	double velocMag = VectorMath.magnitude(velocVec);
    	double crossMag = VectorMath.magnitude(crossVec);
    	
    	double dot = VectorMath.dotProduct3(velocVec, adjMoveVec);
    	dot = Math.sqrt(dot*dot);
    	_dots.add(dot);
    	dot += _velocityDotSigma;
    	
    	// c = Math.pow((crossMag/dot), _velocityPower );
    	c = Math.pow(crossMag/(Math.pow(velocMag, 1)), _velocityPower);
    	return  c;
    }
    
    /** Gets the velocity vector from the 3 Phase Contrast images in X, Y and Z. */
    public double[] getPCvelocityVector(int x, int y, int z)
    {
    	double[] pv = new double[3];
    	pv[0] = _xPC[z][y*_width+x] - PHASE_CONTRAST_ZERO;
    	pv[1] = _yPC[z][y*_width+x] - PHASE_CONTRAST_ZERO;
    	pv[2] = _zPC[z][y*_width+x] - PHASE_CONTRAST_ZERO;
    	return pv;
    }
    /** Creates a vector stepping from the current node to the adjacent using the resolution distances as
     * vector values. 
     * @param node
     * @param adjacent
     * @return
     */
    public double[] adjacentDirectionVector(GraphNode node, GraphNode adjacent)
    {
    	double[] vec = new double[3];
    	double nx = node.col;
    	double ny = node.row;
    	double nz = node.z;
    	double ax = adjacent.col;
    	double ay = adjacent.row;
    	double az = adjacent.z;
    	vec[0] = (ax - nx);
    	vec[1] = (ay - ny);
    	vec[2] = (az - nz);
    	if (_voxelDistance != null)
    	{
    		vec[0] = vec[0] * (double)_voxelDistance.getCol();
    		vec[1] = vec[1] * (double)_voxelDistance.getRow();
    		vec[2] = vec[2] * (double)_voxelDistance.getZ();
    	}
    	return vec;
    }
    
	public static ImagePlus makePCmagImage(String title, ImagePlus xPCimage, ImagePlus yPCimage, ImagePlus zPCimage, ImagePlus segmentationImage)
	{
		ImagePlus image = null;
		
		if (segmentationImage != null && xPCimage != null && yPCimage != null && zPCimage != null)
		{
			int width = segmentationImage.getWidth();
			int height = segmentationImage.getHeight();
			int zSize = segmentationImage.getStackSize();
			double xMax = xPCimage.getDisplayRangeMax();
			double yMax = yPCimage.getDisplayRangeMax();
			double zMax = zPCimage.getDisplayRangeMax();
			IJ.log("x max: "+xMax+ " y max: "+yMax+" zMax: "+zMax);
			short[][] xPCvoxels = ImageProcess.getShortStackVoxels(xPCimage.getImageStack());
			short[][] yPCvoxels = ImageProcess.getShortStackVoxels(yPCimage.getImageStack());
			short[][] zPCvoxels = ImageProcess.getShortStackVoxels(zPCimage.getImageStack());
			float[][] magVoxels = new float[zSize][width*height];
			short[][] segVoxels = ImageProcess.getShortStackVoxels(segmentationImage.getImageStack());
			for (int z=0; z < zSize; z++)
			{
				for (int x=0; x < width; x++)
				{
					for (int y=0; y < height; y++)
					{
						int i = y*width+x;
						double mx = (double)xPCvoxels[z][i];
						double my = (double)yPCvoxels[z][i];
						double mz = (double)zPCvoxels[z][i];
						if (segVoxels[z][i] > 0)
						{
							magVoxels[z][i] = (float)Math.sqrt(mx*mx + my*my + mz*mz);
						}
					}
				}
			}
			image = ImageProcess.makeImage(magVoxels, width, height, title+"PCmag");
		}
		
		
		return image;
	}
	
	/** Make a Phase Contrast magnitude image. */
	public static ImagePlus makePCmagImage(String baseTitle, short[][] xPC, short[][] yPC, short[][] zPC, 
			int width, int height, int zSize)
	{
		ImagePlus image = null;
		
		if (xPC != null && yPC != null && zPC != null)
		{
			float[][] magVoxels = new float[zSize][width*height];
			for (int z=0; z < zSize; z++)
			{
				for (int x=0; x < width; x++)
				{
					for (int y=0; y < height; y++)
					{
						int i = y*width+x;
						double mx = (double)xPC[z][i];
						double my = (double)yPC[z][i];
						double mz = (double)zPC[z][i];
						magVoxels[z][i] = (float)Math.sqrt(mx*mx + my*my + mz*mz);
					}
				}
			}
			image = ImageProcess.makeImage(magVoxels, width, height, baseTitle+"PCmag");
		}
		
		return image;
	}
	public void setXPCimage(ImagePlus cimage) {
		_width = cimage.getWidth();
		_height = cimage.getHeight();
		_zSize = cimage.getStackSize();
		_xPC = ImageProcess.getShortStackVoxels(cimage.getImageStack());
	}
	public void setYPCimage(ImagePlus cimage) {
		_width = cimage.getWidth();
		_height = cimage.getHeight();
		_zSize = cimage.getStackSize();
		_yPC = ImageProcess.getShortStackVoxels(cimage.getImageStack());
	}
	public void setZPCimage(ImagePlus cimage) {
		_width = cimage.getWidth();
		_height = cimage.getHeight();
		_zSize = cimage.getStackSize();
		_zPC = ImageProcess.getShortStackVoxels(cimage.getImageStack());
	}
	public VoxelDistance getVoxelDistance() {
		return _voxelDistance;
	}
	public void setVoxelDistance(VoxelDistance voxelDistance) {
		_voxelDistance = voxelDistance;
	}
	public double getVelocityDotSigma() {
		return _velocityDotSigma;
	}
	public void setVelocityDotSigma(double velocityDotSigma) {
		this._velocityDotSigma = velocityDotSigma;
		IJ.log("PhaseContrast vecloty dot sigma: "+_velocityDotSigma);
	}
	public double getVelocityPower() {
		return _velocityPower;
	}
	public void setVelocityPower(double velocityPower) {
		_velocityPower = velocityPower;
		IJ.log("PhaseContrast velocity power: "+_velocityPower);
	}
}

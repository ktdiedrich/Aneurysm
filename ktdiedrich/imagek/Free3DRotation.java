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
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/** Rotation a 3D image freely on the x, y, and z axises. 
 * Only implemented for short processor types. 
 * @author ktdiedrich@gmail.com 
 * */
public class Free3DRotation 
{
	public static final short X_AXIS = 1;
	public static final short Y_AXIS = 2;
	public static final short Z_AXIS = 3;
	private short[][] _voxels;
	private short[][] _rotVoxels;
	private int _width, _height, _zSize;
	private ImagePlus _image;
	private boolean _isColor;
	private ImageProcessor[] _rcProcs;
	public Free3DRotation(ImagePlus image)
	{
		_isColor = false;
		_image = image;
		_zSize = image.getStackSize();
		_width = image.getWidth();
		_height = image.getHeight();
		ImageStack imageStack = image.getStack();
		ImageProcessor ip1 = imageStack.getProcessor(1); 
		if ( ip1 instanceof ColorProcessor)
		{
			_isColor = true;
			_rcProcs = new ImageProcessor[_zSize];
			for (int i=0; i<_zSize; i++)
			{
				ImageProcessor cp = imageStack.getProcessor(i+1).duplicate();
				_rcProcs[i] = cp;
			}
		}
		else // short image 
		{
			_voxels = ImageProcess.getShortStackVoxels(imageStack);
			_rotVoxels = new short[_zSize][_width*_height];
			for (int z=0; z < _zSize; z++)
			{
				for (int i=0; i < _width*_height; i++)
				{
					_rotVoxels[z][i] = _voxels[z][i];
				}
			}
		}
	}
	
	/** Rotate a 3D image
	 * @param Theta degrees to rotate 0 to 360 and 0 to -360. Converts internally to radians 
	 * @param axis X_AXIS, Y_AXIS, Z_AXIS defined in this class. */
	public void rotate(double theta, short axis)
	{
		// if (theta == 0) return;
		int[] iArray = new int[3];
		int[] colorVox = null;
		// convert to degrees to radians 
		theta = Math.PI/180.0 * theta;
		// IJ.log("radians: "+theta);
		double cosTheta = Math.cos(theta);
		double sinTheta = Math.sin(theta);
		int minX = Integer.MAX_VALUE, maxX = 0, minY=Integer.MAX_VALUE, maxY=0, minZ=Integer.MAX_VALUE, maxZ=0;
		
		// find mins and maxes 
		for (int x=0; x < _width; x++)
		{
			for (int y=0; y < _height; y++)
			{
				for (int z=0; z < _zSize; z++)
				{
					short v = 0;
					if (_isColor)
					{
						v = (short)_rcProcs[z].getPixel(x, y);
					}
					else
					{
						v = _rotVoxels[z][y*_width+x];
					}
					
					
					if (v > 0)
					{
						double dy = (double)y;
						double dx = (double)x;
						double dz = (double)z;
						int xp=x, yp=y, zp=z;
						if (axis == X_AXIS)
						{
							xp = x;
							yp = (int)Math.floor( dy*cosTheta - dz*sinTheta );
							zp = (int)Math.floor( dy*sinTheta + dz*cosTheta );
						}
						else if (axis==Y_AXIS)
						{
							xp = (int)Math.floor(dx*cosTheta + dz*sinTheta);
							yp = y;
							zp = (int)Math.floor( -dx*sinTheta + dz*cosTheta );
						}
						else if (axis==Z_AXIS)
						{
							xp = (int)Math.floor( dx*cosTheta - y*sinTheta);
							yp = (int)Math.floor( dx*sinTheta + dy*cosTheta );
							zp = z; 
						}
						
						int xp1 = xp+1;
						int yp1 = yp+1;
						int zp1 = zp+1;
						if (xp < minX) minX = xp;
						if (yp < minY) minY = yp;
						if (zp < minZ) minZ = zp;
						if (xp1 > maxX) maxX = xp1;
						if (yp1 > maxY) maxY = yp1;
						if (zp > maxZ) maxZ = zp1;
					}
				}
			}
		}
		int xRange = maxX-minX+2;
		int yRange = maxY-minY+2;
		int zRange = maxZ-minZ+2;
		//IJ.log("x range: "+xRange+" y range: "+yRange+" zRange: "+zRange);
		if (xRange > 0 && yRange > 0 && zRange > 0)
		{
			short[][] rotVoxels = null;
			ImageProcessor[] rcProcs = new ImageProcessor[zRange];
			if (_isColor)
			{
				for (int z=0; z < zRange; z++)
				{
					rcProcs[z] = new ColorProcessor(xRange, yRange);
				}
			}
			else
			{
				rotVoxels = new short[zRange][xRange*yRange];
			}
			
			for (int x=0; x < _width; x++)
			{
				for (int y=0; y < _height; y++)
				{
					for (int z=0; z < _zSize; z++)
					{
						short vox = 0;
						if (_isColor)
						{
							vox = (short)_rcProcs[z].getPixel(x, y);
							colorVox = _rcProcs[z].getPixel(x, y, iArray);
						}
						else
						{
							vox = _rotVoxels[z][y*_width+x];
						}
						if (vox > 0)
						{
							double dy = (double)y;
							double dx = (double)x;
							double dz = (double)z;
							int xp=x, yp=y, zp=z;
							if (axis == X_AXIS)
							{
								xp = x;
								yp = (int)Math.floor( dy*cosTheta - dz*sinTheta );
								zp = (int)Math.floor( dy*sinTheta + dz*cosTheta );
							}
							else if (axis==Y_AXIS)
							{
								xp = (int)Math.floor(dx*cosTheta + dz*sinTheta);
								yp = y;
								zp = (int)Math.floor( -dx*sinTheta + dz*cosTheta );
							}
							else if (axis==Z_AXIS)
							{
								xp = (int)Math.floor( dx*cosTheta - y*sinTheta);
								yp = (int)Math.floor( dx*sinTheta + dy*cosTheta );
								zp = z; 
							}
							
							int xp1 = xp+1;
							int yp1 = yp+1;
							int zp1 = zp+1;
							// regrid to mins and maxes.
							int rzp = zp-minZ;
							int ryp = yp-minY;
							int rxp = xp-minX;
							int rzp1 = zp1-minZ;
							int ryp1 = yp1-minY;
							int rxp1 = xp1-minX;
							// TODO filling in missing values due to rounding 
							if (_isColor)
							{
								rcProcs[rzp].putPixel(rxp, ryp, colorVox);
								//rcProcs[rzp1].putPixel(rxp1, ryp1, colorVox);
							}
							else
							{
								rotVoxels[rzp][ryp*xRange+rxp] = vox;
								//rotVoxels[rzp1][ryp1*xRange+rxp1] = vox;
							}
						}
					}
				}
			}
			_width = xRange;
			_height = yRange;
			_zSize = zRange;
			if (_isColor)
			{
				_rcProcs = rcProcs;
			}
			else
			{
				_rotVoxels = rotVoxels;
			}
		}
	}
	public ImagePlus getRotatedImage()
	{
		ImagePlus rotated = null;
		String name = _image.getShortTitle()+"Rotated";
		if (_isColor)
		{
			ImageStack s = new ImageStack(_width, _height);
			for (int i=0; i < _rcProcs.length; i++)
			{
				s.addSlice(""+i, _rcProcs[i]);
			}
			rotated = new ImagePlus(name, s);
		}
		else
		{
			rotated = ImageProcess.makeImage(_rotVoxels, _width, _height, name);
		}
		return rotated;
	}
}

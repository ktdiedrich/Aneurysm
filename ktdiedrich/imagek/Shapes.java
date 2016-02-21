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
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** Generates 3-D shapes for testing 
 * @author ktdiedrich@gmail.com 
 * */
public class Shapes 
{
	public static final short INTENSITY = 164;
	public static final short SEGMENTATION_INT = 50;
	public static final short CENTERLINE_INT = 100;
	private short[][] _voxels;
	private short _intensity;
	public Shapes()
	{
		_intensity = INTENSITY;
	}
	public ImagePlus makeLines(int width, int height, int zSize)
	{
		ImagePlus image = makeImage(width, height, zSize, "lines");
		
		int factor = 10;
		int xLeft = width/factor;
		int yTop = height/factor;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yBottom = height - yTop;
		int zDown = zSize - zUp;
		
		for (int x=xLeft; x < (xRight-xLeft); x++)
		{
			// surroundHollowSphere(x, yTop, zUp, width, 5);
			_voxels[zUp][yTop*width+x] = CENTERLINE_INT;
		}
		 
		for (int y=yTop; y < yBottom/2; y++)
		{
			// surroundHollowSphere(xRight, y, zDown, width, 7);
			_voxels[zDown][y*width+xRight] = CENTERLINE_INT;
		}
		for (int z=zUp; z < zDown/3; z++)
		{
			// surroundHollowSphere(width/2, height/2, z, width, 9);
			_voxels[z][(height/2)*width + (width/2)] = CENTERLINE_INT;
		}
		
		return image;
	}
	
	public ImagePlus makeIntersecting(int height, int width, int zSize, boolean withCenterline)
	{
		int shapeRad = 8;
		String title = "intersecting"+shapeRad+"r";
		if (withCenterline) title = title+"Cent";
		ImagePlus image = makeImage(width, height, zSize, title);
		double helixRad = width*0.30;
		int midW = width/2;
		int midH = height/2;
		int midZ = zSize/2;
		int factor = 15;
		int xLeft = width/factor;
		int yTop = height/factor;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yBottom = height - yTop;
		int zDown = zSize - zUp;
		
		int xPos = (int)(midW+helixRad);
		int yPos = (int)(midH+helixRad);
		int zLen = midZ-zUp;
		
		
		addHelix(width, height, zSize, width*0.30, 1, 25, shapeRad);
		for (int z=zUp; z < midZ; z++)
		{
			surroundHollowSphere(midH, xPos, z, width, shapeRad);	
		}
		for (int z=zUp; z < midZ; z++)
		{
			surroundHollowSphere(midH, xRight, z, width, shapeRad);	
		}
		for (int x=midW; x < xRight; x++)
		{
			surroundHollowSphere(midH, x, midZ-zLen/3, width, shapeRad);
		}
		// centerlines 
		if (withCenterline)
		{
			addHelix(width, height, zSize, width*0.30, 1, 25, 0);
			for (int z=zUp; z < midZ; z++)
			{
				_voxels[z][xPos*width+midH] = CENTERLINE_INT;	
			}
			for (int z=zUp; z < midZ; z++)
			{
				_voxels[z][xRight*width+midH] = CENTERLINE_INT;	
			}
			for (int x=midW; x < xRight; x++)
			{
				_voxels[midZ-zLen/3][x*width+midH] = CENTERLINE_INT;
			}
		}
		
		
		return image;
	}
	public ImagePlus makeMixed(int height, int width, int zSize)
	{
		ImagePlus image = makeImage(width, height, zSize, "mixedShapes");
		
		
		int factor = 10;
		int xLeft = width/factor;
		int yTop = height/factor;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yBottom = height - yTop;
		int zDown = zSize - zUp;
		
		// make 3 rods
		for (int x=xLeft; x < xRight; x++)
		{
			surroundHollowSphere(x, yTop, zUp, width, 3);	
		}
		for (int y=yTop; y < yBottom-yTop; y++)
		{
			surroundHollowSphere(xRight, y, zDown, width, 3);
		}
		for (int z=zUp; z < zDown-zUp; z++)
		{
			surroundHollowSphere(width/2, height/2, z, width, 4);
		}
		int helixRadius = width/(factor);
		if (height < width) helixRadius = height/(factor);
		double alpha = 1;
		double lambda = 5;
		int tubeRadius = 4;
		addHelix(width, height, zSize, helixRadius, alpha, lambda, tubeRadius);
		
		// add sphere representing aneurysms
		// top to bottom 
		surroundSolidSphere(xLeft-1, yTop, zUp, width, 5);
		surroundSolidSphere(xLeft, yTop*2+5, zUp+1, width, 4);
		surroundSolidSphere(xLeft+1, yTop*5-4, zUp-1, width, 6);
		
		// isolated 
		surroundSolidSphere(xRight, height/2, zSize/2, width, 4);
		
		// left to right rod 
		surroundSolidSphere(xLeft+1, yBottom, zDown+1, width, 6);
		surroundSolidSphere(xLeft*3+2, yBottom-2, zDown, width, 5);
		surroundSolidSphere(xLeft*5-3, yBottom+3, zDown, width, 4);
		
		// on center rod 
		surroundSolidSphere(width/2, height/2, zUp+3, width, 6);
		surroundSolidSphere((width/2), height/2+2, zUp*3-2, width, 5);
		surroundSolidSphere((width/2)-2, height/2, zUp*5+3, width, 4);
		
		// on helix
		surroundSolidSphere(width/2, height/2+helixRadius, zSize/2+20, width, 5);
		
		return image;
	}
	public ImagePlus makeRods(int width, int height, int zSize)
	{
		ImagePlus image = makeImage(width, height, zSize, "rods");
		
		int factor = 10;
		int xLeft = width/factor;
		int yTop = height/factor;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yBottom = height - yTop;
		int zDown = zSize - zUp;
		
		for (int x=xLeft; x < (xRight-xLeft); x++)
		{
			surroundHollowSphere(x, yTop, zUp, width, 5);	
		}
		 
		for (int y=yTop; y < yBottom/2; y++)
		{
			surroundHollowSphere(xRight, y, zDown, width, 7);
		}
		for (int z=zUp; z < zDown/3; z++)
		{
			surroundHollowSphere(width/2, height/2, z, width, 9);
		}
		
		return image;
	}
	public ImagePlus makeRod1(int width, int height, int zSize, boolean withCenterline)
	{
		String title = "rod5r";
		if (withCenterline) title = title+"Cent";
		ImagePlus image = makeImage(width, height, zSize, title);
		int factor = 10;
		int xLeft = width/factor;
		int yMid = height/2;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yBottom = height - yMid;
		int zDown = zSize - zUp;
		
		for (int x=xLeft; x < (xRight-xLeft); x++)
		{
			surroundHollowSphere(x, yMid, zUp, width, 5);	
		}
		if (withCenterline)
		{
			for (int x=xLeft; x < (xRight-xLeft); x++)
			{
				_voxels[zUp][yMid*width+x] = CENTERLINE_INT;
			}
			
		}
		
		return image;
	}
	
	public ImagePlus makeT(int width, int height, int zSize, int ts, boolean withCenterline)
	{
		int rad  = 4;
		String title = "tee"+rad+"r"+ts+"t"+width+"w";
		if (withCenterline) title = title+"Cent";
		ImagePlus image = makeImage(width, height, zSize, title);
		int factor = 10;
		int xLeft = width/factor;
		
		int yMid = height/2;
		int zMid = zSize/2;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yTop = height/factor;
		int yBottom = height - (yTop*2);
		
		int zDown = zSize - (3*zUp);
		
		for (int x=xLeft; x < (xRight); x++)
		{
			surroundHollowSphere(x, yMid, zMid, width, rad);	
		}
		int tShift = 4;
		for (int t=tShift; t < ts+tShift; t++)
		{
			for (int y=yMid; y<yBottom; y++)
			{
				surroundHollowSphere(xLeft*(t), y, zMid, width, rad);
			}
		}
		
		if (withCenterline)
		{
			for (int x=xLeft; x < (xRight); x++)
			{
				_voxels[zMid][yMid*width+x] = CENTERLINE_INT;
			}
			for (int t=tShift; t < ts+tShift; t++)
			{
				for (int y=yMid; y<yBottom; y++)
					_voxels[zMid][y*width+(xLeft*t)] = CENTERLINE_INT;
			}
		}
		
		
		return image;
	}
	
	/** Add a coil to the current image. Call makeImage before calling this function.  */
	private final void addHelix(int width, int height, int zSize, double helixRadius, 
			double alpha, double lambda, int tubeRadius)
	{
		int wd2 = width/2;
		int hd2 = height/2;
		int zd2 = zSize/2;
		
		int factor = 20;
		int xLeft = width/factor;
		int yTop = height/factor;
		int zUp = zSize/factor;
		int xRight = width - xLeft;
		int yBottom = height - yTop;
		int zDown = zSize - zUp;
		
		double step = 0.00001;
		// double step = 0.05;
		double x = 0.0, y = 0.0, z = 0.0;

		// tortuosity calculation 
		double t = 0.0, d = 0, L = 0, DFM = 0, maxDFM = 0;
		double x1 = 0, y1 = 0, z1 = 0;
		
		x = helixRadius*Math.cos(alpha*t) + (double)wd2;
		y = helixRadius*Math.sin(alpha*t) + (double)hd2;
		z = lambda*t + (double)zUp;
		x1 = x;
		y1 = y;
		z1 = z;
		double xStart = x, yStart = y, zStart = z;
		IJ.log("Lambda="+lambda+" radius="+helixRadius+" alpha="+alpha+" step="+step);
		// IJ.log("L\\d\\DFM\\n");
		while(z < zDown)
		{
			x = helixRadius*Math.cos(alpha*t) + (double)wd2;
			y = helixRadius*Math.sin(alpha*t) + (double)hd2;
			z = lambda*t + (double)zUp;
			
			int xCoord = (int)Math.round(x);
			int yCoord = (int)Math.round(y);
			int zCoord = (int)Math.round(z);
			if (tubeRadius>0)
				surroundSolidSphere(xCoord, yCoord, zCoord, width, tubeRadius);
			else
				_voxels[zCoord][yCoord*width+xCoord] = CENTERLINE_INT;
			t += step;
			
			d = Shapes.distance(x, xStart, y, yStart, z, zStart);
			double increaseL = Shapes.distance(x, x1, y, y1, z, z1); 
			L += increaseL;
			DFM = L/d;
			// IJ.log(L+"|"+d+"|"+DFM+"|"+increaseL+"\n");
			if (DFM > maxDFM)
			{
				maxDFM = DFM;
			}
			x1 = x;
			y1 = y;
			z1 = z;
		}
		IJ.log("   maxDFM="+maxDFM+" Length="+L+" mm\n");
	}
	public static double distance(double x1, double x2, double y1, double y2, double z1, double z2)
	{
		return Math.sqrt( (x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) + (z2-z1)*(z2-z1) );
	}
	public ImagePlus makeHelix(int width, int height, int zSize, double helixRadius, 
			double alpha, double lambda, int tubeRadius, String name, boolean withCenterline)
	{
		ImagePlus image = makeImage(width, height, zSize, name);
		addHelix(width, height, zSize, helixRadius, alpha, lambda, tubeRadius);
		if (withCenterline)
			addHelix(width, height, zSize, helixRadius, alpha, lambda, 0);
		return image;
	}
	
	
	/** Make a sphere image 
	 * @param width image width
	 * @param height image height
	 * @param zSize image depth
	 * @param r radius of sphere
	 * @param centerX x direction center of the sphere
	 * @param centerY y direction center of the sphere 
	 * @param centerZ z direction center of the sphere 
	 * */
    public ImagePlus makeSphere(int width, int height, int zSize, int r, int centerX, int centerY, int centerZ, String name)
	{
		ImagePlus image = makeImage(width, height, zSize, name);
		surroundSolidSphere(centerX, centerY, centerZ, width, r);
		return image;
	}
	public ImagePlus makeSphere(int width, int height, int zSize, int r)
	{
		ImagePlus image = makeImage(width, height, zSize, "sphere");
		surroundSolidSphere(width/2, height/2, zSize/2, width, r);
		return image;
	}
	/** Call make image first. Creates a fast access _voxels object. */
	private ImagePlus makeImage(int width, int height, int zSize, String name)
	{
		ImageStack stack = new ImageStack(width, height);
		_voxels = new short[zSize][];
		for (int i=1; i <= zSize; i++)
		{
			ImageProcessor proc = new ShortProcessor(width, height);
			stack.addSlice(""+i, proc);
			_voxels[i-1] = (short[])proc.getPixels();
		}
		
		ImagePlus image = new ImagePlus(name, stack);
		
		return image;
	}
	/** Draw a sphere around the point (a, b, c) . 
	 * @param a x coordinate center point.
	 * @param b y coordinate center point.
	 * @param c z coordinate center point.
	 * @param width width of image used for locating voxels in row major order voxel lists. 
	 * @param radius radius of the sphere. 
	 * */
	public void surroundSolidSphere(int a, int b, int c, int width, int radius)
	{
		double q=0.0, p=0.0, x = 0.0, y=0.0, z=0.0;
		double step = 1.0/100.0*Math.PI;
		for (q=0.0; q < Math.PI; q+=step)
		{
			for (p=0.0; p < 2*Math.PI; p+=step)
			{
				for (int r=0; r <=radius; r++)
				{
					x = r * Math.cos(q) * Math.sin(p) + a;
					y = r * Math.sin(q) * Math.sin(p) + b;
					z = r * Math.cos(p) + c;
					int i = (int)Math.round(y) * width + (int)Math.round(x);
					_voxels[(int)Math.round(z)][i] = _intensity;
				}
			}
		}
	}
	/** Make a centered sphere */
	public static short[][][] makeSolidSphereStructure(int radius, short value)
	{
		int width = 2*radius+1;
		int height = width;
		int zSize = height;
		short[][][] voxels = new short[zSize][height][width];
		//short[][] voxels = new short[zSize][height*width];
		
		double q=0.0, p=0.0, x = 0.0, y=0.0, z=0.0;
		double step = 1.0/100.0*Math.PI;
		for (q=0.0; q < Math.PI; q+=step)
		{
			for (p=0.0; p < 2*Math.PI; p+=step)
			{
				for (int r=0; r <=radius; r++)
				{
					x = r * Math.cos(q) * Math.sin(p)+width/2;
					y = r * Math.sin(q) * Math.sin(p)+height/2;
					z = r * Math.cos(p)+zSize/2;
					int ix = (int)Math.round(x);
					int iy = (int)Math.round(y);
					int iz = (int)Math.round(z);
					voxels[iz][iy][ix] = value;
					// voxels[iz][iy*width+ix] =value;
				}
			}
		}
		return voxels;
	}
	public ImagePlus makeSolidCircle(int width, int height, int a, int b, int radius)
	{
		double q=0.0, p=0.0, x = 0.0, y=0.0, xd=0.0, yd=0.0;
		double step = 1.0/100.0*Math.PI;
		ImageProcessor proc = new ShortProcessor(width, height);
		short[] pixels = (short[])proc.getPixels();
		for (q=0.0; q < Math.PI; q+=step)
		{
			for (p=0.0; p < 2*Math.PI; p+=step)
			{
				for (int r=0; r <=radius; r++)
				{
					xd = r * Math.cos(q) * Math.sin(p);
					yd = r * Math.sin(q) * Math.sin(p);
					double delta = Math.sqrt(xd*xd + yd*yd);
					x = xd + a;
					y = yd + b;
					int i = (int)Math.round(y) * width + (int)Math.round(x);
					pixels[i] = (short)Math.round((1/delta)*100 );
				}
			}
		}
		
		ImagePlus im = new ImagePlus("circle", proc);
		return im;
	}
	
	/** Draw a sphere around the point. */
	public void surroundHollowSphere(int a, int b, int c, int width, int radius)
	{
		double q=0.0, p=0.0, x = 0.0, y=0.0, z=0.0;
		double step = 1.0/100.0*Math.PI;
		for (q=0.0; q < Math.PI; q+=step)
		{
			for (p=0.0; p < 2*Math.PI; p+=step)
			{
				
				x = radius * Math.cos(q) * Math.sin(p) + a;
				y = radius * Math.sin(q) * Math.sin(p) + b;
				z = radius * Math.cos(p) + c;
				int i = (int)Math.round(y) * width + (int)Math.round(x);
				_voxels[(int)Math.round(z)][i] = _intensity;
			}
		}
	}
	public short getIntensity() {
		return _intensity;
	}
	public void setIntensity(short intensity) {
		_intensity = intensity;
	}
}

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
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

/** 2D-Convergence filter to amplify round aneurysm like objects in an image. 
 * References: 
 * 1. Suárez-Cuenca, J.J. et al. Application of the iris filter for automatic detection of pulmonary nodules on computed tomography images. Comput. Biol. Med 39, 921-933(2009).
 * 2. Kobatake, H. & Hashimoto, S. Convergence index filter for vector fields. IEEE Trans Image Process 8, 1029-1038(1999).
 * 3. Matsumoto, S. et al. Pulmonary nodule detection in CT images with quantized convergence index filter. Med Image Anal 10, 343-352(2006).
 * @author ktdiedrich@gmail.com */
public class Convergence2D 
{
	public static int RADIUS = 5;
	private short[] _pixels;
	private ImageProcessor _ip;
	private int _radius;
	public Convergence2D(ImageProcessor ip)
	{
		_ip = ip;
		_pixels = (short[])_ip.getPixels();
		_radius = RADIUS;
	}
	
	public void filter()
	{
		int width  = _ip.getWidth();
		int height = _ip.getHeight();
		short[] voxels = (short[])_ip.getPixels();
		ImageProcessor gradientR = _ip.duplicate();
	    short[] rVoxels = (short[])gradientR.getPixels();
	    ImageProcessor gradientC = _ip.duplicate();
	    short[] cVoxels = (short[])gradientC.getPixels();
	    ImageProcessor gradientMag = new FloatProcessor(width, height);
	    float[] magVoxels = (float[])gradientMag.getPixels();
	    ImageProcessor gradientOrientation = new FloatProcessor(width, height);
	    float [] orientVoxels = (float[])gradientOrientation.getPixels();
	    Convolution.convolve3x3Short(gradientC, Convolution.PREWITT_KERNEL_3X3_X);
	    Convolution.convolve3x3Short(gradientR, Convolution.PREWITT_KERNEL_3X3_Y);
	    
	    for (int i=0; i<magVoxels.length; i++)
	    {
	    	magVoxels[i] = (float)Math.sqrt(rVoxels[i]*rVoxels[i] + cVoxels[i]*cVoxels[i] );
	    	orientVoxels[i] = (float)Math.atan2(rVoxels[i], cVoxels[i]);
	    	// orientVoxels[i] = (float)Math.atan2(cVoxels[i], rVoxels[i]);
	    }
	    ImagePlus gradientRIm = new ImagePlus("gradientR", gradientR);
	    ImagePlus gradientCIm = new ImagePlus("gradientC", gradientC);
	    ImagePlus magIm = new ImagePlus("gradientMagnitude", gradientMag);
	    ImagePlus orientIm = new ImagePlus("gradientOrientation", gradientOrientation);
	    
	    gradientRIm.show(); gradientRIm.updateAndDraw();
	    gradientCIm.show(); gradientCIm.updateAndDraw();
	    magIm.show(); magIm.updateAndDraw();
	    orientIm.show(); orientIm.updateAndDraw();
	    ImageProcessor gcProc = new FloatProcessor(width, height);
	    float[] gcVoxels = (float[])gcProc.getPixels();
	    ImageProcessor gProc = new FloatProcessor(width, height);
	    float[] gVoxels = (float[])gProc.getPixels();
	    for (int row=0; row < height; row++)
	    {
	    	for (int col=0; col < width; col++)
	    	{
	    		int i = row*width+col;
	    		if (voxels[i] > 0)
	    		{
	    			// IJ.log("voxel intensity: "+voxels[i]);
	    			List<Position> supportRegion = this.getSupportRegion(width, height, 
	    					col, row, _radius);
	    			double gradientConcentrate = 0;
	    			// IJ.log("Support region size: "+supportRegion.size());
	    			int supportCount = 0;
	    			for (Position q: supportRegion)
	    			{
	    				int qy = q.getRow();
	    				int qx = q.getColumn();
	    				int qi = qy*width + qx;
	    				short Gx = cVoxels[qi];
		    			short Gy = rVoxels[qi];
		    			double magG = Math.sqrt(Gx*Gx + Gy*Gy);
		    			if (magG > 0)
		    			{
		    				double gx = Gx/magG;
		    				double gy = Gy/magG;
		    			
		    				int Cx = qx-col;
		    				int Cy = qy-row;
		    				double magC = Math.sqrt(Cx*Cx + Cy*Cy);
		    				if (magC > 0)
		    				{
		    					double cx = Cx/magC;
		    					double cy = Cy/magC;
		    					double g = gx*cx + gy*cy;
		    			
		    					if (gVoxels[i] == 0)
		    					{
		    						gVoxels[i] = (float)g;
		    						// IJ.log("col="+col+" row="+row+" qx="+qx+" qy="+qy+" Gx="+Gx+" Gy="+Gy+" Cx="+Cx+" Cy="+Cy+" g="+g);
		    					}
		    					gradientConcentrate += g;
		    					supportCount++;
		    				}
		    			}
	    			}
	    			gradientConcentrate = gradientConcentrate/supportCount;
	    			gcVoxels[i] = (float)gradientConcentrate;
	    		}
	    	}
	    }
	    ImagePlus gIm = new ImagePlus("G", gProc);
	    gIm.show();
	    gIm.updateAndDraw();
	    
	    ImagePlus gcIm = new ImagePlus("GC", gcProc);
	    gcIm.show();
	    gcIm.updateAndDraw();
	}
	/** List of Position points in a circle around the point (a, b) . 
	 * @param x x coordinate center point.
	 * @param y y coordinate center point.
	 * @param width width of image used for locating voxels in row major order voxel lists. 
	 * @param radius radius of the sphere. 
	 * */
	public List<Position> getSupportRegion(int width, int height, int x, int y, int radius)
	{
		double q=0.0, p=0.0; 
		int xx = 0, yy=0;
		double step = 1.0/100.0*Math.PI;
		List<Position> surround = new LinkedList<Position>();
		for (q=0.0; q < Math.PI; q+=step)
		{
			for (p=0.0; p < 2*Math.PI; p+=step)
			{
				for (int r=0; r <=radius; r++)
				{
					xx = (int)Math.round(r * Math.cos(q) * Math.sin(p) + x);
					yy = (int)Math.round(r * Math.sin(q) * Math.sin(p) + y);
					int i = yy * width + xx;
					if (_pixels[i] > 0 && (xx != x || yy != y))
					{
						Position pos = new Position(xx, yy);
						surround.add(pos);
					}
				}
			}
		}
		return surround;
	}

	public int getRadius() {
		return _radius;
	}

	public void setRadius(int radius) {
		_radius = radius;
	}
}



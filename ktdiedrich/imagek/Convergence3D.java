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

import ij.*;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/** 3D-Convergence filter to amplify round aneurysm like objects in an image. 
 * References: 
 * 1. Suárez-Cuenca, J.J. et al. Application of the iris filter for automatic detection of pulmonary nodules on computed tomography images. Comput. Biol. Med 39, 921-933(2009).
 * 2. Kobatake, H. & Hashimoto, S. Convergence index filter for vector fields. IEEE Trans Image Process 8, 1029-1038(1999).
 * 3. Matsumoto, S. et al. Pulmonary nodule detection in CT images with quantized convergence index filter. Med Image Anal 10, 343-352(2006).
 * @author ktdiedrich@gmail.com */
public class Convergence3D 
{
	public static int RADIUS = 5;
	private ImagePlus _image;
	short[][] _voxels;
	private int _radius;
	public Convergence3D(ImagePlus image)
	{
		_image = image;
		_voxels = ImageProcess.getShortStackVoxels(image.getImageStack());
	}
	/** Spherical neighborhood around a point  
	 * @return List of Position points in a sphere around the point (a, b, c) . 
	 * @param x x coordinate center point.
	 * @param y y coordinate center point.
	 * @param z z coordinate center point.
	 * @param width width of image used for locating voxels in row major order voxel lists. 
	 * @param radius radius of the sphere. 
	 * */
	public List<Position> getSupportRegion(int x, int y, int z, int width, int radius)
	{
		List<Position> surround = new LinkedList<Position>();
		double q=0.0, p=0.0; 
		int xx = 0, yy=0, zz=0;
		double step = 1.0/100.0*Math.PI;
		for (q=0.0; q < Math.PI; q+=step)
		{
			for (p=0.0; p < 2*Math.PI; p+=step)
			{
				for (int r=0; r <=radius; r++)
				{
					xx = (int)Math.round(r * Math.cos(q) * Math.sin(p) + x);
					yy = (int)Math.round(r * Math.sin(q) * Math.sin(p) + y);
					zz = (int)Math.round(r * Math.cos(p) + z);
					int i = yy * width + xx;
					if (_voxels[zz][i] > 0)
					{
						Position pos = new Position(xx, yy, zz);
						surround.add(pos);
					}
				}
			}
		}
		return surround;
	}
	
	public void filter()
	{
		int width  = _image.getWidth();
		int height = _image.getHeight();
		int zSize = _image.getStackSize();
		ImageStack stack = _image.getImageStack();
		
		short[][] voxels = new short[zSize][]; 
		short[][] rVoxels = new short[zSize][];
		short[][] cVoxels = new short[zSize][];
		short[][] zVoxels = new short[zSize][];
		
		ImageStack gradientCstack = new ImageStack(width, height);
		ImageStack gradientRstack = new ImageStack(width, height);
		ImageStack gradientZstack = new ImageStack(width, height);
		
		ImageStack gcStack = new ImageStack(width, height);
		float[][] gcVoxels = new float[zSize][];
		for (int i=0; i < zSize; i++)
		{
			ImageProcessor proc = stack.getProcessor(i+1);
			voxels[i] = (short[])proc.getPixels();
			       
			ImageProcessor gradientR = proc.duplicate();
			gradientRstack.addSlice(""+i, gradientR);
		    rVoxels[i] = (short[])gradientR.getPixels();
		    Convolution.convolve3x3Short(gradientR, Convolution.PREWITT_KERNEL_3X3_Y);
		    
		    ImageProcessor gradientC = proc.duplicate();
		    gradientCstack.addSlice(""+i, gradientC);
		    cVoxels[i] = (short[])gradientC.getPixels();
		    Convolution.convolve3x3Short(gradientC, Convolution.PREWITT_KERNEL_3X3_X);
		    
		    ImageProcessor gradientZ = proc.duplicate();
		    gradientZstack.addSlice(""+i, gradientZ);
		    zVoxels[i] = (short[])gradientZ.getPixels();
		    
		    ImageProcessor gc = new FloatProcessor(width, height);
		    gcStack.addSlice(""+i, gc);
		    gcVoxels[i] = (float[])gc.getPixels();
		}
		
	    Convolution.convolveZ3x3Short(gradientZstack, Convolution.PREWITT_KERNEL_3X3_Z);
	    
	    
	    ImagePlus gradientRIm = new ImagePlus("gradientR", gradientRstack);
	    ImagePlus gradientCIm = new ImagePlus("gradientC", gradientCstack);
	    ImagePlus gradientZIm = new ImagePlus("gradientZ", gradientZstack);
	    
	    
	    gradientRIm.show(); gradientRIm.updateAndDraw();
	    gradientCIm.show(); gradientCIm.updateAndDraw();
	    gradientZIm.show(); gradientZIm.updateAndDraw();
	       
	    for (int row=0; row < height; row++)
	    {
	    	for (int col=0; col < width; col++)
	    	{
	    		for (int z=0; z < zSize; z++)
	    		{
	    			int i = row*width+col;
		    		if (voxels[z][i] > 0)
		    		{
		    			// IJ.log("voxel intensity: "+voxels[i]);
		    			List<Position> supportRegion = this.getSupportRegion(col, row, 
		    					z, width, _radius);
		    			double gradientConcentrate = 0;
		    			// IJ.log("Support region size: "+supportRegion.size());
		    			int supportCount = 0;
		    			for (Position q: supportRegion)
		    			{
		    				int qy = q.getRow();
		    				int qx = q.getColumn();
		    				int qz = q.getZ();
		    				int qi = qy*width + qx;
		    				short Gx = cVoxels[qz][qi];
			    			short Gy = rVoxels[qz][qi];
			    			short Gz = zVoxels[qz][qi];
			    			double magG = Math.sqrt(Gx*Gx + Gy*Gy + Gz*Gz);
			    			if (magG > 0)
			    			{
			    				double gx = Gx/magG;
			    				double gy = Gy/magG;
			    				double gz = Gz/magG;
			    				int Cx = qx-col;
			    				int Cy = qy-row;
			    				int Cz = qz-z;
			    				double magC = Math.sqrt(Cx*Cx + Cy*Cy + Cz*Cz);
			    				if (magC > 0)
			    				{
			    					double cx = Cx/magC;
			    					double cy = Cy/magC;
			    					double cz = Cz/magC;
			    					double g = gx*cx + gy*cy + gz*cz;
			    			
			    					
			    					gradientConcentrate += g;
			    					supportCount++;
			    				}
			    			}
		    			}
		    			gradientConcentrate = gradientConcentrate/supportCount;
		    			gcVoxels[z][i] = (float)gradientConcentrate;
		    		}
		    	}
	    		}
	    		
	    }
	    
	    
	    ImagePlus gcIm = new ImagePlus("GC", gcStack);
	    gcIm.show();
	    gcIm.updateAndDraw();
	    
	
	}
	public int getRadius() {
		return _radius;
	}
	public void setRadius(int radius) {
		_radius = radius;
	}
	
}

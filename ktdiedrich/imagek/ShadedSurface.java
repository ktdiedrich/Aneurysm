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
import java.util.ListIterator;

import ij.*;
import ij.process.*;
import ij.plugin.ContrastEnhancer;

/** Create a shaded surface rendering of a 3-D image 
 * @author ktdiedrich@gmail.com
 * // TODO position the rotated slides in the middle of the canvas so the view changes are smooth   
 * */
public class ShadedSurface 
{
	public static short BASE_INTENSITY = 100;
	private boolean _depthShade;
	private short _baseIntensity;
	
	private boolean _showSteps;
	public ShadedSurface()
	{
		_baseIntensity = BASE_INTENSITY;
	}
	public ImagePlus shade6(ImagePlus inputImage)
	{
		String title = inputImage.getShortTitle();
		ImageStack inputStack = inputImage.getImageStack();
		int height = inputImage.getHeight();
		int width = inputImage.getWidth();
		int zSize = inputStack.getSize();
		// make largest canvas 
		int dim = height;
		if (width > dim) dim = width;
		if (zSize > dim) dim = zSize;
		ImageProcessor inputProc1 = inputStack.getProcessor(1);
		
		ImageStack colorStack = null;
		if (inputProc1 instanceof ColorProcessor)
		{
			//_colorStack = new ImageStack(width, height);
			colorStack = new ImageStack(dim, dim);
		}
		
		ImageStack surfDepths = surfaceDepths6(inputStack, colorStack, dim, dim);
		if (_showSteps)
		{
			ImagePlus depthImages = new ImagePlus(title+"Depths", surfDepths);
			depthImages.show();
			depthImages.updateAndDraw();
		}
		return this.shadeDepths(surfDepths, colorStack, title, dim, dim);
	}
	
	public ImagePlus shadeDepths(ImageStack surfDepths, ImageStack colorStack, String title, 
			int canvasWidth, int canvasHeight)
	{
		ImageStack shadedStack = new ImageStack(canvasWidth, canvasHeight);
		ContrastEnhancer ce = new ContrastEnhancer();
		for (int i=1; i <= surfDepths.getSize(); i++)
		{
			//IJ.log("Shade depth: "+i);
			ImageProcessor depthProc = surfDepths.getProcessor(i);
			ImageProcessor colorProc = null;
			if (colorStack != null) colorProc = colorStack.getProcessor(i); 
			ImageProcessor shadedProc = getShadedView(depthProc, colorProc);
			
			ce.equalize(shadedProc);
			shadedStack.addSlice(surfDepths.getSliceLabel(i), shadedProc);
		}
        ImagePlus shaded = new ImagePlus(title+"Shaded", shadedStack);
		return shaded;
	}
	/** Make a shaded surface image from a surface depth image view. */
	public ImageProcessor getShadedView(ImageProcessor surfDepthProc, 
			ImageProcessor colorProc)
	{
		int height = surfDepthProc.getHeight();
		int width = surfDepthProc.getWidth();
		short[] surfDepthPixels = (short[])surfDepthProc.getPixels();
		
        ImageProcessor gradient = surfDepthProc.duplicate();
        gradient.findEdges();
        
        short[] gradPixels = (short[])gradient.getPixels();
        
        ImageProcessor shadedProc = new ShortProcessor(width, height);
        
        short[] shadedPixels = (short[])shadedProc.getPixels();
        double max = 0, min = Double.MAX_VALUE;
        for (int x=0; x < width; x++)
        {
        	for (int y = 0; y < height; y++)
        	{
        		short d = surfDepthPixels[y*width+x];
        		if (d > 0)
        		{
        			short g = gradPixels[y*width+x];
        			double zn = Math.pow((1 + g*g), -0.5);
        			if (!_depthShade)
        				d = _baseIntensity;
        			double v = Math.round(d*zn);
        			if (v < min) min = v;
        			if (v > max) max = v;
        			shadedPixels[y*width+x] = (short)(v);
        		}
        	}
        }
        if ( colorProc != null)
        {
        	ImageProcessor shadedColor = new ColorProcessor(width, height);
        	int[] voxelTmp = new int[3];
        	for (int x=0; x < width; x++)
        	{
        		for (int y=0; y < height; y++)
        		{
        			short v = shadedPixels[y*width+x];
        			if (v > 0)
        			{
        				double normal = v/max;
        				// multiply normal by each color channel
        				int[] cv = colorProc.getPixel(x, y, voxelTmp);
        				cv[0] = (short)Math.round(cv[0]*normal);
        				cv[1] = (short)Math.round(cv[1]*normal);
        				cv[2] = (short)Math.round(cv[2]*normal);
        				shadedColor.putPixel(x, y, cv);
        			}
        		}
        	}
        	return shadedColor;
        }
        
        return shadedProc;
	}
	
	/** Add depth image in Z direction
	 * @param canvasWidth must be equal or greater than inputStack width
	 * @param canvasHeight must be equal or greater than inputStack height 
	 * */
	private void zAxisDepth(final ImageStack inputStack, final short[][] voxels, ImageStack depthStack, ImageStack colorStack, 
			int canvasWidth, int canvasHeight)
	{
		int height = inputStack.getHeight();
		int width = inputStack.getWidth();
		int zSize = inputStack.getSize();
		assert canvasWidth >= width;
		assert canvasHeight >= height;
		
		int shiftWidth = 0;
		if (canvasWidth > width) shiftWidth = (int)Math.round((double)(canvasWidth - width)/2.0);
		int shiftHeight = 0;
		if (canvasHeight > height) shiftHeight = (int)Math.round((double)(canvasHeight - height)/2.0);
		
		int[] voxelTmp = new int[3];
		// Z axis 
		String titleA = "Z_axis_from_top";
		String titleB = "Z_axis_from_bottom";
		short[] surfaceA = new short[canvasWidth*canvasHeight];
		short[] surfaceB = new short[canvasWidth*canvasHeight];
		ImageProcessor cpA = null;
		ImageProcessor cpB = null;
		if (colorStack != null) 
		{
			cpA = new ColorProcessor(canvasWidth, canvasHeight);
			cpB = new ColorProcessor(canvasWidth, canvasHeight);
		}
		// IJ.log("Shaded surface: canvas max index: W: "+canvasWidth+" H: "+canvasHeight+" = "+
		//		canvasWidth*canvasHeight);
		for (short zA=0, zB=(short)(zSize-1); zA < zSize; zA++, zB--)
		{
			for (short x=0; x < width; x++)
			{
				for (short y=0; y < height; y++)
				{
					int canvasX = x+shiftWidth;
					int canvasY = y+shiftHeight;
					int canvasI = (canvasY)*canvasWidth+(canvasX);
					/*
					if (canvasI > canvasWidth*canvasHeight)
					{
						IJ.log("Shaded surface Canvas index out of range: X: "+canvasX+" Y: "+canvasY+
								" = "+canvasI);
					}
					*/
					if (canvasI < canvasWidth*canvasHeight && surfaceA[canvasI] == 0)
					{
						short valA = voxels[zA][y*width+x];
						if (valA > 0)
							surfaceA[canvasI] = (short)(zSize - zA + 1);
						if (colorStack != null)
						{
							int[] vox = inputStack.getProcessor(zA+1).getPixel(x, y, voxelTmp);
							cpA.putPixel(canvasX, canvasY, vox);
						}
					}
					if (canvasI < canvasWidth*canvasHeight && surfaceB[canvasI] == 0)
					{
						short valB = voxels[zB][y*width+x];
						if (valB > 0)
							surfaceB[canvasI] = (short)(zB + 1);
						if (colorStack != null)
						{
							int[] vox = inputStack.getProcessor(zB+1).getPixel(x, y, voxelTmp);
							cpB.putPixel(canvasX, canvasY, vox);
						}
					}
				}
			}
		}
		ImageProcessor surfProcA = new ShortProcessor(canvasWidth, canvasHeight);
		surfProcA.setPixels(surfaceA);
        depthStack.addSlice(titleA, surfProcA);
        
        ImageProcessor surfProcB = new ShortProcessor(canvasWidth, canvasHeight);
        surfProcB.setPixels(surfaceB);
        // add B surfaces 
        depthStack.addSlice(titleB, surfProcB);
        if (colorStack != null) 
        {
        	colorStack.addSlice(titleA, cpA);
        	colorStack.addSlice(titleB, cpB);
        }
        
	}
	private void xAxisDepth(ImageStack inputStack, short[][] voxels, ImageStack depthStack, ImageStack colorStack,
			int canvasWidth, int canvasHeight)
	{
		int height = inputStack.getHeight();
		int width = inputStack.getWidth();
		int zSize = inputStack.getSize();
		int[] voxelTmp = new int[3];
		// X axis 
        String titleA = "X_axis_from_left";
		String titleB = "X_axis_from_right";
		short[] surfaceA = new short[canvasWidth*canvasHeight];
		short[] surfaceB = new short[canvasWidth*canvasHeight];
		ColorProcessor cpA = null;
		ColorProcessor cpB = null;
		if (colorStack != null) 
		{
			cpA = new ColorProcessor(canvasWidth, canvasHeight);
			cpB = new ColorProcessor(canvasWidth, canvasHeight);
		}
        
		for (int z = 0; z < zSize; z++) 
        {
        	for (int xA=0, xB=(width-1); xA < width; xA++, xB--) 
            {
                for (int y = 0; y < height; y++) 
                {   
                	int i = z*canvasWidth + y;
                    if (surfaceA[i] == 0)
                    {
						short valA = voxels[z][y*width+xA];
						if (valA > 0)
						{		
							surfaceA[i] = (short)(width - xA + 1);
						}
						if (colorStack != null)
						{
							int[] vox = inputStack.getProcessor(z+1).getPixel(xA, y, voxelTmp);
							cpA.putPixel(y, z, vox);
						}
					}
                    if (surfaceB[i] == 0)
					{
						short valB = voxels[z][y*width+xB];
						if (valB > 0)
							surfaceB[i] = (short)(xB + 1);
						if (colorStack != null)
						{
							int[] vox = inputStack.getProcessor(z+1).getPixel(xB, y, voxelTmp);
							cpB.putPixel(y, z, vox);
						}
					}
                }
            }
        }
		ImageProcessor surfProcA = new ShortProcessor(canvasWidth, canvasHeight);
		surfProcA.setPixels(surfaceA);
        depthStack.addSlice(titleA, surfProcA);
        
        ImageProcessor surfProcB = new ShortProcessor(canvasWidth, canvasHeight);
        surfProcB.setPixels(surfaceB);
        depthStack.addSlice(titleB, surfProcB);
        if (colorStack != null) 
        {
        	colorStack.addSlice(titleA, cpA);
        	colorStack.addSlice(titleB, cpB);
        }
	}
	private void yAxisDepth(ImageStack inputStack, short[][] voxels, ImageStack depthStack, ImageStack colorStack,
			int canvasWidth, int canvasHeight)
	{
		int height = inputStack.getHeight();
		int width = inputStack.getWidth();
		int zSize = inputStack.getSize();
		int[] voxelTmp = new int[3];
		// Y axis 
        String titleA = "Y_axis_from_front";
		String titleB = "Y_axis_from_back";
		short[] surfaceA = new short[canvasWidth*canvasHeight];
		short[] surfaceB = new short[canvasWidth*canvasHeight];
		ColorProcessor cpA = null;
		ColorProcessor cpB = null;
		if (colorStack != null) 
		{
			cpA = new ColorProcessor(canvasWidth, canvasHeight);
			cpB = new ColorProcessor(canvasWidth, canvasHeight);
		}
        
		for (int z = 0; z < zSize; z++) 
        {
        	for (int x=0; x < width; x++) 
            {
                for (int yA = 0, yB=(height-1); yA < height; yA++, yB--) 
                {
                    int i = z*canvasWidth + x;
                    
                    if (surfaceA[i] == 0)
                    {
						short valA = voxels[z][yA*width+x];
						if (valA > 0)
						{		
							surfaceA[i] = (short)(height - yA + 1);
						}
						if (colorStack != null)
						{
							int[] vox = inputStack.getProcessor(z+1).getPixel(x, yA, voxelTmp);
							cpA.putPixel(x, z, vox);
						}
					}
                    if (surfaceB[i] == 0)
					{
						short valB = voxels[z][yB*width+x];
						if (valB > 0)
						{
							surfaceB[i] = (short)(yB + 1);
						}
						if (colorStack != null)
						{
							int[] vox = inputStack.getProcessor(z+1).getPixel(x, yB, voxelTmp);
							cpB.putPixel(x, z, vox);
						}
					}
                }
            }
        }
		ImageProcessor surfProcA = new ShortProcessor(canvasWidth, canvasHeight);
		surfProcA.setPixels(surfaceA);
        depthStack.addSlice(titleA, surfProcA);
        
        ImageProcessor surfProcB = new ShortProcessor(canvasWidth, canvasHeight);
        surfProcB.setPixels(surfaceB);
        depthStack.addSlice(titleB, surfProcB);
        if (colorStack != null) 
        {
        	colorStack.addSlice(titleA, cpA);
        	colorStack.addSlice(titleB, cpB);
        }
	}
	/** Finds the depth of the surface of the 3-D volume consisting of the depth of first non-zero voxels 
	 * encountered from the point of view in six faces */
	public ImageStack surfaceDepths6(ImageStack inputStack, ImageStack colorStack, 
			int canvasWidth, int canvasHeight)
	{
		
		ImageStack depthStack = new ImageStack(canvasWidth, canvasHeight);
		ImageStack intensityStack = inputStack;
		if (colorStack != null)
		{
			intensityStack = this.makeIntensityStack(inputStack);
		}
		short[][] voxels = ImageProcess.getShortStackVoxels(intensityStack);
		
		this.xAxisDepth(inputStack, voxels, depthStack, colorStack, canvasWidth, canvasHeight);
		this.yAxisDepth(inputStack, voxels, depthStack, colorStack, canvasWidth, canvasHeight);
		this.zAxisDepth(inputStack, voxels, depthStack, colorStack, canvasWidth, canvasHeight);
		
		return depthStack;
	}
	/** Converts an RBG color image stack into a short intensity image stack. */
	private ImageStack makeIntensityStack(ImageStack inputStack)
	{
		ImageStack intensityStack = new ImageStack(inputStack.getWidth(), inputStack.getHeight());
		for (int i=1; i <=inputStack.getSize(); i++)
		{
			intensityStack.addSlice(""+i, inputStack.getProcessor(i).convertToShort(true));
		}
		return intensityStack;
	}
	public ImagePlus rotateShadedSurface(ImagePlus inputImage, double degrees, 
			short axis)
	{
		String title = inputImage.getShortTitle();
		ImageStack inputStack = inputImage.getImageStack();
		int height = inputImage.getHeight();
		int width = inputImage.getWidth();
		int zSize = inputImage.getStackSize();
		
		// make largest canvas 
		// TODO check canvas size
		int dim = height;
		if (width > dim) dim = width;
		if (zSize > dim) dim = zSize;
		// _dim *= 2;
		
		int canvasWidth = dim;
		int canvasHeight = dim;
		ImageProcessor proc1 = inputStack.getProcessor(1);
		ImageStack colorStack = null;
		if (proc1 instanceof ColorProcessor)
		{
			colorStack = new ImageStack(canvasHeight, canvasWidth);
		}
		
		ImageStack depthStack = new ImageStack(canvasHeight, canvasWidth);
		
		// center all rotations on the canvas, calculate canvas width and height 
		
		for (double d=0; d < 180; d+=degrees)
		{
			// IJ.log("Rotation: "+d+" degrees.");
			// TODO only works for 1 axis at a time
			Free3DRotation rot = new Free3DRotation(inputImage);
			
			rot.rotate(d, axis);
			ImagePlus rotIm = rot.getRotatedImage();	
			
			if (rotIm != null)
			{
				ImageStack intensityStack = rotIm.getImageStack();
				if (colorStack != null)
				{
					intensityStack = this.makeIntensityStack(intensityStack);
				}
				short[][] intensityVoxels = ImageProcess.getShortStackVoxels(intensityStack);
				this.zAxisDepth(rotIm.getImageStack(), intensityVoxels, depthStack, colorStack, 
						canvasHeight, canvasWidth);
			}
		}
		// resort depthStack  
		ImagePlus shadeIm =  this.shadeDepths(depthStack, colorStack, title, canvasHeight, canvasWidth);
		ImageStack shadeImStack  = shadeIm.getImageStack();
		ImageStack sortedStack = new ImageStack(canvasWidth, canvasHeight);
		LinkedList<ImageProcessor> backSideDepths = new LinkedList<ImageProcessor>();
		int c=1;
		double d = 0.0;
		for (int i=0; i<shadeImStack.getSize(); i++)
		{
			ImageProcessor ip = shadeImStack.getProcessor(i+1);
			if ((i%2)==0 )
			{
				sortedStack.addSlice(""+c, ip);
				c++;
			}
			else
			{
				backSideDepths.add(ip);
			}
			d += degrees;
		}
		
		String axisName = "";
		if (axis == Free3DRotation.Y_AXIS)
		{
			axisName = "Y";
			for (ImageProcessor ip: backSideDepths)
			{
				ip.flipHorizontal();
				sortedStack.addSlice(""+c, ip);
				c++;
			}
		}
		else if (axis == Free3DRotation.X_AXIS)
		{
			axisName = "X";
			for (ImageProcessor ip: backSideDepths)
			{
				ip.flipVertical();
				sortedStack.addSlice(""+c, ip);
				c++;
			}
		}
		ImagePlus sortedIm = new ImagePlus(shadeIm.getShortTitle()+axisName, sortedStack);
		return  sortedIm;
	}
	public boolean isDepthShade() {
		return _depthShade;
	}
	public void setDepthShade(boolean depthShade) {
		_depthShade = depthShade;
	}
	public short getBaseIntensity() {
		return _baseIntensity;
	}
	public void setBaseIntensity(short baseIntensity) {
		_baseIntensity = baseIntensity;
	}
	public boolean isShowSteps() {
		return _showSteps;
	}
	public void setShowSteps(boolean showSteps) {
		_showSteps = showSteps;
	}
}

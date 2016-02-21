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
import ij.process.*;

import java.util.List;
import java.util.LinkedList;

/**Maximum Intensity Projection.
 * @author Karl Diedrich <ktdiedrich@gmail.com> */
public class MIP
{
    public static short Z_AXIS = 0;
    public static short X_AXIS = 1;
    public static short Y_AXIS = 2;
    public static short XYZ_AXIS = 3;
    protected MIP()
    {

    }

    /** short image Maximum intensity projection along the axis */
    public static ImagePlus createShortMIP(ImagePlus image, short axis)
    {
    	ImageStack stack = image.getStack();
    	String title = image.getShortTitle();
        int zSize = stack.getSize();
        int height = stack.getHeight();
        int width = stack.getWidth();

        short[] mipZData = null;
        short[] mipXData = null;
        short[] mipYData = null;

        if (axis == Z_AXIS || axis == XYZ_AXIS)
            mipZData = new short[height*width];

        short[][] sliceData = new short[zSize][];

        ImageProcessor mipZProc = null;
        ImageProcessor mipXProc = null;
        ImageProcessor mipYProc = null;

        for (int z=1; z <= zSize; z++)
        {
            sliceData[z-1] = (short[])stack.getPixels(z);
            if (axis == Z_AXIS || axis == XYZ_AXIS )
            {
                for (int j=0; j<sliceData[z-1].length; j++)
                {
                	int p = 0xFFFF & sliceData[z-1][j]; 
                    if ( p > mipZData[j])
                        mipZData[j] = (short)(0xFFFF & p);
                }
            }
        }
        if ( axis == Z_AXIS || axis == XYZ_AXIS) {
            mipZProc = new ShortProcessor(width, height, mipZData, null);
        }

        if (axis == X_AXIS || axis == XYZ_AXIS) 
        {
            mipXData = new short[zSize * height];
            for (int x=0; x < width; x++) 
            {
                for (int z = 0; z < zSize; z++) 
                {
                    for (int y = 0; y < height; y++) 
                    {
                    	// TODO rotate X projection vertically 
                        int i = z*height + y;
                        int p = 0xFFFF & sliceData[z][y*width + x];
                        if ( p > mipXData[i])
                            mipXData[i] = (short)(0xFFFF & p);
                    }
                }
            }
            mipXProc = new ShortProcessor( height, zSize, mipXData, null);
            // mipXProc = new ShortProcessor( zSize, height, mipXData, null);
        }
        if (axis == Y_AXIS || axis == XYZ_AXIS) 
        {
            mipYData = new short[zSize * width];
            for (int y=0; y < height; y++) 
            {
                for (int z = 0 ; z < zSize; z++) 
                {
                    for (int x = 0; x < width; x++) 
                    {
                        int  i = z*width +x;
                        int p = 0xFFFF & sliceData[z][y*width + x]; 
                        if ( p > mipYData[i])
                            mipYData[i] = (short)(0xFFFF & p);
                    }
                }
            }
            mipYProc = new ShortProcessor(width, zSize, mipYData, null);
        }
        ImagePlus mip = null; // new LinkedList<ImagePlus>();
        if (mipZProc != null)
            mip = (new ImagePlus(title+"Z_MIP", mipZProc));
        if (mipXProc != null)
            mip = (new ImagePlus(title+"X_MIP", mipXProc));
        if (mipYProc != null)
            mip = (new ImagePlus(title+"Y_MIP", mipYProc));
        return mip;
    }

    /** Byte image Maximum intensity projection along the axis */
    public static ImagePlus createByteMIP(ImagePlus image, short axis)
    {
    	ImageStack stack = image.getStack();
    	String title = image.getShortTitle();
        int zSize = stack.getSize();
        int height = stack.getHeight();
        int width = stack.getWidth();

        byte[] mipZData = null;
        byte[] mipXData = null;
        byte[] mipYData = null;

        if (axis == Z_AXIS || axis == XYZ_AXIS)
            mipZData = new byte[height*width];

        byte[][] sliceData = new byte[zSize][];

        ImageProcessor mipZProc = null;
        ImageProcessor mipXProc = null;
        ImageProcessor mipYProc = null;

        for (int z=1; z <= zSize; z++)
        {
            sliceData[z-1] = (byte[])stack.getPixels(z);
            if (axis == Z_AXIS || axis == XYZ_AXIS )
            {
                for (int j=0; j<sliceData[z-1].length; j++)
                {
                	int p = 0xFF & sliceData[z-1][j]; 
                    if ( p > mipZData[j])
                        mipZData[j] = (byte)(0xFF & p);
                }
            }
        }
        if ( axis == Z_AXIS || axis == XYZ_AXIS) {
            mipZProc = new ByteProcessor(width, height, mipZData, null);
        }

        if (axis == X_AXIS || axis == XYZ_AXIS) 
        {
            mipXData = new byte[zSize * height];
            for (int x=0; x < width; x++) 
            {
                for (int z = 0; z < zSize; z++) 
                {
                    for (int y = 0; y < height; y++) 
                    {
                        int i = z*height + y;
                        int p = 0xFF & sliceData[z][y*width + x]; 
                        if ( p > mipXData[i])
                            mipXData[i] = (byte)(0xFF & p);
                    }
                }
            }
            mipXProc = new ByteProcessor(height, zSize, mipXData, null);
        }
        if (axis == Y_AXIS || axis == XYZ_AXIS) 
        {
            mipYData = new byte[zSize * width];
            for (int y=0; y < height; y++) 
            {
                for (int z = 0 ; z < zSize; z++) 
                {
                    for (int x = 0; x < width; x++) 
                    {
                        int  i = z*width +x;
                        int p  = 0xFF & sliceData[z][y*width + x]; 
                        if (p > mipYData[i])
                            mipYData[i] = (byte)(0xFF & p);
                    }
                }
            }
            mipYProc = new ByteProcessor(width, zSize, mipYData, null);
        }
        ImagePlus mip = null; // new LinkedList<ImagePlus>();
        if (mipZProc != null)
            mip = (new ImagePlus(title+"Z_MIP", mipZProc));
        if (mipXProc != null)
            mip = (new ImagePlus(title+"X_MIP", mipXProc));
        if (mipYProc != null)
            mip = (new ImagePlus(title+"Y_MIP", mipYProc));
        return mip;
    }


    /** Float image Maximum intensity projection along the axis */
    public static ImagePlus createFloatMIP(ImagePlus image, short axis)
    {
    	ImageStack stack = image.getStack();
    	String title = image.getShortTitle();
        int zSize = stack.getSize();
        int height = stack.getHeight();
        int width = stack.getWidth();

        float[] mipZData = null;
        float[] mipXData = null;
        float[] mipYData = null;

        if (axis == Z_AXIS || axis == XYZ_AXIS)
            mipZData = new float[height*width];

        float[][] sliceData = new float[zSize][];

        ImageProcessor mipZProc = null;
        ImageProcessor mipXProc = null;
        ImageProcessor mipYProc = null;

        for (int z=1; z <= zSize; z++)
        {
            sliceData[z-1] = (float[])stack.getPixels(z);
            if (axis == Z_AXIS || axis == XYZ_AXIS )
            {
                for (int j=0; j<sliceData[z-1].length; j++)
                {
                    if (sliceData[z-1][j] > mipZData[j])
                        mipZData[j] = sliceData[z-1][j];
                }
            }
        }
        if ( axis == Z_AXIS || axis == XYZ_AXIS) {
            mipZProc = new FloatProcessor(width, height, mipZData, null);
        }

        if (axis == X_AXIS || axis == XYZ_AXIS) 
        {
            mipXData = new float[zSize * height];
            for (int x=0; x < width; x++) 
            {
                for (int z = 0; z < zSize; z++) 
                {
                    for (int y = 0; y < height; y++) 
                    {
                        int i = z*height + y;
                        if (sliceData[z][y*width + x] > mipXData[i])
                            mipXData[i] = sliceData[z][y*width + x];
                    }
                }
            }
            mipXProc = new FloatProcessor(height, zSize, mipXData, null);
        }
        if (axis == Y_AXIS || axis == XYZ_AXIS) 
        {
            mipYData = new float[zSize * width];
            for (int y=0; y < height; y++) 
            {
                for (int z = 0 ; z < zSize; z++) 
                {
                    for (int x = 0; x < width; x++) 
                    {
                        int  i = z*width +x;
                        if (sliceData[z][y*width + x] > mipYData[i])
                            mipYData[i] = sliceData[z][y*width + x];
                    }
                }
            }
            mipYProc = new FloatProcessor(width, zSize, mipYData, null);
        }
        ImagePlus mip = null; // new LinkedList<ImagePlus>();
        if (mipZProc != null)
            mip = (new ImagePlus(title+"Z_MIP", mipZProc));
        if (mipXProc != null)
            mip = (new ImagePlus(title+"X_MIP", mipXProc));
        if (mipYProc != null)
            mip = (new ImagePlus(title+"Y_MIP", mipYProc));
        return mip;
    }

    /** Color image Maximum intensity projection along the axis */
    public static ImagePlus createColorMIP(ImagePlus image, short axis)
    {
    	ImageStack stack = image.getStack();
    	String title = image.getShortTitle();
    	int zSize = stack.getSize();
        int height = stack.getHeight();
        int width = stack.getWidth();

        ColorProcessor zProc = null;
        ColorProcessor xProc = null;
        ColorProcessor yProc = null;
        
        if (axis == Z_AXIS || axis == XYZ_AXIS)
        {   
        	zProc = new ColorProcessor(width, height);
            for (int z=0; z < zSize; z++) 
            {
            	ImageProcessor sliceProc = stack.getProcessor(z+1);
                for (int x = 0; x < width; x++) 
                {
                    for (int y = 0; y < height; y++) 
                    {
                        // int i = y*width + x;
                        float sliceVal = sliceProc.getPixelValue(x, y);
                        float mipVal = zProc.getPixelValue(x, y);
                        if (sliceVal > mipVal)
                        {
                        	int[] rgb = sliceProc.getPixel(x, y, new int[3]);
                        	zProc.putPixel(x, y, rgb);
                        }
                    }
                }
            }
        }
        
        
        if (axis == X_AXIS || axis == XYZ_AXIS) 
        {
            xProc = new ColorProcessor(zSize, height);
            for (int z = 0; z < zSize; z++) 
            {
            	ImageProcessor sliceProc = stack.getProcessor(z+1);
            	for (int x=0; x < width; x++) 
                {
                    for (int y = 0; y < height; y++) 
                    {
                        float sliceVal = sliceProc.getPixelValue(x, y);
                        float mipVal = xProc.getPixelValue(z, y);
                        if (sliceVal > mipVal)
                        {
                        	int[] rgb = sliceProc.getPixel(x, y, new int[3]);
                        	xProc.putPixel(z, y, rgb);
                        }
                    }
                }
            }
        }
        
        if (axis == Y_AXIS || axis == XYZ_AXIS) 
        {
        	yProc = new ColorProcessor(width, zSize);
            for (int z = 0 ; z < zSize; z++)
            {
            	ImageProcessor sliceProc = stack.getProcessor(z+1);
            	for (int y=0; y < height; y++) 
                 
                {
                    for (int x = 0; x < width; x++) 
                    {
                        float sliceVal = sliceProc.getPixelValue(x, y);
                        float mipVal = yProc.getPixelValue(x, z);
                        if (sliceVal > mipVal)
                        {
                        	int[] rgb = sliceProc.getPixel(x, y, new int[3]);
                        	yProc.putPixel(x, z, rgb);
                        }
                    }
                }
            }
        }
        ImagePlus mip = null; // new LinkedList<ImagePlus>();
        if (zProc != null)
            mip = (new ImagePlus(title+"Z_MIP", zProc));
        
        if (xProc != null)
            mip = (new ImagePlus(title+"X_MIP", xProc));
        if (yProc != null)
            mip = (new ImagePlus(title+"Y_MIP", yProc));
        
        return mip;
    }
    public static ImagePlus rotateMIP(ImagePlus inputImage, double degrees, short axis)
	{
		String title = inputImage.getShortTitle()+"MIPs";
		ImageStack inputStack = inputImage.getImageStack();
		int height = inputImage.getHeight();
		int width = inputImage.getWidth();
		int zSize = inputImage.getStackSize();
		
		// make largest canvas 
		// TODO check canvas size
		int dim = height;
		if (width > dim) dim = width;
		if (zSize > dim) dim = zSize;
		
		int canvasWidth = dim;
		int canvasHeight = dim;
		ImageProcessor proc1 = inputStack.getProcessor(1);
		ImageStack mipStack = new ImageStack(canvasWidth, canvasHeight);
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
				ImagePlus mip = null;
		    	if (proc1 instanceof ShortProcessor)
		    	    mip = MIP.createShortMIP(rotIm, MIP.Z_AXIS);
		    	if (proc1 instanceof FloatProcessor)
		    	    mip = MIP.createFloatMIP(rotIm, MIP.Z_AXIS);
		    	if (proc1 instanceof ByteProcessor)
		    	    mip = MIP.createByteMIP(rotIm, MIP.Z_AXIS);
		    	if (proc1 instanceof ColorProcessor)
		    	    mip = MIP.createColorMIP(rotIm, MIP.Z_AXIS);
		    	if (mip != null)
		    	{
		    		int w = mip.getWidth();
		    		int h = mip.getHeight();
		    		int shiftWidth = 0;
		    		if (canvasWidth > w) shiftWidth = (int)Math.round((double)(canvasWidth - w)/2.0);
		    		int shiftHeight = 0;
		    		if (canvasHeight > h) shiftHeight = (int)Math.round((double)(canvasHeight - h)/2.0);
		    		int maxCanvasI = canvasWidth*canvasHeight;
		    		short[] regrid = new short[maxCanvasI];
		    		ImageProcessor mipProc = mip.getProcessor();
		    		short[] mipPixels = (short[])mipProc.getPixels();
		    		for (int x=0; x < w; x++)
		    		{
		    			for (int y=0; y < h; y++)
		    			{
		    				int canvasX = x + shiftWidth;
		    				int canvasY = y + shiftHeight;
		    				int canvasI = canvasY*canvasWidth + canvasX; 
		    				if (canvasI < maxCanvasI)
		    				{
		    					regrid[canvasI] = mipPixels[y*w+x];
		    				}
		    			}
		    		}
		    		ImageProcessor regridMipProc = new ShortProcessor(canvasWidth, canvasHeight);
		    		regridMipProc.setPixels(regrid);
		    		mipStack.addSlice(d+"_degrees", regridMipProc);
		    	}
			}
		}
		ImagePlus mipIm = new ImagePlus(title, mipStack);
		return mipIm;
	}
    
}

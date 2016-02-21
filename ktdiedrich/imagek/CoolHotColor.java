package ktdiedrich.imagek;

import ij.*;
import ij.plugin.ContrastEnhancer;
import ij.process.*;

/** Colors an image from blue to red based on increasing value. 
 * @author ktdiedrich@gmail.com 
 * */
public class CoolHotColor
{
    private ImagePlus _image;
    /** @param image short image stack image. */
    public CoolHotColor(ImagePlus image)
    {
    	this(ImageProcess.getShortStackVoxels(
        		image.getImageStack()), image.getWidth(), image.getHeight(), 
                image.getShortTitle());
    }
    public CoolHotColor(short[][] voxels, int width, int height, String name)
    {
        int zSize = voxels.length;
        short maxVoxel = 0;
        for (int i=0; i<zSize;i++)
        {
            for (int j=0; j < voxels[0].length; j++)
            {
                if (voxels[i][j] > maxVoxel)
                {
                    maxVoxel = voxels[i][j];
                }
            }
        }
        
        float binF = (float)maxVoxel/6.0F;
        short bin = (short)Math.floor(binF);
        
        ImageStack stack = new ImageStack(width, height);
        ImageProcessor[] procs = new ImageProcessor[zSize];
        int[] color = new int[3];
        color[0] = 0; color[1] = 0; color[2] = 0;
        
        double factor = 255.0/(double)maxVoxel;
        short maxBlue = (short)Math.round(bin*factor);
        short maxGreen = (short)Math.round(2*bin*factor);
        IJ.log("Cool to hot Coloring. Max intensity="+maxVoxel+" 1 bin max="+bin+" max blue="+bin+
        		" max green="+(2*bin)+" factor="+factor);
        // TODO Should the factor multiply against all color channels? 
        for (int z=0; z < zSize; z++)
        {
            ImageProcessor p = new ColorProcessor(width, height);
            procs[z] = p;
            
            stack.addSlice(z+"", p);
            for (int col=0; col<width; col++)
            {
                for (int row=0; row<height; row++)
                {
                    color[0] = 0; color[1] = 0; color[2] = 0;
                    short v = voxels[z][row*width+col];
                    if (v <= bin) // add blue
                    {
                        color[2] = (short)(Math.round(v*factor));
                    }
                    else if (v <= 3*bin) // add green
                    {
                        color[2] = maxBlue;
                        color[1] = (short)Math.round((v-bin)*factor);
                    }
                    else if (v <= 4*bin) // add red
                    {
                        color[2] = maxBlue;
                        color[1] = maxGreen;
                        color[0] = (short)Math.round((v-3*bin)*factor);
                    }
                    else if (v <= 5*bin) // subtract blue add red 
                    {
                    	int t = v - 4*bin;
                    	int b = (bin-t);
                    	color[2] = (short)Math.round(b*factor);
                    	if (b<0) IJ.log("Blue="+b+" ");
                    	int g = 2*bin;
                    	color[1] = maxGreen;
                    	int r = v - b - g;
                    	double rf = (double)(r)*factor;
                    	color[0] = (short)Math.round(rf);
                    	if (rf < 0 || rf > 255) IJ.log("Red="+r+"*"+factor+" = "+rf);
                    }
                    else if (v <= 6*bin) // subtract green add red 
                    {
                    	int t = v - 4*bin;
                    	int b = 0;
                    	color[2] = b;
                    	int g = 2*bin-t;
                    	color[1] = (short)Math.round(g*factor);
                    	if (g < 0) IJ.log("Green="+g+" ");
                    	int r = v - b - g;
                    	double rf = (double)(r)*factor;
                    	color[0] = (short)Math.round( rf);
                    	if (rf < 0 || rf > 255) IJ.log("Red="+r+"*"+factor+" = "+rf);
                    }
                    p.putPixel(col, row, color);
                }
            }
        }
        
        _image = new ImagePlus();   
        _image.setStack(name+"CoolHot", stack);
        
    }
    public ImagePlus getImage()
    {
        return _image;
    }
    
}

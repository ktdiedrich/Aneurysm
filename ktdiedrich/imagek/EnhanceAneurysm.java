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

import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

/** Threaded class for creating an enhanced image for diagnosing aneurysms.
 * @author Karl.Diedrih@utah.edu */
public class EnhanceAneurysm extends Thread 
{
	public static final short DISPLAY_SCREEN = 1;
	public static final short WRITE_FILE = 2;
	public static final int EXTEND_ENHANCE_RADIUS = 2;
	public static final double ROTATION_STEP_DEGREES = 10.0;
	private List<CenterlineGraph> _centerlineGraphs;
	private List<Graph> _imageGraphs;
	private String _title;
	private List<Graph> _shortestPaths;
	private float _xRes, _yRes, _zRes;
	private int _width, _height, _zSize;
	private int _windowSize;
	private double _enhancementIntensityThreshold;
	private double _enhancementSizeThreshold;
	private int _extendEnhancement;
	private double _dfcDfeRatioThreshold;
	private short _displayType;
	private String _directory;
	public EnhanceAneurysm(List<CenterlineGraph> centerlineGraphs, List<Graph> imageGraphs, String title,
			List<Graph> shortestPaths, 
			float xRes, float yRes, float zRes, int width, int height, int zSize, int windowSize)
	{
		_centerlineGraphs = centerlineGraphs;
		_imageGraphs = imageGraphs;
		_title = title;
		_shortestPaths  = shortestPaths;
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
		_width = width;
		_height = height;
		_zSize = zSize;
		_windowSize = windowSize;
		_enhancementIntensityThreshold = DistanceFromCenterline.THRESHOLD;
		_extendEnhancement = EXTEND_ENHANCE_RADIUS;
		_displayType = DISPLAY_SCREEN;
		_directory = "";
	}
	public void run()
	{
		this.enhance();
	}
	public ImagePlus enhance()
	{
		DistanceFromCenterline dtc = new DistanceFromCenterline(_xRes, _yRes, _zRes);
		dtc.setWindowSize(_windowSize);
		dtc.setIntensityThreshold(_enhancementIntensityThreshold);
		dtc.setDfcDfeRatioThreshold(_dfcDfeRatioThreshold);
        dtc.setNodesDFC(_centerlineGraphs, _shortestPaths, _zSize);
        dtc.enhanceAneurysm(_centerlineGraphs, _zSize);
        ImagePlus enhancedImage = DistanceFromCenterline.makeEnhancedImage(_imageGraphs, _width, _height, _zSize, _title+"Enhance");
        // remove smaller signals
        Clusters clus = new Clusters(enhancedImage);
        List<Cluster> clusters = clus.getImageClusters();
        //IJ.log(_title+" ehanced aneurysm clusters: "+clusters.size());
        /* 
        int i = 1;
        for (Cluster clst: clusters)
        {
        	IJ.log(i+" "+clst.toString());
        	i++;
        }
        */
        // removes smaller clusters from dfcImage 
        
        clus.thresholdClusters(_enhancementSizeThreshold);
        // TODO record size histogram of enhanced clusters 
        clusters = clus.getImageClusters();
        //IJ.log(_title+" Threshold enhanced clusters below percentile: "+_enhancementSizeThreshold+" remaining clusters: "+clusters.size());
        /* 
        i = 1;
        for (Cluster clst: clusters)
        {
        	IJ.log(i+" "+clst.toString());
        	i++;
        }
       	*/
        
        // grow enhanced regions along centerline
        // DistanceFromCenterline.extendImageEnhance(_centerlineGraphs, enhancedImage, _extendEnhancement);
        
        CoolHotColor imageColorer = new CoolHotColor(ImageProcess.getShortStackVoxels(
        		enhancedImage.getImageStack()), _width, _height, 
                enhancedImage.getShortTitle());
        enhancedImage = imageColorer.getImage();
        imageColorer = null;
        
        Colors colors = Colors.getColors();
        Overlay.overlayCenterlineOnColorImage(enhancedImage, _centerlineGraphs, colors.centerline);
        Graph.fillImage(enhancedImage, _shortestPaths);
        
        // TODO may display slices without Z up in Y rotation  
        // enhancedImage = Rotate3D.rotateZupY(enhancedImage);
        // IJ.log("Display enhanced image. ");
        if (_displayType == DISPLAY_SCREEN)
        {
        	enhancedImage.show();
        	enhancedImage.updateAndDraw();
        }
        else if (_displayType == WRITE_FILE)
        {
        	FileSaver saveDfe = new FileSaver(enhancedImage);
        	String st = enhancedImage.getShortTitle();
        	if (st.contains("/"))
        	{
        		int i = st.indexOf("/");
        		st = st.substring(i+1);
        	}
        	if (st.contains("\\"))
        	{
        		int i = st.indexOf("\\");
        		st = st.substring(i+1);
        	}
            String path  = _directory+st+".tif";
            //IJ.log("Preparing to save: "+path);
            saveDfe.saveAsTiffStack(path);
            IJ.log("Saved Enhanced image: "+path);
        }
        /* 
        ShadedSurface surf = new ShadedSurface();
        ImagePlus shadedSurfaceY = surf.rotateShadedSurface(enhancedImage, ROTATION_STEP_DEGREES, 
        		Free3DRotation.Y_AXIS);
        shadedSurfaceY.show();
        shadedSurfaceY.updateAndDraw();
        
        ImagePlus shadedSurfaceX = surf.rotateShadedSurface(enhancedImage, ROTATION_STEP_DEGREES, 
        		Free3DRotation.X_AXIS);
        shadedSurfaceX.show();
        shadedSurfaceX.updateAndDraw();
        */
        return enhancedImage;
	}
	public double getEnhancementIntensityThreshold() {
		return _enhancementIntensityThreshold;
	}
	public void setEnhancementIntensityThreshold(double enhancementThreshold) {
		_enhancementIntensityThreshold = enhancementThreshold;
	}
	public double getEnhancementSizeThreshold() {
		return _enhancementSizeThreshold;
	}
	public void setEnhancementSizeThreshold(double enhancementSizeThreshold) {
		_enhancementSizeThreshold = enhancementSizeThreshold;
	}
	public int getExtendEnhancement() {
		return _extendEnhancement;
	}
	public void setExtendEnhancement(int extendEnhancement) {
		_extendEnhancement = extendEnhancement;
	}
	public double getDfcDfeRatioThreshold() {
		return _dfcDfeRatioThreshold;
	}
	public void setDfcDfeRatioThreshold(double dfcDfeRatioThreshold) {
		_dfcDfeRatioThreshold = dfcDfeRatioThreshold;
	}
	public short getDisplayType() {
		return _displayType;
	}
	public void setDisplayType(short displayType) {
		_displayType = displayType;
	}
	public String getDirectory() {
		return _directory;
	}
	public void setDirectory(String directory) {
		_directory = directory;
	}
}


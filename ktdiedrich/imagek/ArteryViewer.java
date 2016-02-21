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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.CoordinateRange;
import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.db.aneurysm.Queries;
import ktdiedrich.db.aneurysm.SubjectArteryImage;


/** View artery of a subject.
 * @author ktdiedrich@gmail.com 
 * */
public class ArteryViewer 
{
	public static final int BUFFER = 20;
	public static final double ROTATION = 10;
	public static final int ROTATING_MIP_DISPLAY = 1;
	public static final int ROTATING_SHADED_SURFACE_DISPLAY = 2;
	public static final String IMAGE_SOURCE_DIR = "/v/raid5/mrdatabase/public/";
	
	public static final int ALL_ARTERY_ID = 1000;
	public static final int BASILAR_ARTERY_ID = 7;
	public static final int NONE_ARTERY_ID = 0;
	
	public ArteryViewer()
	{
	}
	public List<ImagePlus> makeArteryDisplays(int subjectId, int arteryId, String arteryName)
	{
		return makeArteryDisplays(subjectId, arteryId, arteryName, false);
	}
	
	public List<ImagePlus> makeArteryDisplays(int subjectId, int arteryId, String arteryName, boolean largestOnly)
	{
		IJ.log("Make Artery Display ArteryID: "+arteryId);
		String fullProp = DbConn.findPropertiesFile(DbConn.PROPERTIES);
		Properties props = new Properties();
		String arteryDisplayBase = null;
		String imageBase  = null;
		try
		{
			FileInputStream in = new FileInputStream(fullProp);
			props.load(in);
			arteryDisplayBase = props.getProperty("base.arterydisplay");
			imageBase = props.getProperty("base.image");
			// IJ.log("Artery Display Base: "+arteryDisplayBase);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
			IJ.log(e.getMessage());
		}
		catch (IOException e)
		{
			e.printStackTrace();
			IJ.log(e.getMessage());
		}
		String arteryFileName = arteryName.replaceAll("\\s+", "");
		List<ImagePlus> displayImages = new LinkedList<ImagePlus>(); 
        Connection con = null;
        DbConn dbConn = new DbConn();
        List<SubjectArteryImage> saImages = new LinkedList<SubjectArteryImage>(); 
        try
        {   
            con = dbConn.connect(fullProp);
            Queries queries = new Queries(con);
            List<String> fullImageDisplayPaths = queries.arteryDisplays(subjectId, arteryId, ArteryViewer.ROTATING_MIP_DISPLAY);
            List<String> segImageDisplayPaths = queries.arteryDisplays(subjectId, arteryId, ArteryViewer.ROTATING_SHADED_SURFACE_DISPLAY);
            
            
            if (fullImageDisplayPaths.size() > 0 && segImageDisplayPaths.size() > 0)
            {
            	for (String path: fullImageDisplayPaths)
            	{
            		File check = new File(path+".zip");
            		if (check.exists())
            		{
            			path = check.getAbsolutePath();
            		}
            		// IJ.log("Loading artery display :"+path);
            		ImagePlus im = IJ.openImage(path);
            		if (im!=null) displayImages.add(im);
            	}
            	for (String path: segImageDisplayPaths)
            	{
            		File check = new File(path+".zip");
            		if (check.exists())
            		{
            			path = check.getAbsolutePath();
            		}
            		// IJ.log("Loading artery display :"+path);
            		ImagePlus im = IJ.openImage(path); 
            		if (im != null) displayImages.add(im);
            	}
            }
            else
            {
	            saImages = queries.subjectArteryImages(subjectId, arteryId);
	            ImageData3D image3d = new ImageData3D();
	            String rdgehdr = dbConn.getRdgehdr();
	            if (rdgehdr != null)
	            {
	            	image3d.setRdgehdr(rdgehdr);
	            }
	            image3d.setDisplay(false);
	            SubjectArteryImage largestSAI = null;
	            int maxSize3 = 0;
	            if (largestOnly) // largest only display images read from database 
	            {
	            	for (SubjectArteryImage saIm: saImages)
	                {
	                	CoordinateRange range = saIm.getCoordRange();
	                	int sz3 = range.cubicSize();
	                	if (sz3 > maxSize3)
	                	{
	                		maxSize3=sz3;
	                		largestSAI = saIm;
	                	}
	                }
	            	saImages = new LinkedList<SubjectArteryImage>();
	            	saImages.add(largestSAI);
	            }
	            
	            Inserts inserts = new Inserts(con);
	            for (SubjectArteryImage saIm: saImages)
	            {
	            	CoordinateRange range = saIm.getCoordRange();
	            	IJ.log("ArteryViewer: "+saIm.toString());
	            	ImagePlus fullImage = null;
	            	String changedImagePath = saIm.changedImagePath(); 
	            	if ( changedImagePath != null)
	            	{
	            		fullImage = IJ.openImage(changedImagePath);
	            	}
	            	else
	            	{
	            		try {
	            			int mriType = image3d.mriType(saIm.getDirectory());
	            			if (mriType == ImageData3D.SIEMENS)
	            			{
	            				image3d.setIntelByteOrder(true);
	            			}
	    					fullImage = image3d.getImage(saIm.getDirectory(), saIm.getFileName(), false);
	    					
	    				} catch (IOException e) {
	    					IJ.log(e.getMessage());
	    					e.printStackTrace();
	    				} catch (ParseException e) {
	    					IJ.log(e.getMessage());
	    					e.printStackTrace();
	    				}
	    				if (fullImage!=null)
	    				{
	    					ImageStack stack = fullImage.getStack();
	    					ImageStack rStack = new ImageStack(fullImage.getHeight(), fullImage.getWidth());
	    					for (int i=1; i<= stack.getSize(); i++)
	    					{
	    						ImageProcessor ip = stack.getProcessor(i);
	    						rStack.addSlice(""+i, ip.rotateRight());
	    					}
	    					fullImage.setStack(fullImage.getShortTitle(), rStack);
	    				}
	            	}
	            	
	        		if (fullImage != null)
	        		{
		        		int w = fullImage.getWidth();
		    			int h = fullImage.getHeight();
		    			int z = fullImage.getStackSize();
		    			int minX=0, maxX=w, minY=0, maxY=h, minZ=0, maxZ = z;
		    			if (saIm.getArteryId() != ArteryViewer.ALL_ARTERY_ID) // only crop specific arteries 
		    			{
			    			minX = range.getMinX()-BUFFER;
			    			if (minX < 0) minX = 0;
			    			maxX = range.getMaxX()+BUFFER;
			    			if (maxX > w) maxX = w;
			    			minY = range.getMinY()-BUFFER;
			    			if (minY < 0) minY = 0; 
			    			maxY = range.getMaxY()+BUFFER;
			    			if (maxY > h) maxY = h;
			    			minZ = range.getMinZ()-BUFFER;
			    			if (minZ < 0) minZ=0;
			    			maxZ = range.getMaxZ()+BUFFER;
			    			if (maxZ > z) maxZ = z;
		    			}
		    			ImagePlus cropFullImage = Cropper.crop3d(fullImage, minX, maxX, minY, maxY, minZ, maxZ);
		    			if (cropFullImage != null)
		    			{
			    			boolean z2int = false;
			    			IJ.log("ArteryViewer: Xres: "+saIm.getXRes()+" Yres: "+saIm.getYRes()+" Zres: "+saIm.getZRes());
			    			if (saIm.getZRes() >= saIm.getXRes()*1.5 )
			    			{
			    				z2int = true;
			    				Z zz = new Z();
			    				cropFullImage = zz.fillShort(cropFullImage, 2);
			    				IJ.log("Z fill 2");
			    			}
			    			List<String> segPaths = saIm.segmentationPath();
			    			
			    			String publicDir = saIm.getPublicDir();
			    			IJ.log("Read from public: "+publicDir);
			    			if (arteryDisplayBase != null)
			    			{
			    				// TODO change publicDir to root here 
			    				publicDir = publicDir.replace(imageBase+"public", "");
			    			}
			    			IJ.log("Relocatable public Dir: "+publicDir);
			    			cropFullImage = Rotate3D.rotateZupY(cropFullImage);
			    			ImagePlus fullMIP = MIP.rotateMIP(cropFullImage, ROTATION, Free3DRotation.Y_AXIS);
			    			fullMIP.setTitle(subjectId+arteryFileName+"RotateMIP");
			    			displayImages.add(fullMIP);
			    			if (!arteryDisplayBase.endsWith(File.separator)) arteryDisplayBase = arteryDisplayBase+File.separator;
			    			String saveDir = arteryDisplayBase+publicDir;
			    			String displayFileName = FileUtil.saveTiff(fullMIP, saveDir);
			    			IJ.log("Saved: "+saveDir+displayFileName);
			    			inserts.insertArteryDisplay(subjectId, arteryId, ArteryViewer.ROTATING_MIP_DISPLAY,
			    					publicDir, displayFileName);
		    			
			        		for (String p: segPaths)
			        		{
			        			File zipSeg = new File(p+".zip");
			        			if (zipSeg.exists())
			        			{
			        				p = zipSeg.getAbsolutePath();
			        			}
			        			IJ.log("ArteryViewer segmentation: "+p);
			        			ImagePlus segIm = IJ.openImage(p);
			        			ImagePlus cropSegIm = Cropper.crop3d(segIm, minX, maxX, minY, maxY, minZ, maxZ);
			        			if (cropSegIm != null)
			        			{
				        			if (z2int)
				        		    {
				        				Z zz =new Z();
				        		    	cropSegIm = zz.fillShort(cropSegIm, 2);
				        		    }
				        		    
				        			cropSegIm = Rotate3D.rotateZupY(cropSegIm);
				        			ShadedSurface shadedSurf = new ShadedSurface();
				        		    ImagePlus shadedIm = shadedSurf.rotateShadedSurface(cropSegIm, ROTATION, Free3DRotation.Y_AXIS);
				        		    shadedIm.setTitle(subjectId+arteryFileName+"RotateShaded");
				        			displayImages.add(shadedIm);
				        			displayFileName = FileUtil.saveTiff(shadedIm, saveDir);
				        			IJ.log("Saved: "+saveDir+displayFileName);
				        			inserts.insertArteryDisplay(subjectId, arteryId, ArteryViewer.ROTATING_SHADED_SURFACE_DISPLAY,
				        					publicDir, displayFileName);
			        			}
			        		}
		    			}
	        		}
	            }
            }
        }
        catch (FileNotFoundException e)
        {
            IJ.log(e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e)
        {
            IJ.log(e.getMessage());
            e.printStackTrace();
        }
        catch (SQLException e)
        {
            IJ.log(e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (con!=null)
                    con.close();
            }
            catch (SQLException e)
            {
                IJ.log(e.getMessage());
                e.printStackTrace();
            }
        }
        return displayImages;
	}
}

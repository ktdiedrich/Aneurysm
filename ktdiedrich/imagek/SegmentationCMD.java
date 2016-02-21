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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.db.aneurysm.Queries;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

public class SegmentationCMD 
{

	/**
	 * @author ktdiedrich@gmail.com
	 * @param ags:  [file path] [Median filter size] [imageId]
	 * Command line segmentation
	 * @throws org.apache.commons.cli.ParseException 
	 */
	public static void main(String[] args) 
		throws org.apache.commons.cli.ParseException 
	{
		int imageIds[] = null;
		int medianFilterSize = 0;
		float seed = Extractor3D.SEED_HIST_THRES;
		String paths[] = null;
		
		Options options = new Options();
		options.addOption("p", true, "path name to file including filename");
		options.addOption("m", true, "median filter size, m*2+1");
		options.addOption("i", true, "image ID");
		options.addOption("f", true, "Image ID from");
		options.addOption("t", true, "Image ID to, (inclusive)");
		options.addOption("s", true, "Seed threshold default "+seed);
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse( options, args);
		
		if (cmd.hasOption("s"))
		{
			seed = Float.parseFloat(cmd.getOptionValue("s"));
		}
		if (cmd.hasOption("i"))
		{
			imageIds = new int[1];
			imageIds[0] = Integer.parseInt(cmd.getOptionValue("i"));
			paths = new String[1];
			paths[0] = getImageIdPath(imageIds[0]);
			// TODO get path to image ID from database and properties file. 
		}
		if (cmd.hasOption("f") && cmd.hasOption("t"))
		{
			int from = Integer.parseInt(cmd.getOptionValue("f"));
			int to = Integer.parseInt(cmd.getOptionValue("t"));
			int range = to-from+1;
			paths = new String[range];
			imageIds = new int[range];
			for (int i=0, imId=from; i <range; i++, imId++)
			{
				imageIds[i] = imId;
				paths[i] = getImageIdPath(imId);
			}
		}
		if (paths == null && cmd.hasOption("p"))
		{
			paths = new String[1];
			paths[0] = cmd.getOptionValue("p");
		}
		if (cmd.hasOption("m"))
		{
			medianFilterSize = Integer.parseInt(cmd.getOptionValue("m"));
		}
		
		// System.out.println("ImageID: "+imageId+" Path: "+paths+" Median filter: "+medianFilterSize);
		if (paths != null)
		{
			int i = 0;
			for (String path: paths)
			{
				String p[] = parseDirectoryFileName(path);
				String dirPath = p[0];
				ImagePlus segImage = segment(path, medianFilterSize, imageIds[i], seed);
				String title = segImage.getShortTitle();
				if (title.contains(File.separator));
				{
					title = parseDirectoryFileName(title)[1];
				}
		        String outputPath = null;
		        if (!dirPath.endsWith(File.separator))
		        	dirPath = dirPath+File.separator;
		        outputPath = dirPath+title+".zip";
		        
		        FileSaver fs = new FileSaver(segImage);
		        fs.saveAsZip(outputPath);
		        System.out.println("Saved: "+outputPath);
		        ImagePlus mipYim = MIP.createShortMIP(segImage, MIP.Y_AXIS);
		        fs = new FileSaver(mipYim);
		        title = mipYim.getShortTitle();
				if (title.contains(File.separator));
				{
					title = parseDirectoryFileName(title)[1];
				}
		        outputPath = dirPath+title+".png";
		        fs.saveAsPng(outputPath);
		        System.out.println("Saved: "+outputPath+"\n");
		        i++;
			}
		}
	}
	public static ImagePlus segment(String path, int medianFilterSize, int imageId, float seed)
	{
		String dir = parseDirectoryFileName(path)[0];
		System.out.println("Path: "+path);
		File f = new File(path);
		if (!f.exists())
		{
			// look for unzipped files 
			if (path.endsWith("tif.zip")) path = path.replace("tif.zip", "tif");
			else if (path.endsWith("zip")) path  = path.replace("zip", "tif");
			System.out.println("Looking for: "+path);
		}
		ImagePlus wholeImage = IJ.openImage(path);
		
		Extractor3D segmentor = new Extractor3D();
		
		segmentor.setMedFilterStdDevAbove(Extractor3D.MEDIAN_FILTER_STD_DEV_ABOVE);
		segmentor.setMedianFilterSize(medianFilterSize);
        
		segmentor.setScalpDist(Extractor3D.SCALP_SKULL);
		segmentor.setMaxChisq(Extractor3D.CHI_SQ_SMOOTHNESS);
		segmentor.setZDiff(Extractor3D.VOXEL_Z_DIFF);
		segmentor.setSeedClusterMin(Extractor3D.MIN_2D_SEED_SIZE);
        
		segmentor.setSeedHistogramThreshold(seed);
        
		segmentor.setFillHolesTimes(Extractor3D.HOLE_FILL_ITERATIONS);
		segmentor.setHoleFillDirections(Extractor3D.HOLE_FILL_DIRECTIONS);
		segmentor.setHoleFillRadius(Extractor3D.HOLE_FILL_RADIUS);
        
		segmentor.setClusterSizeThreshold(Extractor3D.MIN_3D_CLUSTER_SIZE);
		segmentor.setBubbleFillAlgorithm(BubbleFill.BUBBLE_FILL_3D);
		
		
        long start = System.currentTimeMillis();
        ImagePlus segImage = segmentor.segment(wholeImage);
        long duration = System.currentTimeMillis() - start;
        System.out.println("Segmentation: "+duration/60000+" minutes");
        if (imageId > 0)
        {
        	// TODO activate 
        	recordDatabase(segmentor, imageId, dir);
        }
        
        return segImage;
	}
	public static String[] parseDirectoryFileName(String path)
	{
		int lastSep = 0;
		lastSep = path.lastIndexOf(File.separator);
		String dirPath = path.substring(0, lastSep+1);
		String inputFileName = path.substring(lastSep+1);
		String s[] = new String[2];
		s[0] = dirPath;
		s[1] = inputFileName;
		return s;
	}
	public static int parseSubjectId(String directory)
	{
		int subjectId = 0;
		String regex = "[htn|nor|\\"+File.separator+"](\\d+)\\"+File.separator;
		Pattern  subjectPat= Pattern.compile(regex);
    	Matcher mat = subjectPat.matcher(directory);
    	
    	
    	while (mat.find())
    	{
    		subjectId = Integer.parseInt(mat.group(1));
    	}
    	return subjectId;
	}
	public static String getImageIdPath(int imageId)
	{
		String path = null;
		Connection con = null;
		DbConn dbConn = new DbConn();
		try
		{	
			con = dbConn.connect();
			Queries q = new Queries(con);
			path = q.getImagePath(imageId);
		}
		catch (FileNotFoundException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return path;
	}
	public static void recordDatabase(Extractor3D segmentor, int imageId, String directory)
	{
		Connection con = null;
		DbConn dbConn = new DbConn();
		try
		{	
			con = dbConn.connect();
			Inserts inserts = new Inserts(con);
			String name = segmentor.getSegBaseName();
			if (name.contains(File.separator))
			{
				name = SegmentationCMD.parseDirectoryFileName(name)[1];
			}
			int segmentationId = inserts.insertSegmentation(
					segmentor.getSeedClusterMin(),
					segmentor.getClusterSizeThreshold(),
					segmentor.getMaxChisq(),
					segmentor.getZDiff(),
					segmentor.getSeedHistogramThreshold(),
					segmentor.getScalpDist(),
					segmentor.getFillHolesTimes(),
					segmentor.getHoleFillDirections(),
					segmentor.getHoleFillRadius(),
					segmentor.getMedianFilterSize(),
					segmentor.getMedFilterStdDevAbove(),
					imageId, 
					segmentor.getBubbleFillAlgorithm(),
					name+".zip", // segFileName
					directory
	        );
			System.out.println("Recorded Segmentation_id: "+segmentationId+" Image_id: "+imageId);
		}
		catch (FileNotFoundException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		catch (IOException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		catch (SQLException e)
		{
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		finally
		{
			try
			{
				con.close();
			}
			catch (SQLException e)
			{
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}
}

/*=========================================================================
 *
 *  Copyright (c)   Karl T. Diedrich 
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

package ktdiedrich.db.aneurysm;

import ktdiedrich.db.DbConn;
import ktdiedrich.imagek.ArteryViewer;

import ij.IJ;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

/** Queries of the Aneurysm database.
 * @author ktdiedrich@gmail.com
 *  */
public class Queries 
{
	private PreparedStatement _hasSubjectId;
	private PreparedStatement _mris;
	private PreparedStatement _measureArteries;
	private PreparedStatement _variableArteries;
	private PreparedStatement _selectArteries;
	private PreparedStatement _arteryShapes;
	private PreparedStatement _imageId;
	private PreparedStatement _maxTortuosityId;
	private PreparedStatement _maxRateExperimentId;
	private PreparedStatement _maxCenterlineId;
	private PreparedStatement _maxSegmentationId;
	private PreparedStatement _imageNote;
	private PreparedStatement _subjectNote;
	private PreparedStatement _subjectarteryShape;
	private PreparedStatement _imageChangedimage;
	private PreparedStatement _imagePath;
	private PreparedStatement _subjectArteryImageFiles;
	private PreparedStatement _arteryCentRange;
	private PreparedStatement _xyzRes;
	private PreparedStatement _arteriesForSubject;
	 
	private PreparedStatement _raters; 
	private PreparedStatement _rateScales;
	private PreparedStatement _arteryDisplay;
	private PreparedStatement _subjectIds;
	private PreparedStatement _rateExperimentToday;
	private PreparedStatement _hasRating;
	private PreparedStatement _segmentationImage;
	private PreparedStatement _segImageRes;
	private Connection _dbConn;
	public Queries(Connection dbConn)
		throws SQLException
	{
		_dbConn = dbConn;
		_hasSubjectId = dbConn.prepareStatement("select subject_id from subject where subject_id=?");
		_mris = dbConn.prepareStatement("Select mri_id, tesla, brand from mri m, brand b where m.brand_id=b.brand_id");
		_measureArteries = dbConn.prepareStatement("select artery_id, arteryname, side from measurearteryview ");
		_variableArteries = dbConn.prepareStatement("select artery_id, arteryname, side from variablearteryview ");
		_selectArteries = dbConn.prepareStatement("select artery_id, arteryname, side from selectarteryview ");
		_arteryShapes = dbConn.prepareStatement("select arteryshape_id, arteryshape from arteryshape");
		_imageId = dbConn.prepareStatement("select image_id from image where filename=? and directory=? and mri_id=?");
		_maxTortuosityId = dbConn.prepareStatement("select max(tortuosity_id) from centerlinetortuosity");
		_maxRateExperimentId = dbConn.prepareStatement("select max(rateexperiment_id) from rateexperiment ");
		_maxCenterlineId = dbConn.prepareStatement("select max(centerline_id) from centerline");
		_maxSegmentationId = dbConn.prepareStatement("select max(segmentation_id) from segmentation");
		_imageNote = dbConn.prepareStatement("select note from image where image_id=?");
		_subjectNote = dbConn.prepareStatement("select note from subject where subject_id=?");
		_subjectarteryShape = dbConn.prepareStatement("select s.subject_id, s.artery_id, s.arteryshape_id, a.arteryshape from subjectartery s, arteryshape a where s.subject_id=? and s.artery_id=? and s.arteryshape_id = a.arteryshape_id");
		_imageChangedimage = dbConn.prepareStatement("select changedimage from image where image_id=?");
		_subjectArteryImageFiles = dbConn.prepareStatement("select i.subject_id, i.image_id, i.filename, i.directory, i.changedimage, g.segmentation_id, c.centerline_id, ct.tortuosity_id from image i, segmentation g, centerline c, centerlinetortuosity ct where ct.usable!='no' and c.rescorrect!='no'and  g.image_id=i.image_id and c.segmentation_id=g.segmentation_id and c.centerline_id=ct.centerline_id and i.subject_id=? and ct.artery_id = ? ");
		_arteryCentRange = dbConn.prepareStatement("select tortuosity_id, min(x) as minx, max(x) as maxx, min(y) as miny, max(y) as maxy, min(z) as minz, max(z) as maxz from tortuositycoordinate where tortuosity_id=?");
		_xyzRes = dbConn.prepareStatement("Select xres, yres, zres from centerline where centerline_id=?");
		_arteriesForSubject = dbConn.prepareStatement("select distinct i.subject_id, ct.artery_id, a.arteryname, s.side  from image i, segmentation g, centerline c, centerlinetortuosity ct, artery a, side s where ct.usable!='no' and c.rescorrect!='no'and  g.image_id=i.image_id and c.segmentation_id=g.segmentation_id and c.centerline_id=ct.centerline_id and ct.artery_id=a.artery_id and a.side_id=s.side_id and i.subject_id=? and a.artery_id != 100000 order by arteryname, side");
		_raters = dbConn.prepareStatement("Select rater_id, rater from rater ");
		_rateScales = dbConn.prepareStatement("select ratescale_id, low, high from ratescale ");
		_arteryDisplay = dbConn.prepareStatement("select subject_id, artery_id, arterydisplaytype_id, directory, filename from arterydisplay where subject_id=? and artery_id=? and arterydisplaytype_id=? ");
		_subjectIds = dbConn.prepareStatement("select subject_id from subject order by subject_id ");
		_rateExperimentToday = dbConn.prepareStatement("select rateexperiment_id, ratescale_id, rater_id, expdate from rateexperiment where rater_id=? and ratescale_id=? and expdate=curdate() ");
		_hasRating = dbConn.prepareStatement("select rating from rating where rateexperiment_id=? and subject_id=? and artery_id=? ");
		_imagePath = dbConn.prepareStatement("select filename, changedimage, directory from image where image_id=?");
		_segmentationImage = dbConn.prepareStatement("select filename, directory from segmentation where segmentation_id=?");
		_segImageRes = dbConn.prepareStatement("select g.segmentation_id, i.image_id, i.xres, i.yres, i.zres from image i, segmentation g where  i.image_id=g.image_id and segmentation_id=?");
	}
	public String imageChangedimage(int image_id)
		throws SQLException 
	{
		String im = null;
		_imageChangedimage.setInt(1, image_id);
		ResultSet r = _imageChangedimage.executeQuery();
		while (r.next())
		{
			im = r.getString("changedimage");
		}
		return im;
	}
	public String subjectarteryShape(int subject_id, int artery_id)
		throws SQLException 
	{
		String s = null;
		_subjectarteryShape.setInt(1, subject_id);
		_subjectarteryShape.setInt(2, artery_id);
		ResultSet r = _subjectarteryShape.executeQuery();
		while (r.next())
		{
			s = r.getString("arteryshape");
		}
		return s;
	}
	public String imageNote(int id)
		throws SQLException 
	{
		String imageNote = null;
		_imageNote.setInt(1, id);
		ResultSet res = _imageNote.executeQuery();
		while (res.next())
		{
			imageNote =  res.getString("note");
		}
		return imageNote;
	}
	
	public String subjectNote(int id)
		throws SQLException 
	{
		String note = null;
		_subjectNote.setInt(1, id);
		ResultSet res = _subjectNote.executeQuery();
		while (res.next())
		{
			note =  res.getString("note");
		}
		return note;
	}
	
	public int maxCenterlineTortuosityId()
	    throws SQLException 
	{
	    ResultSet res = _maxTortuosityId.executeQuery();
	    int max = 0;
	    while (res.next())
	    {
	        max = res.getInt(1);
	    }
	    return max;
	}
	
	public int maxCenterlineId()
        throws SQLException 
    {
        ResultSet res = _maxCenterlineId.executeQuery();
        int max = 0;
        while (res.next())
        {
            max = res.getInt(1);
        }
        return max;
    }
	
	public int maxSegmentationId()
    	throws SQLException 
    {
		ResultSet res = _maxSegmentationId.executeQuery();
		int max = 0;
		while (res.next())
		{
			max = res.getInt(1);
		}
		return max;
    }
	public int maxRateExperimentId()
		throws SQLException 
	{
		ResultSet res = _maxRateExperimentId.executeQuery();
		int max = 0;
		while (res.next())
		{
			max = res.getInt(1);
		}
		return max;
	}
	/** @return existing image id or 0 if none existed. */
	public int getImageId(String filename, String directory, int mri_id)
	    throws SQLException 
	{
	    _imageId.setString(1, filename);
	    _imageId.setString(2, directory);
	    _imageId.setInt(3, mri_id);
	    ResultSet res = _imageId.executeQuery();
	    int id = 0;
	    while(res.next())
	    {
	        id = res.getInt("image_id");
	    }
	    return id;
	}
	public boolean hasSubjectId(int subject_id)
		throws SQLException 
	{
		_hasSubjectId.setInt(1, subject_id);
		ResultSet res = _hasSubjectId.executeQuery();
		return res.first();
	}
	public Map<String, Integer> mris()
		throws SQLException
	{
		Map<String, Integer> mri = new HashMap<String, Integer>();
		ResultSet res = _mris.executeQuery();
		while (res.next())
		{
			int mri_id = res.getInt("mri_id");
			float tesla = res.getFloat("tesla");
			String brand = res.getString("brand");
			mri.put(brand+" "+tesla, mri_id);
		}
		return mri;
	}
	
	public Map<String, Integer> measureArteries()
	throws SQLException
	{
		Map<String, Integer> m = new HashMap<String, Integer>();
		ResultSet res = _measureArteries.executeQuery();
		while (res.next())
		{
			int artery_id = res.getInt("artery_id");
			String arteryname = res.getString("arteryname");
			String side = res.getString("side");
			m.put(arteryname+" "+side, artery_id);
		}
		return m;
	}
	
	public Map<String, Integer> variableArteries()
		throws SQLException
	{
		Map<String, Integer> m = new HashMap<String, Integer>();
		ResultSet res = _variableArteries.executeQuery();
		while (res.next())
		{
			int artery_id = res.getInt("artery_id");
			String arteryname = res.getString("arteryname");
			String side = res.getString("side");
			m.put(arteryname+" "+side, artery_id);
		}
		return m;
	}
	
	public Map<String, Integer> selectArteries()
        throws SQLException
    {
        Map<String, Integer> m = new HashMap<String, Integer>();
        ResultSet res = _selectArteries.executeQuery();
        while (res.next())
        {
            int artery_id = res.getInt("artery_id");
            String arteryname = res.getString("arteryname");
            String side = res.getString("side");
            m.put(arteryname+" "+side, artery_id);
        }
        return m;
    }
	
	public Map<String, Integer> arteryShapes()
        throws SQLException
    {
        Map<String, Integer> m = new HashMap<String, Integer>();
        ResultSet res = _arteryShapes.executeQuery();
        while (res.next())
        {
            int arteryshape_id = res.getInt("arteryshape_id");
            String arteryshape = res.getString("arteryshape");
            m.put(arteryshape, arteryshape_id);
        }
        return m;
    }
	
	/** Get a list of information and full path names to files with from the subject with the artery */
	public List<SubjectArteryImage> subjectArteryImages(int subjectId, int arteryId)
		throws SQLException 
	{
		int queryArteryId = arteryId;
		if (arteryId == ArteryViewer.ALL_ARTERY_ID)
		{
			queryArteryId = ArteryViewer.BASILAR_ARTERY_ID; // for all arteries, query on basilar artery 
		}
		String fullProp = DbConn.findPropertiesFile(DbConn.PROPERTIES);
		Properties props = new Properties();
		String imageBase = null;
		try
		{
			FileInputStream in = new FileInputStream(fullProp);
			props.load(in);
			imageBase = props.getProperty("base.image");
			IJ.log("base.image: "+imageBase);
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
		
		_subjectArteryImageFiles.setInt(1, subjectId);
		_subjectArteryImageFiles.setInt(2, queryArteryId);
		ResultSet res = _subjectArteryImageFiles.executeQuery();
		List<SubjectArteryImage> fnames = new LinkedList<SubjectArteryImage>();
		while (res.next())
		{
			StringBuffer full = new StringBuffer(res.getString("directory"));
			full.append(res.getString("filename"));
			SubjectArteryImage sai = new SubjectArteryImage(res.getInt("subject_id"),
					res.getInt("image_id"),
					res.getInt("segmentation_id"),
					res.getInt("centerline_id"),
					res.getInt("tortuosity_id"),
					imageBase+res.getString("directory"),
					res.getString("filename"),
					res.getString("changedimage"));
			sai.setArteryId(arteryId);
			float[] xyz = getXYZres(sai.getCenterlineId());
			sai.setXRes(xyz[0]);
			sai.setYRes(xyz[1]);
			sai.setZRes(xyz[2]);
			if (arteryId != ArteryViewer.ALL_ARTERY_ID)
			{
				sai.setCoordRange(arteryCenterlineRange(sai.getTortuosityId()));
			}
			else
			{
				sai.setCoordRange(new CoordinateRange(0,0,0,0,0,0));
			}
			fnames.add(sai);
		}
		return fnames;
	}
	/** The ranges in x, y, z of the artery centerlines. */
	public CoordinateRange arteryCenterlineRange(int tortuosityId)
		throws SQLException 
	{
		_arteryCentRange.setInt(1, tortuosityId);
		ResultSet res = _arteryCentRange.executeQuery();
		CoordinateRange range = null; 
		while (res.next())
		{
			range  = new CoordinateRange(res.getInt("minx"), res.getInt("maxx"),
					res.getInt("miny"), res.getInt("maxy"), 
					res.getInt("minz"), res.getInt("maxz"));
		}
		
		return range;
	}
	public float[] getXYZres(int centerlineId)
		throws SQLException 
	{
		_xyzRes.setInt(1, centerlineId);
		ResultSet res = _xyzRes.executeQuery();
		float[] xyz = new float[3];
		while (res.next())
		{
			xyz[0] = res.getFloat("xres");
			xyz[1] = res.getFloat("yres");
			xyz[2] = res.getFloat("zres");
		}
		return xyz;
	}
	public Map<String, Integer> arteriesForSubject(int subjectId)
		throws SQLException 
	{
		_arteriesForSubject.setInt(1, subjectId);
		Map<String, Integer> m = new HashMap<String, Integer>();
		ResultSet res = _arteriesForSubject.executeQuery();
		// TODO display all arteries in whole image
		m.put("All", ArteryViewer.ALL_ARTERY_ID);
		while (res.next())
        {
            int artery_id = res.getInt("artery_id");
            String arteryname = res.getString("arteryname");
            String side = res.getString("side");
            m.put(arteryname+" "+side, artery_id);
        }
		return m;
	}
	public Map<String, Integer> raters()
    	throws SQLException
    {
		Map<String, Integer> m = new HashMap<String, Integer>();
		ResultSet res = _raters.executeQuery();
		while (res.next())
		{
			int id = res.getInt("rater_id");
			String name = res.getString("rater");
			m.put(name, id);
		}
		return m;
    }
	public Map<String, Integer> rateScales()
		throws SQLException
	{
		Map<String, Integer> m = new HashMap<String, Integer>();
		ResultSet res = _rateScales.executeQuery();
		while (res.next())
		{
			int id = res.getInt("ratescale_id");
			int[] name = new int[2];
			name[0] = res.getInt("low");
			name[1] = res.getInt("high");
			m.put(name[0]+"-"+name[1], id);
		}
		return m;
	}
	
	/** @return A list of full path names to artery display images. 
	 * @param arteryDisplayTypeId defined in ArteryViewer and table arterydisplaytype */
	public List<String> arteryDisplays(int subjectId, int arteryId, int arteryDisplayTypeId)
		throws SQLException 
	{
		String fullProp = DbConn.findPropertiesFile(DbConn.PROPERTIES);
		Properties props = new Properties();
		String arteryDisplayBase = null;
		try
		{
			FileInputStream in = new FileInputStream(fullProp);
			props.load(in);
			arteryDisplayBase = props.getProperty("base.arterydisplay");
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
		List<String> filePaths = new LinkedList<String>();
		_arteryDisplay.setInt(1, subjectId);
		_arteryDisplay.setInt(2, arteryId);
		_arteryDisplay.setInt(3, arteryDisplayTypeId);
		ResultSet res = _arteryDisplay.executeQuery();
		while (res.next())
		{
			StringBuffer path = new StringBuffer();
			if (arteryDisplayBase != null)
			{
				path.append(arteryDisplayBase);
				String s = new String(path);
				if (!s.endsWith(File.separator))
				{
					path.append(File.separator);
				}
			}
			path.append(res.getString("directory"));
			path.append(res.getString("filename"));
			IJ.log("ArteryDisplay: "+path.toString());
			filePaths.add(path.toString());
		}
		
		return filePaths;
	}
	
	public List<Integer> subjectIds()
		throws SQLException 
	{
		List<Integer> subjects = new LinkedList<Integer>();
		ResultSet res = _subjectIds.executeQuery(); 
		while (res.next())
		{
			subjects.add(res.getInt("subject_id"));
		}
		return subjects;
	}
	
	/** Return the rateExperimentId for the rater and rate scale today or 0 if there isn't one. */
	public int rateExperimentToday(int raterId, int rateScaleId)
		throws SQLException 
	{
		_rateExperimentToday.setInt(1, raterId);
		_rateExperimentToday.setInt(2, rateScaleId);
		ResultSet res = _rateExperimentToday.executeQuery();
		int id = 0;
		while (res.next())
		{
			id = res.getInt("rateexperiment_id");
		}
		return id;
	}
	/** Get rating or return 0 for no rating. */
	public int hasRating(int rateExperimentId, int subjectId, int arteryId)
		throws SQLException 
	{
		int rating = 0;
		_hasRating.setInt(1, rateExperimentId);
		_hasRating.setInt(2, subjectId);
		_hasRating.setInt(3, arteryId);
		ResultSet res = _hasRating.executeQuery();
		while (res.next())
		{
			rating = res.getInt("rating");
		}
		return rating;
	}
	public Queue<SubjectArtery> subjectArteryToRate(int raterId, int rateExperimentId)
		throws SQLException 
	{
		return subjectArteryToRate(raterId, rateExperimentId, 0);
	}
	/** Get list of subjects and arteries that need rating. */
	public Queue<SubjectArtery> subjectArteryToRate(int raterId, int rateExperimentId, int arteryId)
		throws SQLException 
	{
		Statement stmt = _dbConn.createStatement();
		StringBuffer sq = new StringBuffer("select distinct i.subject_id, ct.artery_id, a.arteryname, s.side  from image i, segmentation g, centerline c, centerlinetortuosity ct, artery a, side s where ct.usable!='no' and c.rescorrect!='no'and  g.image_id=i.image_id and c.segmentation_id=g.segmentation_id and c.centerline_id=ct.centerline_id and ct.artery_id=a.artery_id and a.side_id=s.side_id and a.artery_id < 100 ");
		if (arteryId > 0)
		{
			sq.append(" and a.artery_id=");
			sq.append(arteryId);
		}
		sq.append(" order by arteryname, side ");
		ResultSet res = stmt.executeQuery(sq.toString());
		PreparedStatement ratingSt = _dbConn.prepareStatement("select rateexperiment_id, subject_id, artery_id from rating where rateexperiment_id=? and subject_id=? and artery_id=? ");
		Queue<SubjectArtery> sars = new LinkedList<SubjectArtery>();
		while (res.next())
		{
			int sid = res.getInt("subject_id");
			int aid = res.getInt("artery_id");
			String arteryName = res.getString("arteryname");
			String side = res.getString("side");
			ratingSt.setInt(1, rateExperimentId);
			ratingSt.setInt(2, sid);
			ratingSt.setInt(3, aid);
			ResultSet ratingRes = ratingSt.executeQuery();
			if (!ratingRes.first())
			{
				sars.add(new SubjectArtery(sid, aid, arteryName, side));
			}
		}
		
		
		return sars; 
	}
	public List<SubjectArteryValue> tortuosityTrainingSet()
		throws SQLException 
	{
		List<SubjectArteryValue> list = new LinkedList<SubjectArteryValue>();
		Statement stmt = _dbConn.createStatement();
		ResultSet res = stmt.executeQuery("select subject_id, artery_id, value, arteryname, side from subjectarterytypeview where type_id=2 order by arteryname, value ");
		while (res.next())
		{
			SubjectArteryValue s = new SubjectArteryValue(res.getInt("subject_id"), res.getInt("artery_id"), res.getInt("value"));
			s.setArteryName(res.getString("arteryname"));
			s.setSide(res.getString("side"));
			list.add(s);
		}
		return list;
	}
	public String getImagePath(int imageId)
		throws SQLException 
	{
		String path = null;
		try 
		{
			path = DbConn.getProperty("base.image");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		_imagePath.setInt(1, imageId);	
		ResultSet res = _imagePath.executeQuery();
		String filename = null;
		String directory = null;
		String changedimage = null;
		while (res.next())
		{
			filename = res.getString("filename");
			directory = res.getString("directory");
			changedimage = res.getString("changedimage");
		}
		if (changedimage == null)
		{
			changedimage = filename;
		}
		if (!path.endsWith(File.separator)) path=path+File.separator;
		if (!directory.endsWith(File.separator)) directory=directory+File.separator;
		return path+directory+changedimage;
	}
	public String getSegmentationPath(int segId)
		throws SQLException 
	{
		String path = null;
		try 
		{
			path = DbConn.getProperty("base.image");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		_segmentationImage.setInt(1, segId);	
		ResultSet res = _segmentationImage.executeQuery();
		String filename = null;
		String directory = null;
		while (res.next())
		{
			filename = res.getString("filename");
			directory = res.getString("directory");
		}
		
		if (!path.endsWith(File.separator)) path=path+File.separator;
		if (!directory.endsWith(File.separator)) directory=directory+File.separator;
		return path+directory+filename;
	}
	/** Returns imageID, xres, yres, zres for segmentation */
	public float[] getSegmentationImageRes(int segId)
		throws SQLException 
	{
		_segImageRes.setInt(1, segId);	
		ResultSet res = _segImageRes.executeQuery();
		float[] data = new float[4];
		while (res.next())
		{
			data[0] = res.getInt("image_id");
			data[1] = res.getFloat("xres");
			data[2] = res.getFloat("yres");
			data[3] = res.getFloat("zres");
		}
		return data;
	}
	public static void printMap(Map<String, Integer> map)
	{
		Set<String> keys = map.keySet();
		for (String k: keys)
		{
			int id = map.get(k);
			System.out.println(k+": "+id);
		}
	}
	
	/** Test queries. */
	public static void main(String[] args)
		throws SQLException, IOException, FileNotFoundException
	{	
		Connection conn = null;
		DbConn dbConn = new DbConn();
		try 
		{
			conn = dbConn.connect(DbConn.findPropertiesFile("aneurysm.properties"));
			Queries q = new Queries(conn);
			System.out.println("Has subject 1: "+q.hasSubjectId(1));
			Map<String, Integer> mris = q.mris();
			printMap(mris);
			Map<String, Integer> arteries = q.measureArteries();
			printMap(arteries);
			
			
		}
		finally
		{
			if (conn != null)
				conn.close();
		}
	}
}



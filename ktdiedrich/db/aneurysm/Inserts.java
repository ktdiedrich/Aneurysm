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

import java.io.IOException;
import java.sql.*;

import ktdiedrich.db.DbConn;

/** Insert into the aneursym database 
 * @author ktdiedrich@gmail.com 
 * */
public class Inserts 
{
	// must match aneurysm.algorithm database table 
    public static final int DFE_CENTERLINE_ALGORITHM = 1; 
    
    public static final String DFE_NAME = "DFE";
    
    public static final int COM_CENTERLINE_ALGORITHM = 2;
    public static final String COM_NAME = "COM";
    
    public static final int DFE_WEIGHTED_COM = 3;
    public static final String DFE_WEIGHTED_COM_NAME = "DFE weighted COM";
    
    public static final int TORT_DFM_ALG = 4;
    public static final int TORT_DFM_3_AVE_ALG = 5;
    public static final int MDFE_WT_COM_CENTERLINE = 6;
    
    public static final int DFEWTCOM_MULT_PCCROSSNORM = 7;
    public static final String DFEWTCOM_MULT_PCCROSSNORM_NAME = "DFE weighted COM mult PC cross";
    
    public static final int DFEWTCOM_PLUS_WEIGHTEDPCCROSSNORM = 8;
    
    public static final int VELOC_DFECOM = 11;
    public static final String VELOC_DFECOM_NAME = "Velocity DFECOM";
    
    public static final int VELOC_COST = 12;
    public static final String VELOC_COST_NAME = "Velocity Cost";
    
	private Connection _dbConn;
	private PreparedStatement _image;
	private PreparedStatement _subject;
	private PreparedStatement _centerlinetortuosity;
	private PreparedStatement _dfm;
	private PreparedStatement _centerline;
	private PreparedStatement _segmentation;
	private PreparedStatement _subjectartery;
	private PreparedStatement _tortuosityCoordinate;
	private PreparedStatement _rateExperiment;
	private PreparedStatement _rating;
	private PreparedStatement _arteryDisplay;
	private Queries _queries;
	private boolean _isNewImage;
	
	public Inserts(Connection dbConn)
		throws SQLException 
	{
		_dbConn = dbConn;
		_image = dbConn.prepareStatement("insert into image(filename, directory, mri_id, xres, yres, zres, subject_id, examdate, height, weight, ageatexam) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		_subject = dbConn.prepareStatement("insert into subject(subject_id, sex, birthdate) values(?, ?, ?)");
		_centerlinetortuosity = dbConn.prepareStatement("insert into centerlinetortuosity(artery_id, centerline_id, startx, starty, startz, endx, endy, endz, algorithm_id) values(?,?,?,?,?,?,?,?, ?)");
		_dfm = dbConn.prepareStatement("insert into dfm(tortuosity_id, length, distance, dfm, dfe, direction) values(?, ?, ?, ?, ?, ?)");
		_tortuosityCoordinate = dbConn.prepareStatement("insert into tortuositycoordinate (tortuosity_id, x, y, z) values (?, ?, ?, ?) ");
		_centerline = dbConn.prepareStatement("insert into centerline(dfethreshold, minlinelength, segmentation_id, weighta, weightb, xres, yres, zres, algorithm_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
		_segmentation = dbConn.prepareStatement("insert into segmentation (min2dseed, min3dcluster, "+
					"maxchisqrsmooth, voxelzdiff, hist2dthres, scalpskull, hollfillit, holefillthres, "+
					"holefillneighborhood, medfiltersize, medfilterstdevabove, image_id, algorithm_id, filename, directory) values(?,?,?,?,?,?,?,?,?,?,?,?,?, ?, ?) ");
		_subjectartery = dbConn.prepareStatement("insert into subjectartery(subject_id, artery_id, arteryshape_id) values(?, ?, ?)");
		_rateExperiment = dbConn.prepareStatement("insert into rateexperiment(ratescale_id, rater_id, expdate) values(?,?,curdate()) ");
		_rating = dbConn.prepareStatement("insert into rating(rateexperiment_id, subject_id, artery_id, rating) values(?, ?, ?, ?)");
		_arteryDisplay = dbConn.prepareStatement("insert into arterydisplay(subject_id, artery_id, arterydisplaytype_id, directory, filename) values(?, ?, ?, ?, ?)");
		_queries = new Queries(_dbConn);
	}
	public int insertSubjectartery(int subject_id, int artery_id, int arteryshape_id)
		throws SQLException 
	{
		_subjectartery.setInt(1, subject_id);
		_subjectartery.setInt(2, artery_id);
		_subjectartery.setInt(3, arteryshape_id);
		return _subjectartery.executeUpdate();
	}
	
	/** @return segmentation_id or 0 if no insert. */
	public int insertSegmentation(int min2dseed, int min3dcluster, double maxchisqrsmooth, 
			int voxelzdiff, double hist2dthres, int scalpskull, int hollfillit, int holefillthres, 
				int holefillneighborhood, int medfiltersize, double medfilterstdevabove, int image_id, 
				int bubbleAlgorithm_id, String filename, String directory)
		throws SQLException 
	{
		String imageBase = null;
		try 
		{
			imageBase = DbConn.getProperty("base.image");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		if (imageBase != null)
		{
			directory = directory.replace(imageBase, "");
		}
		
		_segmentation.setInt(1, min2dseed);
		_segmentation.setInt(2, min3dcluster);
		_segmentation.setDouble(3, maxchisqrsmooth);
		_segmentation.setInt(4, voxelzdiff);
		_segmentation.setDouble(5, hist2dthres);
		_segmentation.setInt(6, scalpskull);
		_segmentation.setInt(7, hollfillit);
		_segmentation.setInt(8, holefillthres);
		_segmentation.setInt(9, holefillneighborhood);
		_segmentation.setInt(10, medfiltersize);
		_segmentation.setDouble(11, medfilterstdevabove);
		_segmentation.setInt(12, image_id);
		_segmentation.setInt(13, bubbleAlgorithm_id);
		_segmentation.setString(14, filename);
		_segmentation.setString(15, directory);
		int u = _segmentation.executeUpdate();
		
		if (u > 0)
		{
			int segId  = _queries.maxSegmentationId();
			return segId;
		}
		return 0;
	}
	/** @return inserted centerline _id or 0 on insert failure. */
	public int insertCenterline(float dfethreshold, int minlinelength, int segmentation_id, 
			float weightA, float weightB, float xres, float yres, float zres, int algorithm_id)
		throws SQLException 
	{
		_centerline.setFloat(1, dfethreshold);
		_centerline.setInt(2, minlinelength);
		_centerline.setInt(3, segmentation_id);
		_centerline.setFloat(4, weightA);
		_centerline.setFloat(5, weightB);
		_centerline.setFloat(6, xres);
		_centerline.setFloat(7, yres);
		_centerline.setFloat(8, zres);
		_centerline.setInt(9, algorithm_id);
		int u = _centerline.executeUpdate();
		if (u > 0)
		{
			return _queries.maxCenterlineId();
		}
		return 0;
	}
	public void insertDFM(int tortuosity_id, float length, float distance, float dfm, float dfe, String direction)
		throws SQLException 
	{
		_dfm.setInt(1, tortuosity_id);
		_dfm.setFloat(2, length);
		_dfm.setFloat(3, distance);
		_dfm.setFloat(4, dfm);
		_dfm.setFloat(5, dfe);
		_dfm.setString(6, direction);
		_dfm.executeUpdate();
	}
	
	public void insertCoordinate(int tortuosity_id, int x, int y, int z)
		throws SQLException 
	{
		_tortuosityCoordinate.setInt(1, tortuosity_id);
		_tortuosityCoordinate.setInt(2, x);
		_tortuosityCoordinate.setInt(3, y);
		_tortuosityCoordinate.setInt(4, z);
		_tortuosityCoordinate.executeUpdate();
	}
	
	/** Insert if the image is new and not in the database. 
	 * @return the previous or new image_id or 0 if insert failed. */
	public int insertNewImage(String filename, String directory, int mri_id, float xres, float yres, float zres, 
				int subject_id, String examDate, float height, float weight, int ageatexam)
	    throws SQLException 
	{
	    int id = _queries.getImageId(filename, directory, mri_id);
	    if (id > 0)
	    {
	    	_isNewImage = false;
	        return id;
	    }
	    else
	    {
	        _image.setString(1, filename);
	        _image.setString(2, directory);
	        _image.setInt(3, mri_id);
	        _image.setFloat(4, xres);
	        _image.setFloat(5, yres);
	        _image.setFloat(6, zres);
	        _image.setInt(7, subject_id);
	        _image.setString(8, examDate);
	        _image.setFloat(9, height);
	        _image.setFloat(10, weight);
	        _image.setInt(11, ageatexam);
	        int u = _image.executeUpdate();
	        if (u > 0)
	        {
	            id = _queries.getImageId(filename, directory, mri_id);
	        }
	        _isNewImage = true;
	    }
	    return id;
	}
	
	/** Insert if the subject_id is new 
	 * @return true if the new subject was inserted. */
	public boolean insertNewSubject(int subject_id, String sex, String birthDate)
	    throws SQLException 
	{
	    if (_queries.hasSubjectId(subject_id))
	    {
	        return false;
	    }
	    else
	    {
	        _subject.setInt(1, subject_id);
	        _subject.setString(2, sex);
	        _subject.setString(3, birthDate);
	        int u = _subject.executeUpdate();
	        if (u > 0 )
	        {
	            return true;
	        }
	    }
	    return false;
	}
	
	/** @return new tortuosity_id or 0 if the insert failed. */
	public int insertCenterlineTortuosity(int artery_id, int centerline_id, 
			int startx, int starty, int startz, int endx, int endy, int endz, int algorithm_id)
	    throws SQLException 
	{
		_centerlinetortuosity.setInt(1, artery_id);
	    _centerlinetortuosity.setInt(2, centerline_id);
	    _centerlinetortuosity.setInt(3, startx);
	    _centerlinetortuosity.setInt(4, starty);
	    _centerlinetortuosity.setInt(5, startz);
	    _centerlinetortuosity.setInt(6, endx);
	    _centerlinetortuosity.setInt(7, endy);
	    _centerlinetortuosity.setInt(8, endz);
	    _centerlinetortuosity.setInt(9, algorithm_id);
	    int u = _centerlinetortuosity.executeUpdate();
	    if (u > 0)
	    {
	        return _queries.maxCenterlineTortuosityId(); 
	    }
	    return 0;
	}
	public boolean isNewImage() 
	{
		return _isNewImage;
	}
	
	/** @return the existing or new rateExperiment_id or 0 if insert failed. */
	public int enterRateExperiment(int rateScaleId, int raterId)
		throws SQLException 
	{
        int rateExperimentId = _queries.rateExperimentToday(raterId, rateScaleId);
        if (rateExperimentId == 0)
        {
        	_rateExperiment.setInt(1, rateScaleId);
        	_rateExperiment.setInt(2, raterId);
        	int u = _rateExperiment.executeUpdate();
        	if (u > 0)
        	{
        		return _queries.maxRateExperimentId();
        	}
        }
		return rateExperimentId;
	}
	public int insertRating(int rateExperimentId, int subjectId, int arteryId, int rating)
		throws SQLException 
	{
		int oldRating = _queries.hasRating(rateExperimentId, subjectId, arteryId);
		int u = 0;
		if (oldRating == 0)
		{
			_rating.setInt(1, rateExperimentId);
			_rating.setInt(2, subjectId);
			_rating.setInt(3, arteryId);
			_rating.setInt(4, rating);
			u = _rating.executeUpdate();
		}
		else
		{
			Updates up = new Updates(_dbConn);
			u = up.updateRating(rateExperimentId, subjectId, arteryId, rating);
		}
		return u;
	}
	public int insertArteryDisplay(int subjectId, int arteryId, int arteryDisplayTypeId, String directory,
			String fileName)
		throws SQLException 
	{
		_arteryDisplay.setInt(1, subjectId);
		_arteryDisplay.setInt(2, arteryId);
		_arteryDisplay.setInt(3, arteryDisplayTypeId);
		_arteryDisplay.setString(4, directory);
		_arteryDisplay.setString(5, fileName);
		int u = _arteryDisplay.executeUpdate();
		return u;
	}
}

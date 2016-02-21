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

package ktdiedrich.db.aneurysm;

import java.sql.*;

/** SQL updates of aneurysm database. 
 * @author ktdiedrich@gmail.com 
 * */
public class Updates 
{
	private Connection _dbConn;
	private PreparedStatement _subjectNote;
	private PreparedStatement _imageNote;
	private PreparedStatement _subjectartery;
	private PreparedStatement _imageChangedimage;
	private PreparedStatement _rating;
	private Queries _queries;
	public Updates(Connection dbConn)
		throws SQLException
	{
		_queries = new Queries(dbConn);
		_dbConn = dbConn;
		_subjectNote = dbConn.prepareStatement("update subject set note=? where subject_id=?");
		_imageNote = dbConn.prepareStatement("update image set note=? where image_id=?");
		_subjectartery = dbConn.prepareStatement("update subjectartery set arteryshape_id=? where subject_id=? and artery_id=?");
		_imageChangedimage = dbConn.prepareStatement("update image set changedimage=? where image_id=?");
		_rating = dbConn.prepareStatement("update rating set rating=? where rateexperiment_id=? and subject_id=? and artery_id=? ");
	}
	public int updateImageChangedimage(int image_id, String changedimage)
		throws SQLException 
	{
		_imageChangedimage.setString(1, changedimage);
		_imageChangedimage.setInt(2, image_id);
		return _imageChangedimage.executeUpdate();
	}
	/** Overwrites note. */
	public int updateSubjectNote(String note, int id)
		throws SQLException
	{
		//String oldNote = _queries.subjectNote(id);
		//if (oldNote != null)
		//	note = oldNote+" "+note;
		_subjectNote.setString(1, note);
		_subjectNote.setInt(2, id);
		return _subjectNote.executeUpdate();
	}
	
	/** Overwrites note. */
	public int updateImageNote(String note, int id)
		throws SQLException
	{
		//String oldNote = _queries.imageNote(id);
		//if (oldNote != null)
		//	note = oldNote+" "+note;
		_imageNote.setString(1, note);
		_imageNote.setInt(2, id);
		return _imageNote.executeUpdate();
	}
	public int updateSubjectArtery(int subject_id, int artery_id, int arteryshape_id)
		throws SQLException
	{
		_subjectartery.setInt(1, arteryshape_id);
		_subjectartery.setInt(2, subject_id);
		_subjectartery.setInt(3, artery_id);
		return _subjectartery.executeUpdate();
	}
	public int updateRating(int rateExperimentId, int subjectId, int arteryId, int rating)
		throws SQLException 
	{
		_rating.setInt(1, rating);
		_rating.setInt(2, rateExperimentId);
		_rating.setInt(3, subjectId);
		_rating.setInt(4, arteryId);
		return _rating.executeUpdate();
	}
	
}

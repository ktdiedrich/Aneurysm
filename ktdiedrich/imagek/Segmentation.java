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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Queries;

/** Segmentation functions.
 * @author ktdiedrich@gmail.com 
 * */
public class Segmentation 
{
	/** Returns a segmented image using the path in the database. */
	public static final ImagePlus getSegmentationImage(int segId)
	{
		String path = null;
		ImagePlus segImage = null;
		Connection con = null;
		DbConn dbConn = new DbConn();
		try
		{	
			con = dbConn.connect();
			Queries q = new Queries(con);
			path = q.getSegmentationPath(segId);
			segImage = IJ.openImage(path);
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
		return segImage;
	}
}

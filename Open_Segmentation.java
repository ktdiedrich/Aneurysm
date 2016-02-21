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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Queries;
import ktdiedrich.imagek.Segmentation;
import ktdiedrich.util.TempProperties;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

/** Open a segmentation by ID
 * @author ktdiedrich@gmail.com */
public class Open_Segmentation implements PlugIn 
{
	public void run(String arg0) 
	{
		GenericDialog gd = new GenericDialog("Open_Segmentation");
		gd.addNumericField("Open segmentation ID", 0, 0);
        gd.showDialog();
        if (gd.wasCanceled()) 
        {
            IJ.error("PlugIn canceled!");
            return;
        }
        int segId = (int)gd.getNextNumber();
        
		ImagePlus segImage = null;
		Connection con = null;
		DbConn dbConn = new DbConn();
		int imageId = 0;
		float xres= 0;
		float yres=0;
		float zres=0;
		try
		{	
			con = dbConn.connect();
			Queries q = new Queries(con);
			String path = q.getSegmentationPath(segId);
			segImage = IJ.openImage(path);
			float d[] = q.getSegmentationImageRes(segId);
			imageId = Math.round(d[0]);
			xres = d[1]; yres = d[2]; zres = d[3];
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
       
        segImage.show();
        segImage.updateAndDraw();
        
        try 
		{
			TempProperties tp = new TempProperties(TempProperties.ANERUYSM_TEMP);
			tp.setProperty("segmentation_id", ""+segId);
			tp.setProperty("image_id", ""+imageId);
			tp.setProperty("xres", ""+xres);
			tp.setProperty("yres", ""+yres);
			tp.setProperty("zres", ""+zres);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
}

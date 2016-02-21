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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import ij.*;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.plugin.PlugIn;
import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Queries;
import ktdiedrich.imagek.*;

/** Preprocess artery displays, saving files and recording locations in the database: aneursym.arterydisplay. 
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Preprocess_Artery_Display implements PlugIn {
    protected ImagePlus _imp;
    protected ImageCanvas _canvas;
   
    public void run(String arg0) 
    {   
    	Connection con = null;
    	DbConn dbConn = new DbConn();
        try
        {   
        	ArteryViewer aviewer = new ArteryViewer();
            con = dbConn.connect(DbConn.findPropertiesFile());
            Queries queries = new Queries(con);
            List<Integer> subjects = queries.subjectIds();
            for (int sid: subjects)
            {
            	Map<String, Integer> arteries = queries.arteriesForSubject(sid);
            	Set<String> arteryNames = arteries.keySet();
            	for (String aname: arteryNames)
            	{
            		int aid = arteries.get(aname);
            		IJ.log("Preprocessing: subject ID: "+sid+" artery: "+aid+" "+aname);
            		aviewer.makeArteryDisplays(sid, aid, aname);
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
        
    }
}



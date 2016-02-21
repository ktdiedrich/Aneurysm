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

package ktdiedrich.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** Make database connections and test connection.
 * @author ktdiedrich@gmail.com 
 * */
public class DbConn 
{
	private String _url;
	private DBStatus _dbStatus;
	
	private String _username;
	private String _rdgehdr;
	public static final String PROPERTIES = "aneurysm.properties";
	public Connection connect()
		throws FileNotFoundException, IOException, SQLException 
	{
		return connect(DbConn.findPropertiesFile());
	}
	public Connection connect(String propFilename)
		throws FileNotFoundException, IOException, SQLException 
	{
		Connection conn = null;
		Properties props = new Properties();
		
		FileInputStream in = new FileInputStream(propFilename);
		props.load(in);
		String drivers = props.getProperty("jdbc.drivers");
		if (drivers != null)
		{
			System.setProperty("jdbc.drivers", drivers);
		}
		_url = props.getProperty("jdbc.url");
		_username = props.getProperty("jdbc.username");
		_rdgehdr = props.getProperty("rdgehdr");
		String password = props.getProperty("jdbc.password");
		conn = DriverManager.getConnection(_url, _username, password);
		if (_dbStatus != null) _dbStatus.setDbField(this);
		
		return conn;
	}
	public static String getProperty(String propKey)
		throws FileNotFoundException, IOException 
	{
		String propFileName = findPropertiesFile();
		FileInputStream in = new FileInputStream(propFileName);
		Properties props = new Properties();
		props.load(in);
		return props.getProperty(propKey);
	}
	
	public DBStatus getDbStatus() 
	{
		return _dbStatus;
	}
	public void setDbStatus(DBStatus dbStatus) 
	{
		_dbStatus = dbStatus;
	}
	public static String findPropertiesFile()
	{
		return findPropertiesFile(DbConn.PROPERTIES);
	}
	/** @return full path to the properties file under UNIX or windows. */
	public static String findPropertiesFile(String filename)
	{
	    String unix = System.getenv("HOME")+File.separator+filename;
	    String windows = System.getenv("USERPROFILE")+File.separator+filename;
	    if (new File(unix).exists())
	        return unix;
	    else if (new File(windows).exists())
	        return windows;
	    return null;
	}
	public String getUrl() 
	{
		return _url;
	}
	public String getUsername() 
	{
		return _username;
	}
	/**
	 * Trial connection and query to the aneurysm database. Requires an
	 * aneurysm.properties file in the home directory.  
	 */
	public static void main(String[] args) 
		throws FileNotFoundException, IOException, SQLException 
	{
		DbConn dbConn = new DbConn();
		Connection conn = dbConn.connect(DbConn.findPropertiesFile("aneurysm.properties"));
		Statement stmt = conn.createStatement();
		ResultSet res = stmt.executeQuery("select arteryname, side from artery a, side s where a.side_id=s.side_id");
		while (res.next())
		{
			String arteryname = res.getString("arteryname");
			String side = res.getString("side");
			System.out.println(arteryname+" "+side);
		}
	}
	public String getRdgehdr() {
		return _rdgehdr;
	}

}

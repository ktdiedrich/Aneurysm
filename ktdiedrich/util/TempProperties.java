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

package ktdiedrich.util;


import java.util.Properties;
import java.util.Set;
import java.io.*;

/** Read and write temporary values in a properties file. 
 * @author ktdiedrich@gmail.com*/
public class TempProperties 
{
	public static final String ANERUYSM_TEMP = "aneurysm.tmp";
	private File _file;
	private Properties _prop;
	/** @param filename the name of the temporary file */
	public TempProperties(String filename)
		throws IOException 
	{
		String home = Home.getHome();
		String full = home + File.separator + filename;
		_file = new File(full);
		if (!_file.exists())
		{
			_file.createNewFile();
		}
		_prop = new Properties();
		
	}
	public void clear()
		throws IOException
	{
		_file.createNewFile();
	}
	public void setProperty(String key, String value) throws IOException
	{
		Writer w = new FileWriter(_file);
		BufferedWriter bw = new BufferedWriter(w);
		Set<Object> keys = _prop.keySet();
		for (Object k: keys)
		{
			String sk = (String)k;
			String val = _prop.getProperty(sk);
			_prop.setProperty(sk, val);
		}
		_prop.setProperty(key, value);
		_prop.store(bw, null);
		bw.close();
		w.close();
		
	}
	public String getProperty(String key)
		throws IOException 
	{
		FileInputStream in = new FileInputStream(_file);
		_prop.load(in);	
		String v = _prop.getProperty(key);
		in.close();
		return v;
	}
	
	public static void main(String[] args)
		throws IOException 
	{
		TempProperties tp1 = new TempProperties("test.tmp");
		tp1.setProperty("xres", ""+0.302734);
		
		TempProperties tp2 = new TempProperties("test.tmp");
		float xres = Float.parseFloat(tp2.getProperty("xres"));
		System.out.println(xres);
	}
	
}

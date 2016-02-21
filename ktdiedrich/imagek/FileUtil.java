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

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

/** File utilities 
 * @author ktdiedrich@gmail.com */
public class FileUtil 
{
	private FileUtil()
	{
		
	}
	/** Saves image file to tiff using the short title of the image and appends .tif
	 * @return file name saved to. */
	public static final String saveTiff(ImagePlus image, String directory)
	{
		return saveTiff(image, directory, image.getShortTitle());
	}
	
	public static final String saveTiff(ImagePlus image, String directory, String name)
    {
		File dirFile = new File(directory);
		IJ.log(dirFile.getAbsolutePath()+" exists? "+dirFile.exists());
		if (!dirFile.exists())
		{
			boolean fb = dirFile.mkdirs();
			IJ.log("Create dir: "+fb);
		}
		StringBuffer p = new StringBuffer(directory);
		
		String fileName = name+".tif";
		int last = directory.length()-1;
		if (directory.charAt(last) != File.separator.charAt(0))
		{
			p.append(File.separator);
		}
    	p.append(fileName);
		FileSaver fs = new FileSaver(image);
		String ps = p.toString();
		fs.saveAsTiffStack(ps);	
		return fileName;
    }
}

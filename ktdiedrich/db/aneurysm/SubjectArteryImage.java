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

import ij.IJ;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Data on an image for a given subject and artery. */
public class SubjectArteryImage 
{
	private int _subjectId;
	private int _imageId;
	private int _segmentationId;
	private int _centerlineId;
	private int _tortuosityId;
	private String _directory;
	private String _publicDir;
	private String _fileName;
	private String _changedImage;
	private CoordinateRange _coordRange;
	private int _arteryId;
	private float _xRes, _yRes, _zRes;
	public SubjectArteryImage(int subjectId, int imageId, int segmentationId, int centerlineId, 
			int tortuosityId, String directory, String fileName, String changedImage)
	{
		_subjectId = subjectId;
		_imageId = imageId;
		_segmentationId = segmentationId;
		_centerlineId = centerlineId;
		_tortuosityId = tortuosityId;
		_directory = directory;
		_fileName = fileName;
		_changedImage = changedImage;
	}
	public String fullImageFilePath()
	{
		return _directory+_fileName;
	}
	public List<String> segmentationPath()
	{
		
		Pattern pri = Pattern.compile("private");
    	Matcher mat = pri.matcher(_directory);
    	
    	_publicDir = mat.replaceAll("public");
    	File dir = new File(_publicDir);
    	List<String> segPaths = new LinkedList<String>();
    	if (dir.exists())
    	{
    		segPaths = findSegFiles(dir);
    	}
    	if (segPaths.size() == 0)
    	{
    		Pattern anal = Pattern.compile("analysis");
    		mat = anal.matcher(_publicDir);
    		_publicDir = mat.replaceAll("");
    		Pattern cent = Pattern.compile("centerline");
    		mat = cent.matcher(_publicDir);
    		_publicDir = mat.replaceAll("");
    		dir = new File(_publicDir);
    		if (dir.exists())
    		{
    			segPaths = findSegFiles(dir);
    		}
    	}
    	return segPaths;
	}
	/** Changed image file path name or null. */
	public String changedImagePath()
	{
		if (_changedImage != null)
		{
			Pattern pri = Pattern.compile("private");
	    	Matcher mat = pri.matcher(_directory);
	    	
	    	String publicDir = mat.replaceAll("public");
	    	File dir = new File(publicDir);
	    	File changedFile = new File(publicDir+File.separator+_changedImage);
	    	if (changedFile.exists())
	    	{
	    		return changedFile.getAbsolutePath();
	    	}
	    	else 
	    	{
	    		Pattern anal = Pattern.compile("analysis");
	    		mat = anal.matcher(publicDir);
	    		publicDir = mat.replaceAll("");
	    		Pattern cent = Pattern.compile("centerline");
	    		mat = cent.matcher(publicDir);
	    		publicDir = mat.replaceAll("");
	    		dir = new File(publicDir);
	    		if (dir.exists())
	    		{
	    			changedFile = new File(publicDir+File.separator+_changedImage);
	    			if (changedFile.exists())
	    			{
	    				return changedFile.getAbsolutePath();
	    			}
	    		}
	    	}
		}
		return null;
	}
	private List<String> findSegFiles(File dir)
	{
		List<String> segPaths = new LinkedList<String>();
    	Pattern seg = Pattern.compile(".+(SegArtery\\.tif|SegArtery\\.tif\\.zip|SegArtery\\.zip|Seg\\.tif|Seg\\.tif\\.zip|Seg\\.zip)");
		if (dir.exists())
		{
			String[] fileNames = dir.list();
			for (String f: fileNames)
			{
				Matcher mat = seg.matcher(f);
				while (mat.find())
				{
					String segFN = mat.group();
					segPaths.add(dir.getAbsolutePath()+File.separator+segFN);
				}
			}
		}
		return segPaths;
	}
	
	public int getSubjectId() {
		return _subjectId;
	}
	public int getImageId() {
		return _imageId;
	}
	public int getSegmentationId() {
		return _segmentationId;
	}
	public int getCenterlineId() {
		return _centerlineId;
	}
	public int getTortuosityId() {
		return _tortuosityId;
	}
	public String getDirectory() {
		return _directory;
	}
	public String getFileName() {
		return _fileName;
	}
	public String toString()
	{
		String s = "SubjectID: "+_subjectId+" ArteryID: "+_arteryId+" ImageID: "+_imageId+" SegmentationID: "+_segmentationId+
		" CenterlineID: "+_centerlineId+" TortuosityID: "+_tortuosityId+"\nFile: "+_directory+
		File.separator+_fileName+"\n";
		if (_coordRange!=null)
			s = s+" "+_coordRange.toString();
		return s;
	}
	public CoordinateRange getCoordRange() {
		return _coordRange;
	}
	public void setCoordRange(CoordinateRange coordRange) {
		_coordRange = coordRange;
	}
	public String getChangedImage() {
		return _changedImage;
	}
	public void setChangedImage(String changedImage) {
		_changedImage = changedImage;
	}
	public float getXRes() {
		return _xRes;
	}
	public void setXRes(float res) {
		_xRes = res;
	}
	public float getYRes() {
		return _yRes;
	}
	public void setYRes(float res) {
		_yRes = res;
	}
	public float getZRes() {
		return _zRes;
	}
	public void setZRes(float res) {
		_zRes = res;
	}
	public String getPublicDir() {
		if (_publicDir == null)
		{
			this.segmentationPath();
		}
		return _publicDir;
	}
	public int getArteryId() {
		return _arteryId;
	}
	public void setArteryId(int arteryId) {
		_arteryId = arteryId;
	}
}

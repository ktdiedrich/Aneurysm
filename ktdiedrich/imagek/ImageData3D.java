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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ktdiedrich.db.Months;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.Opener;

/** Reading  2X interpolated image data is a 3D format. Tries to determine the
 * X, Y & Z resolutions by reading MRI DICOM or GE data files. GE data files use a platform specific 
 * binary. 
 * Define MRI brand constants which should match what's in the aneurysm.brand database table. 
 * @author Karl T. Diedrich <ktdiedrich@gmail.com>
 * */
public class ImageData3D
{   
	// Brands constants need to match values in database aneurysm.brand table
	public static final short GE = 1;
	public static final String GE_NAME = "GE 1.5";
	public static final short SIEMENS = 2;
	public static final String SIEMENS_NAME = "Siemens 3.0";
	// public static final float SIEMENS_MAG = 2.0f;
	
	
	// minimum length of DICOM filename to distinguish them from other files 
	public static final int DICOM_FILENAME_LENGTH = 40; 
    private boolean _display;
    private float _xRes;
    private float _yRes;
    private float _zRes;
    private float _xInt;
    private float _yInt;
    private float _zInt;
    private short _mriType;
    private int _subjectId;
	private String _sex;
    private String _examDate;
    private String _birthDate;
    private String _rdgehdr;
    private boolean _intelByteOrder;
    private float _patientSize;
    private float _patientWeight;
    private int _ageAtExam;
    public ImageData3D()
    {   
        _display = false;
        _rdgehdr = GEReader.GE_HEADER_READER;
        _intelByteOrder = false;
        _xInt = 1;
        _yInt = 1;
        _zInt = 1;
    }
    public ImagePlus getImage(String directory, String name)
	throws IOException, ParseException 
	{
    	return getImage(directory, name, true);
	}
    public ImagePlus getImage(String directory, String name, boolean readHeader)
    	throws IOException, ParseException 
    {
        
        FileInfo fi = new FileInfo();
        TextUtil tu = new TextUtil();
        int[] xyz = tu.getXYZ(name);
        
        ImagePlus image = null;
        if (name.substring(name.length()-3).equalsIgnoreCase(".gz"))
        {
        	fi.compression = FileInfo.LZW;
        	fi.width = xyz[0];
            fi.height = xyz[1];
            fi.nImages = xyz[2];
            fi.offset = 0;
            fi.gapBetweenImages = 0;
            fi.fileName = name;
            fi.intelByteOrder = _intelByteOrder;
            fi.directory = directory;
            fi.fileFormat = FileInfo.RAW;
            fi.fileType = FileInfo.GRAY16_UNSIGNED;
            FileOpener fo = new FileOpener(fi);
            image = fo.open(_display);
        }
        else
        {
        	image = IJ.openImage(directory+File.separator+name);
        }
        if (readHeader)
        {
        	readHeader(directory, image.getWidth(), image.getHeight(), image.getStackSize());
        }
        return image;
    }
    /** Display the open image immediately if true. */
    public boolean isDisplay()
    {
        return _display;
    }
    /** Display the open image immediately if true. */
    public void setDisplay(boolean display)
    {
        _display = display;
    }
    
    public int getSubjectId(String directory)
    {
    	int subjectId = 0;
    	Pattern subject = Pattern.compile("\\d+_\\d+\\/(\\d+)\\/");
    	Matcher mat = subject.matcher(directory);
    	while (mat.find())
    	{
    		IJ.log("SUBJECT: "+mat.group(1));
    		subjectId = Integer.parseInt(mat.group(1));
    	}
    	return subjectId;
    }
    public int mriType(String directory)
    {
    	int mriType = 0;
    	Pattern siemens = Pattern.compile("S(.+)");
    	Matcher mat = siemens.matcher(directory);
    	if (mat.find())
    	{
    		mriType = SIEMENS;
    	}
    	if (mriType == 0)
    	{
    		Pattern ge = Pattern.compile("[^SR]e(\\d+)");
    		mat = ge.matcher(directory);
    		if (mat.find())
    		{
    			mriType = GE;
    		}
    	}
    	return mriType;
    }
    /** Reads resolution from either DICOM from Siemens directories starting S######## or GE files from e#######
     * directories.  
     * @return x, y, z resolution from either a DICOM header or a platform dependent GE Image file reader
     * base on the directory name. */
    public void readHeader(String directory, int width, int height, int zSize)
    	throws IOException, ParseException 
    {
    	Matcher mat = null;
    	Pattern seriesPat = Pattern.compile("s(\\d+)");
    	mat = seriesPat.matcher(directory);
    	int series = 0;
    	if (mat.find())
    	{
    		series = Integer.parseInt(mat.group(1));
    	}
    	_mriType = 0;
    	Pattern siemens = Pattern.compile("S(.+)");
    	mat = siemens.matcher(directory);
    	if (mat.find())
    	{
    		_mriType = SIEMENS;
    		//System.out.println("Siemens");
    	}
    	GEReader geReader = null;
    	if (_mriType == 0)
    	{
    		Pattern ge = Pattern.compile("[^SR]e(\\d+)");
    		mat = ge.matcher(directory);
    		if (mat.find())
    		{
    			_mriType = GE;
    			int exam = Integer.parseInt(mat.group(1));
    			geReader = new GEReader(width, height, zSize, series, exam);
    			geReader.setRdgehdr(_rdgehdr);
    			//System.out.println("GE");
    		}
    	}
    	if (_mriType == 0)
    	{
    	    return;
    	}
    	
    	_subjectId = this.getSubjectId(directory);
    	
    	Pattern pub = Pattern.compile("public");
    	mat = pub.matcher(directory);
    	String privateDir = mat.replaceAll("private");
    	
    	Pattern pri = Pattern.compile("private");
    	mat = pri.matcher(directory);
    	String publicDir = mat.replaceAll("public");
    	
    	List<String> buildDirs = new LinkedList<String>();
    	buildDirs.add(publicDir);
    	buildDirs.add(privateDir);
    	
    	//System.out.println(dirName);
    	List<String> dirNames = new LinkedList<String>();
    	Pattern end = Pattern.compile(File.separator+"(s\\d+)");
    	for (String bd: buildDirs)
    	{
    		mat = end.matcher(bd);
    		String endDir = null;
    		while (mat.find())
    		{
    			endDir = mat.group(1);
    		}
    		Pattern clipEnd = Pattern.compile(endDir+File.separator+".+");
    		mat = clipEnd.matcher(bd);
    		bd = mat.replaceFirst(endDir);
    		dirNames.add(bd);
    		if (!bd.endsWith(File.separator)) bd = bd+File.separator;
    		dirNames.add(bd+"analysis");
    	}
    	
    	for (String dirName: dirNames)
    	{
    		IJ.log("LOOKING IN "+dirName);
    		File dir = new File(dirName);
    		if (dir.exists())
    		{
    			String[] fileNames = dir.list();
    			StringBuffer sb = new StringBuffer("FILENAMES: ");
    			for (String f: fileNames)
    				sb.append(f+" ");
    			sb.append("\n");
    			IJ.log(sb.toString());
		    	if (_mriType == SIEMENS)
		    	{
		    		IJ.log("Reading SIEMENS header");
		    		Pattern resXY = null;
		        	Pattern resZ = null;
		    		// Pixel Spacing: 0.29971590917969\0.29971590917969
		    		resXY = Pattern.compile("Pixel Spacing:\\s+(\\d+\\.\\d+)\\\\(\\d+\\.\\d+)");
		    		
		    		// Slice Thickness: 0.60000002384186
		    		resZ = Pattern.compile("Slice Thickness:\\s+(\\d+\\.\\d+|\\d+)");
		    		
		    		// 0008,0022  Acquisition Date: 20060330
		    		Pattern examDate = Pattern.compile("Acquisition Date:\\s+(\\d+)");
		    		// 0010,0030  Patient's Birth Date: 19480630
		    		
		    		Pattern birthDate = Pattern.compile("Patient's Birth Date:\\s+(\\d+)");
		    		
		    		// 0010,0040  Patient's Sex: F 
		    		Pattern sex = Pattern.compile("Patient's Sex:\\s+([MFmf])");
		    		
		    		// 0010,1020  Patient's Size: 1.6002032025
		    		Pattern patientSize = Pattern.compile("Patient's Size:\\s+(\\d+\\.\\d+)");
		    		
		    		// 0010,1030  Patient's Weight: 90.71848554 
		    		Pattern patientWeight = Pattern.compile("Patient's Weight:\\s+(\\d+\\.\\d+)");
		    		
		    		// 0010,1010  Patient's Age: 053Y
		    		Pattern ageAtExam = Pattern.compile("Patient's Age:\\s+(\\d+)");
		    		
		    		Pattern intFilePat = Pattern.compile("dbreconzfi.out.\\d+");
		    		Pattern intXpat = Pattern.compile("xZFI\\s+=\\s+(\\d+)");
		    		Pattern intYpat = Pattern.compile("yZFI\\s+=\\s+(\\d+)");
		    		Pattern intZpat = Pattern.compile("zZFI\\s+=\\s+(\\d+)");
		    		for (String fileName: fileNames)
		        	{
		    			mat = intFilePat.matcher(fileName);
		    			while (mat.find())
		    			{
		    				String intFile = mat.group();
		    				if (!dirName.endsWith(File.separator)) dirName = dirName+File.separator;
		    				File file = new File(dirName+intFile);
		    				BufferedReader reader = null;
		    				try
		    				{
		    					reader = new BufferedReader(new FileReader(file));
		    					String text = null;
		    				    // repeat until all lines is read
		    				    while ((text = reader.readLine()) != null)
		    				    {
		    				    	mat = intXpat.matcher(text);
		    				    	while (mat.find())
		        					{
		    				    		_xInt = Integer.parseInt(mat.group(1));
		        					}
		    				    	mat = intYpat.matcher(text);
		    				    	while (mat.find())
		        					{
		    				    		_yInt = Integer.parseInt(mat.group(1));
		        					}
		    				    	mat = intZpat.matcher(text);
		    				    	while (mat.find())
		        					{
		    				    		_zInt = Integer.parseInt(mat.group(1));
		        					}
		    				    }
		    				} 
		    				catch (FileNotFoundException e)
		    				{
		    					e.printStackTrace();
		    				} 
		    				catch (IOException e)
		    				{
		    					e.printStackTrace();
		    				} 
		    				finally
		    				{
		    					try
		    					{
		    						if (reader != null)
		    						{
		    							reader.close();
		    						}
		    					} 
		    					catch (IOException e)
		    					{
		    						e.printStackTrace();
		    					}
		    				}
		    			}
		    			// DICOM files typically have long names 
		        		if (fileName.length() > DICOM_FILENAME_LENGTH)
		        		{
		        			if (!dirName.endsWith(File.separator)) dirName = dirName+File.separator;
		        			String path = dirName+fileName;
		        			//System.out.println(path);
		        			ImagePlus my_image = (new Opener()).openImage( path );
		        			//System.out.println("Image: "+my_image);
		        			if (my_image != null)
		        			{
		        				String info = (String)my_image.getProperty( "Info" );
		        				if (info != null)
		        				{
			        				String[] lines = info.split("\n");
			        				for (String line: lines)
			        				{
			        					// IJ.log(line);
			        					mat = resXY.matcher(line);
			        					while (mat.find())
			        					{
			        						if (_xRes == 0) _xRes = Float.parseFloat(mat.group(1))/_xInt;
			        						if (_yRes == 0) _yRes = Float.parseFloat(mat.group(2))/_yInt;
			        						// System.out.println("x="+sX+", y="+sY);
			        					}
			        					mat = resZ.matcher(line);
			        					while (mat.find())
			        					{
			        						if (_zRes == 0)_zRes = Float.parseFloat(mat.group(1))/_zInt;
			        						//System.out.println("z="+sZ);
			        					}
			        					mat = examDate.matcher(line);
			        					while (mat.find())
			        					{
			        						String d = mat.group(1);
			        						//IJ.log("Exam Date: "+d);
			        						if (_examDate == null) _examDate = d;
			        					}
			        					mat = birthDate.matcher(line);
			        					while (mat.find())
			        					{
			        						String d = mat.group(1);
			        						//IJ.log("Birth Date: "+d);
			        						if (_birthDate == null) _birthDate = d;
			        					}
			        					mat = sex.matcher(line);
			        					while (mat.find())
			        					{
			        						String f = mat.group(1);
			        						//IJ.log("Sex: "+f+".");
			        						if (f.equalsIgnoreCase("F"))
			        							if (_sex==null) _sex = "female";
			        						if (f.equalsIgnoreCase("M"))
			        						{
			        							if (_sex==null) 
			        							{
			        								_sex = "male";
			        							}
			        						}
			        					}
			        					mat = patientSize.matcher(line);
			        					while (mat.find())
			        					{
			        						_patientSize = Float.parseFloat( mat.group(1) );
			        					}
			        					mat = patientWeight.matcher(line);
			        					while (mat.find())
			        					{
			        						_patientWeight = Float.parseFloat( mat.group(1) );
			        					}
			        					mat = ageAtExam.matcher(line);
			        					while (mat.find())
			        					{
			        						_ageAtExam = Integer.parseInt( mat.group(1) );
			        						IJ.log("Age at exam: "+_ageAtExam);
			        					}
			        				}
		        				}
		        			}
		        		}
		        		if (_xRes > 0 && _yRes > 0 && _zRes> 0 && _sex!=null && _birthDate!=null && _examDate!=null)
		        		{
		        			// System.out.println("("+xyz[0]+", "+xyz[1]+", "+xyz[2]+")");
		        			break;
		        		}
		        	}
		    	}
		    	else if (_mriType == GE)
		    	{
		    		// save reader between directories 
		    		IJ.log("Reading GE image header");
		        	for (String fileName: fileNames)
		        	{
		        		geReader.readFile(dirName, fileName);
		        	}
		        	_xRes = geReader.getXRes();
		        	_yRes = geReader.getYRes();
		        	_zRes = geReader.getZRes();
		        	_sex = geReader.getSex();
		        	_birthDate = geReader.getBirthDate();
		        	_examDate = geReader.getExamDate();
		        	_string = geReader.toString();
		    	}
    		}
    	}
    }
    public String toString()
    {
    	return _string;
    }
    private String _string;
	public float getXRes() 
	{
		return _xRes;
	}
	public float getYRes()
	{
		return _yRes;
	}
	public float getZRes()
	{
		return _zRes;
	}
	/** MRI types defined a constants in this class. */
	public short getMRIType() 
	{
		return _mriType;
	}
	public String getSex() 
	{
		return _sex;
	}
	public String getExamDate() 
	{
		return _examDate;
	}
	public String getBirthDate()
	{
		return _birthDate;
	}
	public int getSubjectId()
	{
		return _subjectId;
	}
	public String getRdgehdr() {
		return _rdgehdr;
	}
	public void setRdgehdr(String rdgehdr) {
		_rdgehdr = rdgehdr;
	}
	public boolean isIntelByteOrder() {
		return _intelByteOrder;
	}
	public void setIntelByteOrder(boolean intelByteOrder) {
		_intelByteOrder = intelByteOrder;
	}
	public float getPatientSize() {
		return _patientSize;
	}
	public float getPatientWeight() {
		return _patientWeight;
	}
	public int getAgeAtExam() {
		return _ageAtExam;
	}
	
	
}

class GEData
{
	public static final float Z_CUT_OFF = 0.5f;
	public GEData(int zSize, int series)
	{
		_zSize = zSize;
		_directorySeries = series;
	}
	private String _filename;
	private int _zSize;
	private int _directorySeries;
	private int _headerSeries;
	private float _sliceThickness;
	private float _scanSpacing;
	private int _matrixSizeX;
	private int _matrixSizeY;
	private float _pixelSizeX;
	private float _pixelSizeY;
	private int _rhnslices;
	private String _sex;
	private String _examDate;
	private String _birthDate;
	
	
	/** Check if the directory series matches the header series, then the header series results 
	 * can be used. 
	 * @return true if directory series matches header series, false otherwise*/
	public boolean isMatchingSeries()
	{
		return (_directorySeries == _headerSeries);
	}
	public String getSex() {
		return _sex;
	}
	public void setSex(String sex) {
		_sex = sex;
	}
	public String getBirthDate() {
		return _birthDate;
	}
	public void setBirthDate(String birthDate) {
		_birthDate = birthDate;
	}
	public String getExamDate() {
		return _examDate;
	}
	public void setExamDate(String examDate) {
		_examDate = examDate;
	}
	
	public float getSliceThickness() {
		return _sliceThickness;
	}
	public void setSliceThickness(float sliceThickness) {
		_sliceThickness = sliceThickness;
	}
	public float getScanSpacing() {
		return _scanSpacing;
	}
	public void setScanSpacing(float scanSpacing) {
		_scanSpacing = scanSpacing;
	}
	public int getMatrixSizeX() {
		return _matrixSizeX;
	}
	public void setMatrixSizeX(int matrixSizeX) {
		_matrixSizeX = matrixSizeX;
	}
	public int getMatrixSizeY() {
		return _matrixSizeY;
	}
	public void setMatrixSizeY(int matrixSizeY) {
		_matrixSizeY = matrixSizeY;
	}
	public float getPixelSizeX() {
		return _pixelSizeX;
	}
	public void setPixelSizeX(float pixelSizeX) {
		_pixelSizeX = pixelSizeX;
	}
	public float getPixelSizeY() {
		return _pixelSizeY;
	}
	public void setPixelSizeY(float pixelSizeY) {
		_pixelSizeY = pixelSizeY;
	}
	public int getRhnslices() {
		return _rhnslices;
	}
	public void setRhnslices(int rhnslices) {
		_rhnslices = rhnslices;
	}
	
	
	public String toString()
	{
		float zRes = (_sliceThickness + _scanSpacing);
		
		return _filename+": Directory series="+_directorySeries+" Header series="+_headerSeries + 
			" (sliceThickness "+_sliceThickness+"+ scanSpacing "+_scanSpacing+" = "+zRes+
				"), rhnslices="+_rhnslices+" zSize="+_zSize+ " Z-interpolation= "+this.isZinterpolated()+
				" XRes="+getXRes()+" YRes="+getYRes()+" ZRes="+getZRes();
			
	}
	/** If rhnslices value is close to the image stack zSize don't divide Z resolution 
	 * by the readstep if not divide Z resolution by the readstep if it's greater than 1 
	 * @return Z-resolution Add Slice_Thickness + Scan_Spacing modified by readstep. */
	public float getZRes()
	{
		float zRes = (_sliceThickness + _scanSpacing);
		return zRes;
	}
	public boolean isZinterpolated()
	{
		return (_rhnslices > 0 && _rhnslices < _zSize); 
	}
	/**  */
	public float getXRes()
	{
		
		float xRes = _pixelSizeX;
		
		return xRes;
	}
	/**  */
	public float getYRes()
	{
		
		float yRes = _pixelSizeY;
		
		return yRes;
	}
	
	public int getDirectorySeries() {
		return _directorySeries;
	}
	public void setDirectorySeries(int directorySeries) {
		_directorySeries = directorySeries;
	}
	public int getHeaderSeries() {
		return _headerSeries;
	}
	public void setHeaderSeries(int headerSeries) {
		_headerSeries = headerSeries;
	}
    public String getFilename()
    {
        return _filename;
    }
    public void setFilename(String filename)
    {
        _filename = filename;
    }
    
}

class GEReader
{
	public static final String GE_HEADER_READER="/usr/local/mr/bin.x86/rdgehdr";
	private Pattern _fileEI;
	private Pattern _finalFile;
	private Pattern _finalFileX;
	private Pattern _finalFileY;
	private Pattern _finalFileZ;
	private Pattern _rhn;
	private Pattern _sireExamPat;
	private Pattern _series;
	private Pattern _slice;
	private Pattern _spacing;
	private Pattern _pixelX;
	private Pattern _pixelY;
	private Pattern _matrixX;
	private Pattern _matrixY;
	private Pattern _step;
	private Pattern _sex;
	private Pattern _examDate;
	private Pattern _birthDate;
	private Pattern _xpixelsize; 	
	private Pattern _ypixelsize; 
	private Pattern _sireSlicethickPat; 
	private Pattern _sireScanSpacingPat;
	private Pattern _imageFiles;
	private int _finalX;
	private int _finalY;
	private int _finalZ;
	private int _exam;
	private int _width, _height, _zSize;
	private List<Sire> _sires; 
	private String _rdgehdr;
	private int _incompleteReadStep;
	
	private GEData _data;
	private GEData _dataP;
	
	public GEReader(int width, int height, int zSize, int series, int exam)
	{
		_width = width;
		_height = height;
		_zSize = zSize;
		_sires = new LinkedList<Sire>();
		_exam = exam;
		_data = new GEData(zSize, series);
		_dataP = new GEData(zSize, series);
		_incompleteReadStep = 0;
		// GE filename examples:  e9947s2i1 E#### I.##### P09728.7
		// TODO don't match .gz .tif .gif 
		_fileEI = Pattern.compile("^e.+|^E.+|^I.+|^P.+|sire.inp.+|sire.runtime.+|zcf.+");
		_imageFiles = Pattern.compile("gz$|GZ$|gif$|GIF$|jpg$|JPG$|tif$|TIF$|tiff$|TIFF$");
		// finalz120y768x1024.img.gz
		_finalFile = Pattern.compile("final.+");
		_finalFileX = Pattern.compile(".+x(\\d+)");
		_finalFileY = Pattern.compile(".+y(\\d+)");
		_finalFileZ = Pattern.compile(".+z(\\d+)");
		
		//_headerPats = new LinkedList<Pattern>();
		
		// rhnslices slices in a pass (0-256): 128
	    _rhn = Pattern.compile(".+(rhnslices\\sslices.+):\\s+(\\d+)");
	    //_headerPats.add(_rhn);
	    
	    // newexamnum 		 = 4549
		_sireExamPat = Pattern.compile("(newexamnum)\\s+=\\s+(\\d+)");
		//_headerPats.add(_headerExam);
		
		// ...Series Number: 4
		_series = Pattern.compile(".+(Series\\sNumber):\\s+(\\d+)");
		//_headerPats.add(_series);
		
		// Slice Thickness (mm): 0.9
		//_slice = Pattern.compile(".+(Slice\\sThickness).+:\\s+(\\d+\\.\\d+)");
		_slice = Pattern.compile("(Slice\\sThickness).+:\\s+(\\d+\\.?\\d*)");
		//_headerPats.add(_slice);
		
		// Spacing between scans (mm?): -0.45
		_spacing = Pattern.compile(".+(Spacing\\sbetween\\sscans).+:\\s+(-?\\d+\\.?\\d*)");
		//_headerPats.add(_spacing);
		
		_pixelX = Pattern.compile(".+(Image\\spixel\\ssize\\s-\\sX):\\s(\\d+\\.\\d+)");
		//_headerPats.add(_pixelX);
		
		
		_pixelY = Pattern.compile(".+(Image\\spixel\\ssize\\s-\\sY):\\s(\\d+\\.\\d+)");
		//_headerPats.add(_pixelY);
		
		// Image matrix size - X: 512
		_matrixX = Pattern.compile(".+(Image\\smatrix\\ssize\\s-\\sX):\\s(\\d+)");
		//_headerPats.add(_matrixX);
		
		_matrixY = Pattern.compile(".+(Image\\smatrix\\ssize\\s-\\sY):\\s(\\d+)");
		//_headerPats.add(_matrixY);
		
		
		_step = Pattern.compile("(readstep)\\s+=\\s+(\\d+)");

		// ...Patient Sex: 2
		_sex = Pattern.compile(".+(Patient Sex):\\s+(\\d)");
		// ...Exam date/time stamp: (1062241331) Sat Aug 30 05:02:11 2003
		_examDate = Pattern.compile(
				".+(Exam\\sdate\\/time\\sstamp):\\s+\\(\\d+\\)\\s+\\w+\\s+(\\w+)\\s+(\\d+)\\s+\\d+:\\d+:\\d+\\s+(\\d+)");
		// ...Date of Birth: 19340504
		_birthDate = Pattern.compile(".+(Date\\sof\\sBirth):\\s(\\d+)");
		// ...Magnet strength (in gauss): 15000

		//xpixelsize 		 = 0.302734
		_xpixelsize = Pattern.compile("(xpixelsize)\\s+=\\s+(\\d\\.\\d+)");
		//ypixelsize 		 = 0.302734
		_ypixelsize = Pattern.compile("(ypixelsize)\\s+=\\s+(\\d\\.\\d+)");
		//slicethick 		 = 0.600000
		_sireSlicethickPat = Pattern.compile("(slicethick)\\s+=\\s+(\\d\\.\\d+)");
		// scanspacing          = -0.500000
		// scanspacing 		 = -0.500000
		_sireScanSpacingPat = Pattern.compile("(scanspacing)\\s+=\\s+(-?\\d+\\.\\d+)");
	}
	public boolean isMatchingExam()
	{
		if (_sires.size() > 0)
		{
			Sire s =  _sires.get(0);
			if (s == null)
				return false;
			return (_exam == s.getSireExam());
		}
		return false;
	}
	/** Tries to return the best readstep value */
	public int getReadStep()
	{
		int readStep = 0;
		if (_sires.size() > 0 )
		{
			readStep = _sires.get(0).getSireReadStep();
		}
		else
		{
			readStep = _incompleteReadStep;
		}
		return readStep;
	}
	/** @return X, Y Z resolutions. Picks the P resolutions over the E resolutions. */
	public float getXRes()
	{
		float x = 0.0f;
		GEData data = null;
		if (_dataP.isMatchingSeries() && _dataP.getXRes() > 0)
			data = _dataP;
		else if (_data.isMatchingSeries())
			data = _data;
			
		if (this.isMatchingExam() &&  _sires.get(0).getSireXpixelsize() > 0.0f) 
		{
			x = _sires.get(0).getSireXpixelsize();
			//if (this.getReadStep() < 2 && _finalX == 1024)
			//if (this.getReadStep() < 2 && (_finalX > _width))
			//	x = x/2.0f;
		}
		// check if readstep missed 2x interpolation
		else if (data != null)
		{
			x = data.getXRes();
			// if (this.getReadStep() >= 2 || _finalX == 1024)
			//if (this.getReadStep() >= 2 || (_finalX > _width))
			if (this.getReadStep() >=2)
			{
				x = x/2.0f;
			}
		}
		return x;
	}
	
	public float getYRes()
	{
		GEData data = null;
		if (_dataP.isMatchingSeries() && _dataP.getXRes() > 0)
			data = _dataP;
		else if (_data.isMatchingSeries())
			data = _data;
		
		float y = 0.0f;
		if (this.isMatchingExam() && _sires.get(0).getSireYpixelsize() > 0.0f)
		{
			y =  _sires.get(0).getSireYpixelsize();
			//if (this.getReadStep() < 2 && _finalY == 768)
			//if (this.getReadStep() < 2 && (_finalY > _height))
			//	y = y/2.0f;
		}
		
		// check if readstep missed 2x interpolation
		else if (data != null)
		{
			y =  data.getYRes();
			// if (this.getReadStep() >= 2 || _finalY == 768)
			//if (this.getReadStep() >= 2 || (_finalY > _height))
			if (this.getReadStep() >= 2)
			{
				y = y/2.0f;
			}
		}
		return y;
	}	
	
	public float getZRes()
	{
		GEData data = null;
		if (_dataP.isMatchingSeries() && _dataP.getZRes() > 0)
			data = _dataP;
		else if (_data.isMatchingSeries())
			data = _data;
		
		float zRes = 0.0f;
		Sire s = null;
		if (_sires.size() > 0)
		{
			s = _sires.get(0);
		}
		if (s != null && isMatchingExam() && s.getSireSlicethick() > 0.0f ) 
		{
			zRes = (s.getSireSlicethick()+s.getSireScanspacing());
			//if (this.getReadStep() < 2 && data.isZinterpolated())
			//	zRes = zRes/2.0f;
		}
		else if (data != null)
		{
			zRes = data.getZRes();
			if (data.isZinterpolated())
				zRes =  zRes/2.0f;
		}
		
		return zRes;
	}
	public String toString()
	{
		GEData data = null;
		if (_dataP.isMatchingSeries() && _dataP.getZRes() > 0)
			data = _dataP;
		else if (_data.isMatchingSeries())
			data = _data;
		
		StringBuffer sireBuf = new StringBuffer();
		for(Sire s: _sires)
		{
			sireBuf.append(s.toString());
			sireBuf.append("\n");
		}
		String headerReport = "none";
		if (data != null) headerReport = data.toString();
		GEData other = null;
		if (data == _dataP)
			other = _data;
		else if (data == _data)
			other = _dataP;
		return "Using header: "+data+
			"\nOther header: "+other+
			"\nfinal x="+_finalX+" final y="+_finalY+" final z="+_finalZ+
			" width="+_width+" height="+_height+" zSize="+_zSize+
			", readstep="+this.getReadStep()+"\n"+sireBuf.toString();
	}
	public String getSex()
	{
		String s = _dataP.getSex();
		if (s != null)
			return s;
		else
		{
			return _data.getSex();
		}
	}
	public String getBirthDate()
	{
		String d = _dataP.getBirthDate();
		if (d!=null)
			return d;
		else
		{
			return _data.getBirthDate();
		}
	}
	
	public String getExamDate()
	{
		String d = _dataP.getExamDate();
		if (d!=null)
			return d;
		else
		{
			return _data.getExamDate();
		}
	}
	public void readFile(String dirName, String fileName)
		throws IOException, ParseException
	{
		
		// check for GE Header Reader
		File reader = new File(_rdgehdr);
		if (reader.exists() == false)
		{
			IJ.log(reader.getAbsolutePath()+" not found");
			return;
		}
		Matcher mat = null;
		mat = _finalFile.matcher(fileName);
		while(mat.find())
		{
			Matcher sm = _finalFileX.matcher(fileName);
			while (sm.find())
			{
				_finalX = Integer.parseInt(sm.group(1));
			}
			sm = _finalFileY.matcher(fileName);
			while (sm.find())
			{
				_finalY = Integer.parseInt(sm.group(1));
			}
			sm = _finalFileZ.matcher(fileName);
			while (sm.find())
			{
				_finalZ = Integer.parseInt(sm.group(1));
			}
		}
		
		mat = _fileEI.matcher(fileName);
		while (mat.find())
		{
			String geName = mat.group();
			Matcher fMat = _imageFiles.matcher(geName);
			if (!fMat.find())
			{			
				if (!dirName.endsWith(File.separator)) dirName = dirName+File.separator;
				String geFullPath = dirName+geName;
				IJ.log("FILENAME: "+geName);
				Runtime run = Runtime.getRuntime();
				char first = geName.charAt(0);
				BufferedReader bufferedReader = null;
				String line = null;
				if (first == 'e' || first == 'E' || first == 'I'|| first == 'P') // GE headers
				{
					GEData data = null;
					if (first == 'P')
	            	{
	            		data = _dataP;
	            	}
	            	else
	            	{
	            		data = _data;
	            	}
					data.setFilename(fileName);
					String exec = _rdgehdr+" "+geFullPath;
					IJ.log("EXECUTE: "+exec);
					Process proc = run.exec(exec);
				
					InputStream inputstream =
		                proc.getInputStream();
		            InputStreamReader inputstreamreader =
		                new InputStreamReader(inputstream);
		            try
		            {
		            	bufferedReader = new BufferedReader(inputstreamreader);
		            	while ((line = bufferedReader.readLine()) != null) 
		            	{
		            		// System.out.println(line);
		            		mat = _rhn.matcher(line);
	            			while (mat.find())
	            			{
	            				data.setRhnslices(Integer.parseInt(mat.group(2)));
	            				IJ.log(mat.group(1)+" = "+data.getRhnslices());
	            			}
		            		
	            			mat = _slice.matcher(line);
	            			while (mat.find())
	            			{
	            				float thickness = Float.parseFloat(mat.group(2));
	            				// ignore 0 slice thickness 
	            				if (thickness > 0.0)
	            				{
	            					data.setSliceThickness(thickness);
	            					IJ.log(mat.group(1)+" = "+data.getSliceThickness());
	            				}
	            			}
	            			
	            			mat = _spacing.matcher(line);
	            			while (mat.find())
	            			{
	            				data.setScanSpacing(Float.parseFloat(mat.group(2)));
	            				IJ.log(mat.group(1)+" = "+data.getScanSpacing());
	            			}
	            			
	            			mat = _matrixX.matcher(line);
	            			while (mat.find())
	            			{
	            				data.setMatrixSizeX(Integer.parseInt(mat.group(2)));
	            				IJ.log(mat.group(1)+" = "+data.getMatrixSizeX());
	            			}
	            			
	            			mat = _matrixY.matcher(line);
	            			while (mat.find())
	            			{
	            				data.setMatrixSizeY(Integer.parseInt(mat.group(2)));
	            				IJ.log(mat.group(1)+" = "+data.getMatrixSizeY());
	            			}
	            			
	            			mat = _pixelX.matcher(line);
	            			while (mat.find())
	            			{
	            				data.setPixelSizeX(Float.parseFloat(mat.group(2)));
	            				IJ.log(mat.group(1)+" = "+data.getPixelSizeX());
	            			}
	            			
	            			mat = _pixelY.matcher(line);
	            			while (mat.find())
	            			{
	            				data.setPixelSizeY(Float.parseFloat(mat.group(2)));
	            				IJ.log(mat.group(1)+" = "+data.getPixelSizeY());
	            			}	
	            			
	            			mat = _sex.matcher(line);
	            			while (mat.find())
	            			{
	            				int s = Integer.parseInt(mat.group(2));
	            				if (s ==1)
	            					data.setSex("male");
	            				else if (s==2)
	            					data.setSex("female");
	            				IJ.log(mat.group(1)+" = "+data.getSex()+": "+s);
	            			}
	            			mat = _examDate.matcher(line);
	            			while (mat.find())
	            			{
	            				String mon = Months.threeLetter2twoNumber(mat.group(2));
	            				String dayOfmon = mat.group(3);
	            				String year = mat.group(4);
	            				String datestamp = year+mon+dayOfmon;
	            				data.setExamDate(datestamp);
	            				IJ.log(mat.group(1)+" = "+data.getExamDate()+": "+datestamp);
	            			}
	            			mat = _birthDate.matcher(line);
	            			while (mat.find())
	            			{
	            				String d = mat.group(2);
	            				data.setBirthDate(d);
	            				IJ.log(mat.group(1)+" = "+data.getBirthDate()+": "+d);
	            			}
	            			mat = _series.matcher(line);
	            			while (mat.find())
	            			{
	            				int headerSeries = Integer.parseInt(mat.group(2));
	            				data.setHeaderSeries(headerSeries);
	            				IJ.log(mat.group(1)+" = "+data.getHeaderSeries());
	            			}
		            	}
		            }
		            finally
		            {
		            	bufferedReader.close();
		            }
				}
				else if (geName.substring(0, 3).equals("zcf") )
				{
					try 
					{
				        bufferedReader = new BufferedReader(new FileReader(geFullPath));
				        
				        while ((line = bufferedReader.readLine()) != null) 
				        {
				        	//System.out.println(line);
				        	// look for the name of a final file that may be removed 
				        	mat = _finalFile.matcher(line);
				    		while(mat.find())
				    		{
				    			Matcher sm = _finalFileX.matcher(line);
				    			while (sm.find())
				    			{
				    				_finalX = Integer.parseInt(sm.group(1));
				    			}
				    			sm = _finalFileY.matcher(line);
				    			while (sm.find())
				    			{
				    				_finalY = Integer.parseInt(sm.group(1));
				    			}
				    			sm = _finalFileZ.matcher(line);
				    			while (sm.find())
				    			{
				    				_finalZ = Integer.parseInt(sm.group(1));
				    			}
				    		}
		            		
				        }
				        
				    } 
					finally
					{
						bufferedReader.close();
					}
					
				}
				else if (first == 's') // text file 
				{
					try 
					{
				        bufferedReader = new BufferedReader(new FileReader(geFullPath));
				        boolean foundSireScanspacing = false; // scan spacing can be 0 
				        
				        int sireReadStep = 0;
				        float sireXpixelsize = 0;
				        float sireYpixelsize = 0;
				        int sireExam = 0;
				        float sireSlicethick = 0;
				        float sireScanspacing = 0;
				        while ((line = bufferedReader.readLine()) != null) 
				        {
				        	//System.out.println(line);
				        	mat = _step.matcher(line);
		            		while (mat.find())
		            		{
		            			sireReadStep = Integer.parseInt(mat.group(2));
		            			
		            			IJ.log("FOUND: "+mat.group(1)+" = "+sireReadStep);
		            		}
		            		mat = _xpixelsize.matcher(line);  
		            		while (mat.find())
		            		{
		            			sireXpixelsize = Float.parseFloat(mat.group(2));
		            			IJ.log("FOUND: "+mat.group(1)+" = "+sireXpixelsize);
		            		}
		            		mat = _ypixelsize.matcher(line);
		            		while (mat.find())
		            		{
		            			sireYpixelsize = Float.parseFloat(mat.group(2));
		            			IJ.log("FOUND: "+mat.group(1)+" = "+sireYpixelsize);
		            		}
		            		mat = _sireExamPat.matcher(line);
		            		while (mat.find())
		            		{
		            			sireExam = Integer.parseInt(mat.group(2));
		            			IJ.log("FOUND: "+mat.group(1)+" = "+sireExam);
		            			
		            		}
		            		mat = _sireSlicethickPat.matcher(line);
		            		while (mat.find())
		            		{
		            			sireSlicethick = Float.parseFloat(mat.group(2));
		            			IJ.log("FOUND: "+mat.group(1)+" = "+sireSlicethick);
		            			
		            		}
		            		mat = _sireScanSpacingPat.matcher(line);
			                while (mat.find())
			                {
			                     sireScanspacing = Float.parseFloat(mat.group(2));
			                     IJ.log("FOUND: "+mat.group(1)+" = "+sireScanspacing);
			                     foundSireScanspacing = true;
		            		}
				        }
				        _incompleteReadStep = sireReadStep;
				        if ( sireReadStep > 0 && sireXpixelsize > 0 && sireYpixelsize > 0 && 
				        		sireExam > 0 && sireSlicethick > 0 && foundSireScanspacing)
				        {
				        	Sire s = new Sire(sireReadStep, 
				        			sireXpixelsize, 
				        			sireYpixelsize,
				        			sireExam,
				        			sireSlicethick,
				        			sireScanspacing);
				        	s.setFilename(geName);
				        	_sires.add(s);
				        }
				    } 
					finally
					{
						bufferedReader.close();
					}
					
				}
			}
		}
	}
	public String getRdgehdr() {
		return _rdgehdr;
	}
	public void setRdgehdr(String rdgehdr) {
		_rdgehdr = rdgehdr;
	}
}

/** sire file data on GE reconstruction. */
class Sire
{
	private String _filename;
	private int _sireReadStep, _sireExam;
	private float _sireXpixelsize, _sireYpixelsize, _sireSlicethick, _sireScanspacing;
	public Sire(int sireReadStep, float sireXpixelsize, float sireYpixelsize, int sireExam,
			float sireSlicethick, float sireScanspacing)
	{
		_sireReadStep = sireReadStep;
		_sireXpixelsize = sireXpixelsize;
		_sireYpixelsize = sireYpixelsize;
		_sireExam = sireExam;
		_sireSlicethick = sireSlicethick;
		_sireScanspacing = sireScanspacing;
	}
	public String toString()
	{
		return _filename+":"+" readstep="+_sireReadStep+" xpixelsize="+_sireXpixelsize+
			" ypixelsize="+_sireYpixelsize+" exam="+_sireExam+" (slicethick: "+_sireSlicethick+
			" scanspacing: "+_sireScanspacing+ " = "+(_sireSlicethick+_sireScanspacing)+")";
	}
	public int getSireReadStep() {
		return _sireReadStep;
	}
	public void setSireReadStep(int sireReadStep) {
		_sireReadStep = sireReadStep;
	}
	public int getSireExam() {
		return _sireExam;
	}
	public void setSireExam(int sireExam) {
		_sireExam = sireExam;
	}
	public float getSireXpixelsize() {
		return _sireXpixelsize;
	}
	public void setSireXpixelsize(float sireXpixelsize) {
		_sireXpixelsize = sireXpixelsize;
	}
	public float getSireYpixelsize() {
		return _sireYpixelsize;
	}
	public void setSireYpixelsize(float sireYpixelsize) {
		_sireYpixelsize = sireYpixelsize;
	}
	public float getSireSlicethick() {
		return _sireSlicethick;
	}
	public void setSireSlicethick(float sireSlicethick) {
		_sireSlicethick = sireSlicethick;
	}
	public float getSireScanspacing() {
		return _sireScanspacing;
	}
	public void setSireScanspacing(float sireScanspacing) {
		_sireScanspacing = sireScanspacing;
	}
	public String getFilename() {
		return _filename;
	}
	public void setFilename(String filename) {
		_filename = filename;
	}
}

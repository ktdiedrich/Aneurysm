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
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Makes an image accumulating centerlines and writes them in summed intensity on top of the 
 * segmentation. 
 * @author ktdiedrich@gmail.com 
 * */
public class AccumulateCenterlines 
{
	public static final int BACKGROUND = 25;
	private ImagePlus _centImage;
	private ImageStack _centStack;
	private short[][] _centVoxels;
	private short[][] _posVoxels;
	private short _posCentVal;
	private int _maxCentAcc;
	private int _posCentCount;
	private ImagePlus _bifImage;
	private ImageStack _bifStack;
	private short[][] _bifVoxels;
	private int _maxPossible;
	private short _centMin;
	private short _centMax;
	private short _bifMin, _bifMax;
	private int _width, _height, _zSize;
	private List<Graph> _segmentation;
	private boolean _centBackgroundAdded;
	private boolean _bifBackgroundAdded;
	private ImagePlus _accuracyIm;
	private ImageProcessor[] _accuracyProcs;
	private double _cumAccuray;
	private double _cumSSE;
	private double _cumMSSE;
	private double _cumRMSE;
	
	private double _cumTrueAccuray;
	private double _cumTrueSSE;
	private double _cumTrueMSSE;
	private double _cumTrueRMSE;
	
	private VoxelDistance _vd;
	private List<Position> _posCent;
	private List<DPoint> _trueCent;
	
	private String _fileBaseName;
	
	public AccumulateCenterlines(List<Graph> segmentation, int width, int height, int zSize, String title, 
			ImagePlus positiveControlIm, VoxelDistance vd)
	{
		_vd=vd;
		_maxPossible = 0;
		_maxCentAcc = 0;
		_centBackgroundAdded = false;
		_bifBackgroundAdded = false;
		_segmentation = segmentation;
		_centMin = Short.MAX_VALUE;
		_centMax = 0;
		_bifMin = Short.MAX_VALUE;
		_bifMax = 0;
		_width = width;
		_height = height;
		_zSize = zSize;
		
		_posCent = this.positiveCenterlineControl(positiveControlIm, title);
		_centVoxels = new short[zSize][];
		_bifVoxels = new short[zSize][];
		_centStack = new ImageStack(width, height);
		_bifStack  = new ImageStack(width, height);
		for (int i=0; i < zSize; i++)
		{
			ImageProcessor centIp = new ShortProcessor(width, height);
			_centVoxels[i] = (short[])centIp.getPixels();
			_centStack.addSlice(""+i, centIp);
			
			ImageProcessor bifIp = new ShortProcessor(width, height);
			_bifVoxels[i] = (short[])bifIp.getPixels();
			_bifStack.addSlice(""+i, bifIp);
		}
		
		
		_centImage = new ImagePlus(title+"CentStability", _centStack);
		_bifImage = new ImagePlus(title+"BifStability", _bifStack);
	}
	/** Extract the centerline from a positive control image.
	 * @return list of centerline positions. */
	private List<Position> positiveCenterlineControl(ImagePlus pImage, String title)
	{
		List<Position> posCent = new LinkedList<Position>();
		if (pImage != null)
		{
			IJ.log("Positive  control image");
			_posVoxels = ImageProcess.getShortStackVoxels(pImage.getImageStack());
			_posCentVal = 0;
			_posCentCount = 0;
			
			for (int z=0; z < _posVoxels.length; z++)
			{
				for (int x=0; x < _width; x++)
				{
					for (int y=0; y<_height; y++)
					{
						int i = y*_width+x;
						if (_posVoxels[z][i] > _posCentVal)
						{
							_posCentVal = _posVoxels[z][i];
						}
					}
				}
			}
			IJ.log("posCentVal: "+_posCentVal);
			
			for (int z=0; z < _zSize; z++)
			{
				for (int x=0; x < _width; x++)
				{
					for (int y=0; y < _height; y++)
					{
						int i = y*_width+x;
						if (_posVoxels[z][i] == _posCentVal) 
						{
							_posCentCount++;
							Position pos = new Position(x, y, z);
							posCent.add(pos);
						}
						
					}
				}
			}
			ImageStack accStack = new ImageStack(_width, _height);
			_accuracyProcs = new ImageProcessor[_zSize];
			for (int z=0; z < _zSize; z++)
			{
				ImageProcessor ip = new ColorProcessor(_width, _height);
				accStack.addSlice(z+"", ip);
				_accuracyProcs[z] = ip;
			}
			_accuracyIm = new ImagePlus(title+"Accuracy", accStack);
		}
		return posCent;
	}
	/** Adds BACKGROUND value to segmentation voxels including centerlines. */
	private void addCentBackground()
	{
		
		if (_centBackgroundAdded == false)
		{
			IJ.log("Adding background to segmentation: "+BACKGROUND);
			for (Graph g: _segmentation)
			{
				for (GraphNode n: g.getNodes())
				{
					short val = _centVoxels[n.z][n.row*_width+n.col];
					_centVoxels[n.z][n.row*_width+n.col] = (short)(val+BACKGROUND);
				}
			}
			_centBackgroundAdded = true;
		}
	}
	private void addBifBackground()
	{
		
		if (_bifBackgroundAdded == false)
		{
			IJ.log("Adding background to segmentation: "+BACKGROUND);
			for (Graph g: _segmentation)
			{
				for (GraphNode n: g.getNodes())
				{
					short valB = _bifVoxels[n.z][n.row*_width+n.col];
					_bifVoxels[n.z][n.row*_width+n.col] = (short)(valB+BACKGROUND);
				}
			}
			_bifBackgroundAdded = true;
		}
	}
	public void addCenterline(List<CenterlineGraph> centerlineGraphs)
	{
		_maxPossible++;
		int thisCentPos = 0;
		Set<GraphNode> added = new HashSet<GraphNode>();
		short[][] thisCentVoxels = new short[_zSize][_width*_height];
		for (CenterlineGraph g: centerlineGraphs)
		{
			for (Centerline c: g.getCenterlines())
			{
				for (GraphNode cn: c.getCenterlineNodes())
				{
					if (!added.contains(cn))
					{
						added.add(cn);
						int i = cn.row*_width+cn.col;
						short curVal = _centVoxels[cn.z][i];
						short newVal = (short)(curVal+1);
						if (newVal > _centMax) _centMax = newVal;
						if (newVal < _centMin) _centMin = newVal;
						_centVoxels[cn.z][cn.row*_width+cn.col] = newVal;
						thisCentVoxels[cn.z][cn.row*_width+cn.col] = 1;
						if (cn.isBifurcation)
						{
							short curValB = _bifVoxels[cn.z][i];
							short newValB = (short)(curValB+1);
							if (newValB > _bifMax) _bifMax = newValB;
							if (newValB < _bifMin) _bifMin = newValB;
							_bifVoxels[cn.z][i] = newValB;
						}
						if (_posVoxels != null)
						{
							if (newVal > _maxCentAcc) {_maxCentAcc = newVal; }
							
							if (_posVoxels[cn.z][i] == _posCentVal)
							{
								thisCentPos++;	
							}
							
						}
						
					}
				}
			}
		}
		
		if (_posVoxels != null)
		{
			double sumSqrError = this.sumSquaresError(added, _posCent);
			double meanSumSqrError = sumSqrError / _posCent.size();
			double RMSE = Math.sqrt(meanSumSqrError);
			
			double accuracy = (double)thisCentPos / (double)_posCentCount;
			_cumSSE+=sumSqrError;
			_cumMSSE+=meanSumSqrError;
			_cumRMSE+=RMSE;
			_cumAccuray+=accuracy;
			
			IJ.log(accuracy+" = "+thisCentPos+"/"+_posCentCount+", Starts="+_maxPossible+
					", SSE="+sumSqrError+", MSSE="+meanSumSqrError+", RMSE="+RMSE);
		}
		if (_trueCent != null)
		{
			double sumSqrError = this.SSE(centerlineGraphs, _trueCent);
			
			double meanSumSqrError = sumSqrError / _trueCent.size();
			double RMSE = Math.sqrt(meanSumSqrError);
			
			_cumTrueSSE+=sumSqrError;
			_cumTrueMSSE+=meanSumSqrError;
			_cumTrueRMSE+=RMSE;
			
			IJ.log("Accuracy: "+_sseAccuracy+", Starts="+_maxPossible+
					", SSE="+sumSqrError+", MSSE="+meanSumSqrError+", RMSE="+RMSE);
		}
	}
	
	public double sumSquaresError(Set<GraphNode> cent, List<Position> posCent)
	{
		double ss = 0;
		for (Position pc: posCent)
		{
			double closest = Double.MAX_VALUE;
			for (GraphNode c: cent)
			{
				double d = _vd.distance(c, pc);
				if (d < closest)
				{
					closest = d;
				}
			}
			ss += closest*closest;
		}
		return ss;
	}
	private double _sseAccuracy;
	private StringBuffer _sseString;
	public double SSE(List<CenterlineGraph> cents, List<DPoint> posCent)
	{
		if (_sseString == null)
		{
			_sseString = new StringBuffer();
		}
		_sseString.append("x y z\tcol row z\tError\n");
		double ss = 0;
		int acc = 0;
		int tot = 0;
		StringBuffer sb = new StringBuffer();
		for (CenterlineGraph ctln: cents)
		{
			for (GraphNode cNode: ctln.getCenterlineNodes())
			{
				double closestDist = Double.MAX_VALUE;
				DPoint closePoint = null;
				
				for (DPoint pc: posCent)
				{
					double d = _vd.distance(cNode, pc.x, pc.y, pc.z);
					if (d < closestDist)
					{
						closestDist = d;
						closePoint = pc;
					}
					
				}
				double closestVoxel = _vd.distance(cNode, Math.round(closePoint.x), Math.round(closePoint.y), 
						Math.round(closePoint.z));
				if (closestVoxel == 0)
				{
					acc++;
				}
				tot++;
				
				sb.append(closePoint.x); sb.append(" ");
				sb.append(closePoint.y); sb.append(" ");
				sb.append(closePoint.z); sb.append(" ");
				
				sb.append(cNode.col); sb.append(" ");
				sb.append(cNode.row); sb.append(" ");
				sb.append(cNode.z); sb.append(" ");
				sb.append(closestDist);
				/*
				sb.append(acc); sb.append("/"); sb.append(tot);sb.append("=");
				double a = (double)acc;
				double t = (double)tot;
				sb.append( a/t );
				sb.append(closePoint.z); 
				*/
				sb.append("\n");
				
				ss += closestDist*closestDist;
				
			}
		}
		double a = (double)acc;
		double t = (double)tot;
		_sseAccuracy = a/t;
		_cumTrueAccuray+=_sseAccuracy;
		_sseString.append(sb); 
		_sseString.append("\n\n");
		return ss;
	}
	
	/** Write histogram for R and return stability. */
	public double saveCentHistogram(String fileName)
	{
		return saveHistogram(fileName, _centVoxels, _centBackgroundAdded);
	}
	/** @return Bifurcation stability */
	public double saveBifHistogram(String fileName)
	{
		return saveHistogram(fileName, _bifVoxels, _bifBackgroundAdded);
	}
	/** Save the histogram for import into R and return the stability percentage. */
	private double saveHistogram(String fileName, short[][] voxels, boolean addedBackground )
	{
		IJ.log("Save histogram: "+fileName+" maximum possbile: "+_maxPossible);
		int[] hist = new int[_maxPossible];
		for (int x=0; x < _width; x++)
		{
			for (int y=0; y < _height; y++)
			{
				for (int z=0; z < _zSize; z++)
				{
					int val = voxels[z][y*_width+x];
					if (addedBackground) val = val - BACKGROUND;
					if (val > 0)
					{
						int valM1 = val-1;
						if (valM1 < _maxPossible)
						{
							hist[valM1]++;
						}
						else
						{
							IJ.log("Centerline histogram out of range: "+val+" max possible: "+_maxPossible);
						}	
					}
				}
			}
		}
		StringBuffer sb = new StringBuffer("overwrite\tcount\n");
		for (int i=0; i < hist.length; i++)
		{
			sb.append(i+1); sb.append("\t"); sb.append(hist[i]); sb.append("\n");
		}
		PrintWriter out = null;
		try 
		{
			FileWriter outFile = new FileWriter(fileName);
			out = new PrintWriter(outFile);
			out.print(sb.toString());
			out.close();
		} 
		catch (IOException e)
		{
			StackTraceElement[] ste = e.getStackTrace();
			IJ.log("Writing file: "+fileName+": "+e.getMessage());
			for (int i=0; i < ste.length; i++)
			{
				IJ.log(ste[i].toString());
			}
			
		}
		finally
		{
			out.close();
		}
		int hSum = 0 ;
		for (int h=0; h< hist.length; h++)
		{
			hSum+=hist[h];
		}
		double pStable = (double)hist[hist.length-1]/(double)hSum;
		return pStable;
	}
	private ImagePlus getInverseImage(ImagePlus image, short[][] voxels, short min, short max)
	{
		int range = max-min;
		
		double factor = ((double)(255-BACKGROUND)/(double)(range+min) ) - 0.2;
		IJ.log("Scaling intensity: max: "+max+" min: "+min+" range:"+range+" factor: "+factor+" BACKGROUND: "+BACKGROUND);
		// invert so centerlines with less overlap are brighter 
		for (int x=0; x < _width; x++)
		{
			for (int y=0; y < _height; y++)
			{
				for (int z=0; z < _zSize; z++)
				{
					int val = voxels[z][y*_width+x];
					if (val > 0)
					{
						double newVal = 255.0-(double)BACKGROUND-( (double)(val-1)*factor );
						voxels[z][y*_width+x] = (short)(Math.floor(newVal));
					}
				}
			}
		}
		return image;
	}
	
	public ImagePlus getBifImage()
	{
		int back = 0;
		if (_centBackgroundAdded)
		{
			back = BACKGROUND;
		}
		int scale = 255 -_bifMax-BACKGROUND;
		for (int x=0; x < _width; x++)
		{
			for (int y=0; y < _height; y++)
			{
				for (int z=0; z < _zSize; z++)
				{
					int val = _bifVoxels[z][y*_width+x];
					if (val > 0)
					{
						int newVal = val+scale;
						_bifVoxels[z][y*_width+x] = (short)(newVal);
					}
				}
			}
		}
		for (int x=0; x < _width; x++)
		{
			for (int y=0; y < _height; y++)
			{
				for (int z=0; z < _zSize; z++)
				{
					
					if (_bifVoxels[z][y*_width+x] == 0 && _centVoxels[z][y*_width+x]-back > 0)
					{
						_bifVoxels[z][y*_width+x] = BACKGROUND;
					}
				}
			}
		}
		
		this.addBifBackground();
		return _bifImage;
	}
	/** invert values so consistent centerlines are dimmer and inconsistent centerlines are brighter
	 range intensity to 255. Add segmentation BACKGROUND intensity to voxels. */  
	public ImagePlus getCentImage() 
	{
		ImagePlus image =  getInverseImage(_centImage, _centVoxels, _centMin, _centMax);
		this.addCentBackground();
		return image;
	}
	public int getMaxPossible() {
		return _maxPossible;
	}
	/** Needs to be called before getCentImage before values are inverted. */
	public ImagePlus getAccuracyIm() {
		if (_trueCent != null)
		{
			double mp = (double)_maxPossible;
			double aveAccuracy = _cumTrueAccuray/mp;
			double aveSSE = _cumTrueSSE/mp;
			double aveMSSE = _cumTrueMSSE/mp;
			double aveRMSE = _cumTrueRMSE/mp;
			IJ.log("Average Accuracy="+aveAccuracy+", SSE="+aveSSE+", MSSE="+aveMSSE+", RMSE="+aveRMSE+", starts="+_maxPossible);
			if (_fileBaseName != null)
			{
				String fn = _fileBaseName+"TruthError.txt";
				try
				{
					// Create file 
					IJ.log("Write: "+fn);
					FileWriter fstream = new FileWriter(fn);
			        BufferedWriter out = new BufferedWriter(fstream);
			        out.write(_sseString.toString());
			        out.close();
			    }
				catch (Exception e)
				{
					IJ.log("Error: " + e.getMessage());
			    }
			}
		}
		if (_posVoxels != null)
		{
			double mp = (double)_maxPossible;
			double aveAccuracy = _cumAccuray/mp;
			double aveSSE = _cumSSE/mp;
			double aveMSSE = _cumMSSE/mp;
			double aveRMSE = _cumRMSE/mp;
			IJ.log("Average Accuracy="+aveAccuracy+", SSE="+aveSSE+", MSSE="+aveMSSE+", RMSE="+aveRMSE+", starts="+_maxPossible);
			// int[] tmpClr = new int[3];
			double centFactor  = 255.0/(double)_maxCentAcc;
			// IJ.log("maxCentAcc: "+_maxCentAcc+" Centerline factor: "+centFactor);
			for (int z=0; z < _zSize; z++)
			{
				for (int x=0; x < _width; x++)
				{
					for (int y=0; y < _height; y++)
					{
						int i = y*_width+x;
						int[] clr = new int[3];
						if (_centVoxels[z][i] > 0)
						{
							clr[0] = (int)(_centVoxels[z][i]*centFactor);
						}
						if (_posVoxels[z][i] == _posCentVal)
						{
							clr[1]=255;
						}
						if (_centVoxels[z][i] == 0 && _posVoxels[z][i] > 0 && _posVoxels[z][i] < _posCentVal)
						{
							clr[2]=50;
						}
						
						_accuracyProcs[z].putPixel(x, y, clr);
						
					}
					
				}
			}
		}
		return _accuracyIm;
	}
	public List<DPoint> readTrueCentFile(String fileName)
	{
		List<DPoint> points = new LinkedList<DPoint>();
		if (fileName == null)
			return points;
		
		Pattern xyzrPat = Pattern.compile("(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)\\s+(\\d+\\.\\d+)");
    	
	    try 
	    {
	    	BufferedReader input =  new BufferedReader(new FileReader(fileName));
	    	try 
	    	{
	    		String line = null; 
	    		while (( line = input.readLine()) != null)
	    		{
	    			double x, y, z, r;
	    			Matcher mat = xyzrPat.matcher(line);
	    	    	while (mat.find())
	    	    	{
	    	    		x = Double.parseDouble(mat.group(1));
	    	    		y = Double.parseDouble(mat.group(2));
	    	    		z = Double.parseDouble(mat.group(3));
	    	    		r = Double.parseDouble(mat.group(4));
	    	    		DPoint pt = new DPoint(x, y, z, r);
	    	    		// IJ.log("True centerline point: "+pt.toString());
	    	    		points.add(pt);
	    	    	}
	    		}
	    	}
	    	finally 
	    	{
	    		input.close();
	    	}
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
	    _trueCent = points;
	    return points;
	}
	public String getFileBaseName() {
		return _fileBaseName;
	}
	public void setFileBaseName(String fileBaseName) {
		_fileBaseName = fileBaseName;
	}
}

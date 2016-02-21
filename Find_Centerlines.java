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

import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.imagek.CenterOfMass;
import ktdiedrich.imagek.Centerlines;
import ktdiedrich.imagek.MIP;
import ktdiedrich.util.TempProperties;
import ij.*;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.io.*;

/** Find centerlines from a 3-D image stack  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Find_Centerlines implements PlugInFilter {
    private ImagePlus _imp;
    private Centerlines _centerlines;
    
    public int setup(String arg, ImagePlus imp) 
    {
        _imp = imp;
        //return DOES_16+STACK_REQUIRED;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
    	_centerlines = new Centerlines();
    	float xres = 0;
    	float yres = 0;
    	float zres = 0;
    	float dfeThreshold = 0.0f;
    	TempProperties tp = null;
    	try 
		{
			tp = new TempProperties(TempProperties.ANERUYSM_TEMP);
			String p = tp.getProperty("xres");
			if (p!=null) xres = Float.parseFloat(p);
			p = tp.getProperty("yres");
			if (p!=null) yres = Float.parseFloat(p);
			p = tp.getProperty("zres");
			if (p!=null) zres = Float.parseFloat(p);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		/*
		if (zres > 0)
		{
			dfeThreshold = zres;
			if (xres > dfeThreshold) dfeThreshold = xres;
			if (yres > dfeThreshold) dfeThreshold = yres;
		}
		*/
        GenericDialog gd = new GenericDialog("Find_Centerlines");
        gd.setLayout(new FlowLayout());
        gd.setPreferredSize(new Dimension(550, 550));
        gd.addNumericField("Distance From Edge threshold", dfeThreshold, 1);
        gd.addNumericField("Minimum line length", 30, 0);
        gd.addNumericField("X Resolution", xres, 8);
        gd.addNumericField("Y Resolution", yres, 8);
        gd.addNumericField("Z Resolution", zres, 8);
        gd.addNumericField("Weight A", Centerlines.A, 1);
        gd.addNumericField("Weight b", Centerlines.B, 1);
        gd.addNumericField("Centerline window radius", 4, 0);
        gd.addNumericField("Aneurysm enhancement threshold", 0.92, 2);
        gd.addNumericField("Aneurysm enhancement size threshold", 0.80, 2);
        gd.addNumericField("Extend enhancement", 0, 0);
        gd.addNumericField("DFC DFE ratio threshold", 5.0, 2);
        gd.addNumericField("Line length DFE ratio", 10.0, 1);
        gd.addNumericField("Lower cluster threshold", 10000, 0);
        gd.addNumericField("Maximum recenter times", 100, 0);
        gd.addNumericField("Minimum recenter times: ", CenterOfMass.MIN_MOVES, 0);
        gd.addNumericField("Center of mass weight power", CenterOfMass.WEIGHT_POWER, 1);
        gd.addNumericField("Phase Contrast weight", 0, 6);
        gd.addNumericField("Velocity dot sigma", 0.00001, 6);
        gd.addNumericField("Velocity power", 1, 0);
        gd.addCheckbox("Show Intermediate steps", false);
        gd.addCheckbox("Stability of centerlines from all ends", false);
        gd.addCheckbox("Fix badEnds", false);
        gd.addCheckbox("Centerline tortuosity", true);
        gd.addCheckbox("DFE weighted COM cost function", true);
        
        String[] axises = {"X", "Y", "Z"};
        gd.addChoice("MIP axis", axises, "Z");
        
        String[] tortAlgs = {"DFM"};
        gd.addChoice("Tortuosity algorithm", tortAlgs, "DFM");
        
        String[] centAlgs = {Inserts.DFE_WEIGHTED_COM_NAME, Inserts.COM_NAME, Inserts.DFEWTCOM_MULT_PCCROSSNORM_NAME,
        		Inserts.VELOC_DFECOM_NAME, Inserts.DFE_NAME, Inserts.VELOC_COST_NAME};
        gd.addChoice("Centerline algorithm", centAlgs, Inserts.DFE_WEIGHTED_COM_NAME);
        
        Button centerlineB = new Button("Add centerline positive control image");
        centerlineB.addActionListener(new SetImageFileName(SetImageFileName.CENTERLINE_POSTIVE_CONTROL));
        gd.add(centerlineB);
        
        Button pcXB = new Button("Add Phase Contrast X image");
        pcXB.addActionListener(new SetImageFileName(SetImageFileName.PC_X));
        gd.add(pcXB);
        
        Button pcYB = new Button("Add Phase Contrast Y image");
        pcYB.addActionListener(new SetImageFileName(SetImageFileName.PC_Y));
        gd.add(pcYB);
        
        Button pcZB = new Button("Add Phase Contrast Z image");
        pcZB.addActionListener(new SetImageFileName(SetImageFileName.PC_Z));
        gd.add(pcZB);
        
        Button centCoordB = new Button("Centerline x, y, z coordinates");
        centCoordB.addActionListener(new CenterlineCoordinates());
        gd.add(centCoordB);
        
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        String axis = gd.getNextChoice();
    	short axisType = MIP.Z_AXIS;
    	if (axis.equals("X"))
    	    axisType = MIP.X_AXIS;
    	if (axis.equals("Y"))
    	    axisType = MIP.Y_AXIS;
    	
    	String tortAlg = gd.getNextChoice();
    	int tortAlgId = ktdiedrich.db.aneurysm.Inserts.TORT_DFM_3_AVE_ALG;
        if (tortAlg == "DFM")
        {
        	tortAlgId = ktdiedrich.db.aneurysm.Inserts.TORT_DFM_ALG;
        }
        _centerlines.setTortuosityAlg(tortAlgId);
        
        String centAlg = gd.getNextChoice();
        IJ.log("Centerline algorithm: "+centAlg);
        int centAlgId  = Inserts.DFE_WEIGHTED_COM; 
        if (centAlg.equals(Inserts.DFE_WEIGHTED_COM_NAME)) centAlgId = Inserts.DFE_WEIGHTED_COM;
        else if (centAlg.equals(Inserts.DFEWTCOM_MULT_PCCROSSNORM_NAME)) centAlgId = Inserts.DFEWTCOM_MULT_PCCROSSNORM;
        else if (centAlg.equals(Inserts.VELOC_DFECOM_NAME)) centAlgId = Inserts.VELOC_DFECOM;
        else if (centAlg.equals(Inserts.DFE_NAME)) centAlgId  = Inserts.DFE_CENTERLINE_ALGORITHM;
        else if (centAlg.equals(Inserts.VELOC_COST_NAME)) centAlgId = Inserts.VELOC_COST;
        else if (centAlg.equals(Inserts.COM_NAME)) centAlgId = Inserts.COM_CENTERLINE_ALGORITHM;
        
        _centerlines.setCenterlineAlgorithm(centAlgId);
        
        _centerlines.recordPanel();
        _centerlines.setMipAxis(axisType);
        _centerlines.setDfeThreshold( (float)gd.getNextNumber() );
        _centerlines.setMinLineLength( (int)gd.getNextNumber() );
        _centerlines.setXRes((float)gd.getNextNumber());
        _centerlines.setYRes((float)gd.getNextNumber());
        _centerlines.setZRes((float)gd.getNextNumber());
        _centerlines.setA((float)gd.getNextNumber());
        _centerlines.setB((float)gd.getNextNumber());
        _centerlines.setWindowSize((int)gd.getNextNumber());
        _centerlines.setEnhancementIntensityThreshold(gd.getNextNumber());
        _centerlines.setEnhancementSizeThreshold(gd.getNextNumber());
        _centerlines.setExtendEnhancement((int)gd.getNextNumber());
        _centerlines.setDfcDfeRatioThreshold(gd.getNextNumber());
        _centerlines.setLineDFEratio(gd.getNextNumber());
        _centerlines.setLowClusterThreshold((int)gd.getNextNumber());
        _centerlines.setRecenterTimes((int)gd.getNextNumber());
        _centerlines.setMinRecenter((int)gd.getNextNumber());
        _centerlines.setMassWeightPower(gd.getNextNumber());
        _centerlines.setPcWeight(gd.getNextNumber());
        _centerlines.setVelocityDotSigma(gd.getNextNumber());
        _centerlines.setVelocityPower(gd.getNextNumber());
        _centerlines.setShowSteps(gd.getNextBoolean());
        _centerlines.setCenterlinesFromAllEnds(gd.getNextBoolean());
        _centerlines.setFixBadEnds(gd.getNextBoolean());
        _centerlines.setMeasureCenterlines(gd.getNextBoolean());
        _centerlines.setDfeWeightedCOM(gd.getNextBoolean());
        
        if (tp != null)
        {
        	try 
        	{
        		tp.setProperty("xres", ""+_centerlines.getXRes());
        		tp.setProperty("yres", ""+_centerlines.getYRes());
        		tp.setProperty("zres", ""+_centerlines.getZRes());
        	} 
        	catch (IOException e) 
        	{
        		e.printStackTrace();
        	}
        }
        _centerlines.findCenterlines(_imp);
    }
    /** Open up an image file that matches the input file is size and sets the value defined. */
    class SetImageFileName implements ActionListener
    {
    	public static final short CENTERLINE_POSTIVE_CONTROL = 1;
    	public static final short PC_X = 2;
    	public static final short PC_Y = 3;
    	public static final short PC_Z = 4;
    	private short _fileType = 0;
    	public SetImageFileName(short fileType)
    	{
    		_fileType = fileType;
    	}
    	public void actionPerformed(ActionEvent e)
    	{
    		OpenDialog od = new OpenDialog("Open image file", null);
    		
    		ImagePlus image = IJ.openImage(od.getDirectory()+od.getFileName());
    		image.show();
    		image.updateAndDraw();
    		if (_fileType == CENTERLINE_POSTIVE_CONTROL)
    		{
    			_centerlines.setCenterlinePositive(image);
    		}
    		else if (_fileType == PC_X)
    		{
    			_centerlines.setXPCimage(image);
    		}
    		else if (_fileType == PC_Y)
    		{
    			_centerlines.setYPCimage(image);
    		}
    		else if (_fileType == PC_Z)
    		{
    			_centerlines.setZPCimage(image);
    		}
    	}
    }
    /** Open up a centerline coordinate file with x, y, z,r coordinates  */
    class CenterlineCoordinates implements ActionListener
    {
    	public void actionPerformed(ActionEvent e)
    	{
    		OpenDialog od = new OpenDialog("Open centerline positive control", null);
    		String directory = od.getDirectory();
    		String fileName = od.getFileName();
    		IJ.log("Centerline coordinates: "+directory+fileName);
    		_centerlines.setTrueCenterlineFileName(directory+fileName);
    		
    	}
    }
}



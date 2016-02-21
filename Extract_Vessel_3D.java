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
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import ij.*;
import ij.gui.GenericDialog;
import ij.io.FileSaver;
import ij.io.OpenDialog;
import ij.process.*;
import ij.plugin.filter.*;
import ktdiedrich.db.DBStatus;
import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.imagek.*;
import ktdiedrich.util.TempProperties;

/** Keep smooth parts of an image like a Maximum Intensity Projection Z-Buffer  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Extract_Vessel_3D implements PlugInFilter
{
    private ImagePlus _imp;
    public int setup(String arg, ImagePlus imp) {
        this._imp = imp;
        return DOES_ALL+STACK_REQUIRED;
    }
    private int _seedClusterMin;
    private int _cluster3Dmin;
    private double _chisqMax;
    private short _zDiff;
    private double _seedHistThres;
    private int _scalpDist;
    private int _holeFillIt;
    private int _holeFillDirections;
    private int _holeFillRadius;
    private int _medianFilterSize;
    private double _medFilterStdDevFactor;
    private boolean _doMedianFilter;
    private int _bubbleFillAlgorithm;
    private ImagePlus _segImage;
    private String _segBaseName;
    private String _segDir;
    private RecordSegmentationPanel _recordPanel;
    
    public void run(ImageProcessor ip) 
    {     
    	int scalpskull = 0;
    	float seedHistThres = Extractor3D.SEED_HIST_THRES;
    	try 
		{
			TempProperties tp = new TempProperties(TempProperties.ANERUYSM_TEMP);
			String p = tp.getProperty("scalpskull");
			if (p!=null) scalpskull = Integer.parseInt(p);
			p = tp.getProperty("hist2dthres");
			if (p != null) seedHistThres = Float.parseFloat(p);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
        GenericDialog gd = new GenericDialog("Extract_Vessel_3D");
        gd.setLayout(new FlowLayout());
        gd.setPreferredSize(new Dimension(500,500));
        gd.addNumericField("Minimum seed size (2D)", Extractor3D.MIN_2D_SEED_SIZE, 0);
        gd.addNumericField("Minimum 3-D cluster size", Extractor3D.MIN_3D_CLUSTER_SIZE, 0);
        gd.addNumericField("Max chi squared smoothness", Extractor3D.CHI_SQ_SMOOTHNESS, 1);
        gd.addNumericField("Allow vessel voxel Z difference", Extractor3D.VOXEL_Z_DIFF, 0);
        gd.addNumericField("Seed histogram threshold (2D)", seedHistThres, 2);
        gd.addNumericField("Scalp skull", Extractor3D.SCALP_SKULL, 0);
        gd.addNumericField("Hole fill iterations", Extractor3D.HOLE_FILL_ITERATIONS, 0);
        gd.addNumericField("Hole Fill directions", Extractor3D.HOLE_FILL_DIRECTIONS, 0);
        gd.addNumericField("Hole Fill radius", Extractor3D.HOLE_FILL_RADIUS, 0);
        gd.addCheckbox("Median filter", Extractor3D.MEDIAN_FILTER);
        gd.addNumericField("Median filter size", Extractor3D.MEDIAN_FILTER_SIZE, 0);
        gd.addNumericField("Median filter std devs above", Extractor3D.MEDIAN_FILTER_STD_DEV_ABOVE, 1);
        
        gd.addCheckbox("Show Intermediate steps", false);
        gd.addCheckbox("Bubble fill by slice", false);
        
        Extractor3D extractor = new Extractor3D();
        
        Button pcXB = new Button("Add Phase Contrast X image");
        pcXB.addActionListener(new SetImageFileName(SetImageFileName.PC_X, extractor));
        gd.add(pcXB);
        
        Button pcYB = new Button("Add Phase Contrast Y image");
        pcYB.addActionListener(new SetImageFileName(SetImageFileName.PC_Y, extractor));
        gd.add(pcYB);
        
        Button pcZB = new Button("Add Phase Contrast Z image");
        pcZB.addActionListener(new SetImageFileName(SetImageFileName.PC_Z, extractor));
        gd.add(pcZB);
        
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        _seedClusterMin = (int)gd.getNextNumber();
        _cluster3Dmin = (int)gd.getNextNumber();
        _chisqMax = (double)gd.getNextNumber();
        _zDiff = (short)gd.getNextNumber();
        _seedHistThres = (double)gd.getNextNumber();
        _scalpDist = (int)gd.getNextNumber();
        _holeFillIt = (int)gd.getNextNumber();
        _holeFillDirections = (int)gd.getNextNumber();
        _holeFillRadius = (int)gd.getNextNumber();
        _medianFilterSize = (int)gd.getNextNumber();
        _medFilterStdDevFactor = (double)gd.getNextNumber();
        _doMedianFilter = gd.getNextBoolean();
        boolean showSteps = gd.getNextBoolean();
        
        _bubbleFillAlgorithm = BubbleFill.BUBBLE_FILL_2D_PLANES;
        boolean bubbleFillBySlice = gd.getNextBoolean();
        if (bubbleFillBySlice == true) _bubbleFillAlgorithm = BubbleFill.BUBBLE_FILL_2D_PLANES;
        else _bubbleFillAlgorithm = BubbleFill.BUBBLE_FILL_3D;
        
        JFrame dialog = this.makeRecordSegmentationFrame();
        dialog.setVisible(true);
        
        extractor.setMessageWindow(_recordPanel);
        extractor.setShowSteps(showSteps);
        
        if (_doMedianFilter)
        {
            extractor.setMedFilterStdDevAbove(_medFilterStdDevFactor);
            extractor.setMedianFilterSize(_medianFilterSize);
        }
        extractor.setScalpDist(_scalpDist);
        extractor.setMaxChisq(_chisqMax);
        extractor.setZDiff(_zDiff);
        extractor.setSeedClusterMin(_seedClusterMin);
        
        extractor.setSeedHistogramThreshold(_seedHistThres);
        // extractor.setSeedWindowRadius(seedWindowRadius);
        extractor.setFillHolesTimes(_holeFillIt);
        extractor.setHoleFillDirections(_holeFillDirections);
        extractor.setHoleFillRadius(_holeFillRadius);
        
        extractor.setClusterSizeThreshold(_cluster3Dmin);
        extractor.setBubbleFillAlgorithm(_bubbleFillAlgorithm);
        long start = System.currentTimeMillis();
        _segImage = extractor.segment(_imp);
        long duration = System.currentTimeMillis() - start;
        _recordPanel.message("Vessel segmentation: "+duration/60000+" minutes");
        _imp.updateAndDraw();
        
        _segBaseName = extractor.getSegBaseName();
        _segDir = extractor.getSegDir();
        IJ.log("Segmentation: basename: "+_segBaseName+", seg dir: "+_segDir);
        if (showSteps)
        {
            ImagePlus seedRecon = extractor.getSeedImage();
            seedRecon.show();
            seedRecon.updateAndDraw();
        }
        Clusters clus = new Clusters(_segImage);
        List<Cluster> clusters = clus.getImageClusters();
        if (showSteps)
        {
	        int i = 0;
	        for (Cluster clst: clusters)
	        {
	        	IJ.log(i+" "+clst.toString());
	        	i++;
	        }
	        ImagePlus clusImage = clus.getClusterImage();
	        clusImage.show();
	        clusImage.updateAndDraw();
        }
        clus.thresholdClusters(_cluster3Dmin);
        if (showSteps)
        {
        	clus = new Clusters(_segImage);
        	clusters = clus.getImageClusters();
        	int i = 0;
        	IJ.log("Thresholded segmentation clusters: "+_cluster3Dmin);
        	for (Cluster clst: clusters)
        	{
        		IJ.log(i+" "+clst.toString());
        		i++;
        	}
        }
        _segImage.show();
        _segImage.updateAndDraw();
        NegativeStack neg = new NegativeStack();
        neg.negate(_imp.getImageStack(), _segImage.getImageStack());
        _imp.updateAndDraw();
        
    }
    /** make a dialog for saving tortuosity data. */
	public JFrame makeRecordSegmentationFrame()
	{
		JFrame frame = new JFrame();
		frame.setSize(500, 250);
		frame.setTitle("Record_segmentation_aneurysm_database");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		_recordPanel = new RecordSegmentationPanel(DbConn.PROPERTIES);
		contentPane.add(_recordPanel);	
		
		return frame;
	}
	
	class RecordSegmentationPanel extends JPanel implements Message, DBStatus
	{
		private static final long serialVersionUID = 1L;
		private String _fullProp;
		private JTextField _imageIdField, _segmentationIdField;
		private GridBagConstraints _c;
		private JButton _recordButton;
		private JTextArea _messageArea;
		private DbConn _dbConn;
		private TempProperties _temp;
		public RecordSegmentationPanel(String properties)
		{
			try
			{
				_temp = new TempProperties(TempProperties.ANERUYSM_TEMP);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			_dbField = new JTextField();
			_dbConn = new DbConn();
			_dbConn.setDbStatus(this);
			_fullProp = DbConn.findPropertiesFile(properties);
			this.setLayout(new GridBagLayout());
	        _c = new GridBagConstraints();
	        _c.weightx = 0.0;
	        _c.weighty = 0.0;
	        _c.fill = GridBagConstraints.BOTH;
	        _c.gridy = 0;
	        _c.gridx = 0;
	        JLabel label = new JLabel("Image_id");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _c.weightx = 1.0;
	        _imageIdField = new JTextField();
	        
	        try
	        {
	        	String imageId = _temp.getProperty("image_id");
	        	_imageIdField.setText(imageId);
	        }
	        catch (IOException e)
	        {
	        	e.printStackTrace();
	        }
	        this.add(_imageIdField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = 2;
	        _recordButton = new JButton("Record segmentation to aneurysm database");
	        _recordButton.addActionListener(new ActionListener(){
	        	public void actionPerformed(ActionEvent ae)
	        	{
	        		save();
	        	}
	        });
	        this.add(_recordButton, _c);
	        
	        _c.gridwidth = 1;
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.weightx = 0.0;
	        label = new JLabel("Saved segmentation_id");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _segmentationIdField = new JTextField();
	        _segmentationIdField.setEditable(false);
	        _segmentationIdField.setBackground(Color.YELLOW);
	        _segmentationIdField.setForeground(Color.BLUE);
	        this.add(_segmentationIdField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = 2;
	        _c.weighty = 1.0;
	        _messageArea = new JTextArea();
	        _messageArea.setEditable(false);
	        _messageArea.setBackground(Color.LIGHT_GRAY);
	        JScrollPane scroll = new JScrollPane(_messageArea);
	        this.add(scroll, _c);
	    
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = GridBagConstraints.REMAINDER;
	        _c.weightx = 1.0;
	        _c.weighty = 0.0;
	        _dbField.setEditable(false);
	        _dbField.setBorder(new BevelBorder(BevelBorder.LOWERED));
	        this.add(_dbField, _c);
	        
		}
		private JTextField _dbField;
		public void setDbField(DbConn dbConn)
		{
			_dbField.setText(dbConn.getUrl()+" | "+dbConn.getUsername());
	    }
		
		/** Save image to database. */
		public void save()
		{
			String segFileName = _segBaseName+".zip";
			// TODO ZIP and save segmentation 
			FileSaver fs = new FileSaver(_segImage);
			String segPath = _segDir+File.separator+segFileName;
			if (segPath.contains("private"))
			{
				segPath = segPath.replace("private", "public");
			}
			fs.saveAsZip(segPath);
			IJ.log("Saved: "+segPath);
			int imageId = Integer.parseInt(_imageIdField.getText());
			if (!_doMedianFilter)
			{
				_medianFilterSize = 0;
				_medFilterStdDevFactor = 0.0;
			}
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				Inserts inserts = new Inserts(con);
				int segmentationId = inserts.insertSegmentation(
						_seedClusterMin,
						_cluster3Dmin,
						_chisqMax,
						_zDiff,
						_seedHistThres,
						_scalpDist,
						_holeFillIt,
						_holeFillDirections,
						_holeFillRadius,
						_medianFilterSize,
						_medFilterStdDevFactor,
						imageId,
						_bubbleFillAlgorithm,
						segFileName,
						_segDir
		        );
				_segmentationIdField.setText(""+segmentationId);
				_temp.setProperty("segmentation_id", ""+segmentationId);
				_temp.setProperty("scalpskull", ""+_scalpDist);
				_temp.setProperty("hist2dthres", ""+_seedHistThres);
			}
			catch (FileNotFoundException e)
			{
				message(e.getMessage());
				e.printStackTrace();
			}
			catch (IOException e)
			{
				message(e.getMessage());
				e.printStackTrace();
			}
			catch (SQLException e)
			{
				message(e.getMessage());
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
					message(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		public void message(String m)
		{
			_messageArea.append(m+"\n");
		}
		public void clear()
		{
			_messageArea.setText("");
		}
	}
	/** Open up an image file that matches the input file is size and sets the value defined. */
    class SetImageFileName implements ActionListener
    {
    	public static final short PC_X = 2;
    	public static final short PC_Y = 3;
    	public static final short PC_Z = 4;
    	private short _fileType = 0;
    	private Extractor3D _ex;
    	public SetImageFileName(short fileType, Extractor3D ex)
    	{
    		_fileType = fileType;
    		_ex = ex;
    	}
    	public void actionPerformed(ActionEvent e)
    	{
    		OpenDialog od = new OpenDialog("Open image file", null);
    		
    		ImagePlus image = IJ.openImage(od.getDirectory()+od.getFileName());
    		image.show();
    		image.updateAndDraw();
    		
    		if (_fileType == PC_X)
    		{
    			_ex.setXPCimage(image);
    		}
    		else if (_fileType == PC_Y)
    		{
    			_ex.setYPCimage(image);
    		}
    		else if (_fileType == PC_Z)
    		{
    			_ex.setZPCimage(image);
    		}
    	}
    }
}

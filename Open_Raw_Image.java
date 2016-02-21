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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ktdiedrich.db.DBStatus;
import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.db.aneurysm.Queries;
import ktdiedrich.db.aneurysm.Updates;
import ktdiedrich.imagek.ImageData3D;
import ktdiedrich.imagek.Message;
import ktdiedrich.imagek.Rotate3D;
import ktdiedrich.util.TempProperties;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.StackProcessor;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;

/* Open a raw file image 16 bit unsigned gray-scale LZW compressed (.gz) 
 * and read (x, y, z) from the filename.  
 * @author Karl Diedrich <ktdiedrich@gmail.com>
  */
public class Open_Raw_Image implements PlugIn
{
	private ImageData3D _imageData3D;
	private String _directory, _filename;
	private RecordImagePanel _panel;
	private String _imageBase;
	// needs to match a item in database aneurysm.arteryshape; 
	public static final String DEFAULT_ARTERY_SHAPE = "present";
	
	public void message(String s)
	{
		if (_panel != null)
			_panel.message(s);
		else
			System.out.println(s);
	}
	
    public void run(String arg)
    {
    	try 
		{
			_imageBase = DbConn.getProperty("base.image");
			System.out.println("image base: "+_imageBase);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
    	
    	String title = "Open Raw 16 bit 3D image...";
        OpenDialog od = new OpenDialog(title, arg);
        
        _directory = od.getDirectory();
        _filename = od.getFileName();
        if (_filename==null)
            return;
        
        GenericDialog gd = new GenericDialog(title);
        gd.addCheckbox("Little Endian byte order", false);
        gd.addCheckbox("Rotate face up", false);
        gd.addCheckbox("Flip Z", true);
        gd.showDialog();
        if (gd.wasCanceled()) {
            IJ.error("PlugIn canceled!");
            return;
        }
        
        _imageData3D = new ImageData3D();
        _imageData3D.setIntelByteOrder(gd.getNextBoolean());
        boolean rotateFaceUp = gd.getNextBoolean();
        boolean flipZ = gd.getNextBoolean();
        _imageData3D.setDisplay(false);
        int subjectId = _imageData3D.getSubjectId(_directory);
        JFrame dialog = this.makeRecordImageFrame(subjectId);
        dialog.setVisible(true);
        
        try
        {
        	ImagePlus image = _imageData3D.getImage(_directory, _filename);
    	    if (rotateFaceUp)
    	    {
	        	ImageStack stack = image.getStack();
	    	    ImageStack rStack = new ImageStack(image.getHeight(), image.getWidth());
	    	    for (int i=1; i<= stack.getSize(); i++)
	    	    {
	    	    	ImageProcessor ip = stack.getProcessor(i);
	    	    	rStack.addSlice(""+i, ip.rotateRight());
	    	    }
	    	    image.setStack(image.getShortTitle(), rStack);
	    	    
    	    }
    	    if (flipZ)
    	    {
    	    	image = Rotate3D.flipZ(image);
    	    }
    	    image.show();
    	    image.updateAndDraw();
        	_panel.load();
        }
        catch(IOException ex)
        {
        	message(ex.getMessage());
        }
        catch (ParseException ex)
        {
        	message(ex.getMessage());
        }
    }
    
    
	/** make a dialog for saving tortuosity data. */
	public JFrame makeRecordImageFrame(int subjectId)
	{
		JFrame frame = new JFrame();
		frame.setSize(900, 750);
		frame.setTitle("Record_image_aneurysm_database");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		_panel = new RecordImagePanel(DbConn.PROPERTIES, subjectId);
		contentPane.add(_panel);	
		
		return frame;
	}
	
	class RecordImagePanel extends JPanel implements Message, DBStatus
	{
		private static final long serialVersionUID = 1L;
		private String _fullProp;
		private Map<String, Integer> _mris;
		private String[] _mriNames;
		private JComboBox _mrisCombo;
		private GridBagConstraints _c;
		private JTextField _filenameField, _changedImageField, _directoryField;
		private JTextField _sexField, _examDateField, _birthDateField;
		private JTextField _patientSizeField;
		private JTextField _patientWeightField;
		private JTextField _xResField, _yResField, _zResField, _subjectIdField, _imageIdField, _subjectResult;
		private JButton _recordButton;
		private JTextArea _subjectNoteArea;
		private JTextArea _imageNoteArea;
		private JTextArea _messageArea;
		private DbConn _dbConn;
		private JTextField _dbField;
		private int _imageId;
		
		private Map<String, Integer> _arteryShapes;
	    private String[] _arteryShapeNames;
	    private Map<String, Integer> _variableArteries;
	    private String[] _variableArteryNames;
	    private JButton _recordVariableButton;
	    private List<JComboBox> _arteryShapeCombos;
	    private List<String> _currentArteryShapes = null;
	    private JButton _imageNoteButton, _subjectNoteButton;
	    private TempProperties _temp;
		public void message(String m)
		{
			_messageArea.append(m);
		}
		public void clear()
		{
			_messageArea.setText("");
		}
		public RecordImagePanel(String properties, int subjectId)
		{
			try
			{
				_temp = new TempProperties(TempProperties.ANERUYSM_TEMP);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			_arteryShapeCombos = new LinkedList<JComboBox>();
			_currentArteryShapes = new ArrayList<String>();
			_dbField = new JTextField(); // updated before its add to the panel 
			_dbConn = new DbConn();
			_dbConn.setDbStatus(this);
			_fullProp = DbConn.findPropertiesFile(properties);
			this.setLayout(new GridBagLayout());
	        _c = new GridBagConstraints();
	        _c.weightx = 0.0;
	        _c.weighty = 0.0;
	        _c.fill = GridBagConstraints.BOTH;
	        JLabel label = null;
	        
	        _c.gridy = 0;
	        _c.gridx = 0;
	        label = new JLabel("Filename");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _c.weightx = 1.0;
	        _c.gridwidth = GridBagConstraints.REMAINDER;
	        _filenameField = new JTextField(_filename);
	        _filenameField.setEditable(false);
	        this.add(_filenameField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = 1;
	        label = new JLabel("Changed/croped image");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _c.gridwidth -= GridBagConstraints.REMAINDER;
	        _changedImageField = new JTextField(); 
	        this.add(_changedImageField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.weightx = 0.0;
	        _c.gridwidth = 1;
	        label = new JLabel("Directory");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _c.gridwidth = GridBagConstraints.REMAINDER;
	        _directoryField = new JTextField(_directory);
	        _directoryField.setEditable(true);
	        this.add(_directoryField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = 1;
	        label = new JLabel("Subject_id");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _subjectIdField = new JTextField();
	        this.add(_subjectIdField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("X resolution");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _xResField = new JTextField();
	        this.add(_xResField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Y resolution");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _yResField = new JTextField();
	        this.add(_yResField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Z resolution");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _zResField = new JTextField();
	        this.add(_zResField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Subject sex: ");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _sexField = new JTextField("unknown");
	        this.add(_sexField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Birth date: ");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _birthDateField = new JTextField();
	        this.add(_birthDateField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Exam date: ");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _examDateField = new JTextField();
	        this.add(_examDateField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Patient size: ");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _patientSizeField = new JTextField();
	        this.add(_patientSizeField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Patient weight: ");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _patientWeightField = new JTextField();
	        this.add(_patientWeightField, _c);
	        
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				String rdgehdr = _dbConn.getRdgehdr();
				if (rdgehdr != null) _imageData3D.setRdgehdr(rdgehdr);
				//setDbField(_dbConn);
				Queries queries = new Queries(con);
				_mris = queries.mris();
				Set<String> keys = _mris.keySet();
				_mriNames = keys.toArray(new String[keys.size()]);
				_c.gridy++;
	            _c.gridx = 0;
	            label = new JLabel("MRI");
	            this.add(label, _c);
	            
	            _c.gridx = 1;
	            _mrisCombo = new JComboBox(_mriNames);
	            _mrisCombo.setSelectedIndex(1);
	            this.add(_mrisCombo, _c);
				
	            _variableArteries = queries.variableArteries();
	            keys = _variableArteries.keySet();
	            _variableArteryNames = keys.toArray(new String[keys.size()]);
	            Collection<Integer> vals = _variableArteries.values();
	            for (int varArteryId: vals)
	            {
	            	_currentArteryShapes.add(queries.subjectarteryShape(subjectId, varArteryId));
	            }
	            _arteryShapes = queries.arteryShapes();
	            keys = _arteryShapes.keySet();
	            _arteryShapeNames = keys.toArray(new String[keys.size()]);
	            // TODO get cropped filename based on subject_id 
	            
			}
			catch (FileNotFoundException e)
			{
				//message(e.getMessage());
				e.printStackTrace();
			}
			catch (IOException e)
			{
				//message(e.getMessage());
				e.printStackTrace();
			}
			catch (SQLException e)
			{
				//message(e.getMessage());
				e.printStackTrace();
			}
			finally
			{
				try
				{
				    if (con != null)
				        con.close();
				}
				catch (SQLException e)
				{
					message(e.getMessage());
					e.printStackTrace();
				}
			}
			_c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = GridBagConstraints.REMAINDER;
	        _recordButton = new JButton("Record image in aneurysm database");
	        _recordButton.addActionListener(new ActionListener(){
	        	public void actionPerformed(ActionEvent ae)
	        	{
	        		record();
	        	}
	        });
	        this.add(_recordButton, _c);
	        
	        _c.gridwidth = 1;
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Saved image_id");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _imageIdField = new JTextField();
	        _imageIdField.setEditable(false);
	        _imageIdField.setBackground(Color.YELLOW);
	        _imageIdField.setForeground(Color.BLUE);
	        this.add(_imageIdField, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        label = new JLabel("Subject");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _subjectResult = new JTextField();
	        _subjectResult.setEditable(false);
	        this.add(_subjectResult, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = GridBagConstraints.REMAINDER;
	        _c.weighty = 1.0;
	        _messageArea = new JTextArea();
	        _messageArea.setEditable(false);
	        _messageArea.setBackground(Color.LIGHT_GRAY);
	        JScrollPane scroll = new JScrollPane(_messageArea);
	        this.add(scroll, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = 1;
	        _c.weightx = 0.0;
	        _c.weighty = 0.25;
	        label = new JLabel("Subject note");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _c.weightx = 1.0;
	        _subjectNoteArea = new JTextArea();
	        _subjectNoteArea.setBorder(new EtchedBorder());
	        this.add(_subjectNoteArea, _c);
	        
	        _c.gridx = 2;
	        _c.weightx = 0.0;
	        _subjectNoteButton = new JButton("Record subject note");
	        _subjectNoteButton.setEnabled(false);
	        _subjectNoteButton.addActionListener(new ActionListener(){
	        	public void actionPerformed(ActionEvent ae)
	        	{
	        		recordSubjectNote();
	        	}
	        });
	        this.add(_subjectNoteButton, _c);
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = 1;
	        _c.weightx = 0.0;
	        label = new JLabel("Image note");
	        this.add(label, _c);
	        
	        _c.gridx = 1;
	        _c.weightx = 1.0;
	        _imageNoteArea = new JTextArea();
	        _imageNoteArea.setBorder(new EtchedBorder());
	        this.add(_imageNoteArea, _c);
	        
	        _c.gridx = 2;
	        _c.weightx = 0.0;
	        _imageNoteButton = new JButton("Record image note");
	        _imageNoteButton.setEnabled(false);
	        _imageNoteButton.addActionListener(new ActionListener(){
	        	public void actionPerformed(ActionEvent ae)
	        	{
	        		recordImageNote();
	        	}
	        });
	        this.add(_imageNoteButton, _c);
	        
	        int i = 0;
	        for(String varArtery: _variableArteryNames)
	        {
	            _c.gridy++;
	            _c.gridx = 0;
	            label = new JLabel(varArtery);
	            this.add(label, _c);
	            _c.gridx = 1;
	            JComboBox c = new JComboBox(_arteryShapeNames);
	            _arteryShapeCombos.add(c);
	            if (_currentArteryShapes.get(i) != null)
	            {
	            	c.setSelectedItem(_currentArteryShapes.get(i));
	            }
	            /* 
	            else if (varArtery.equals("Posterior Communicating left") ||
	            		varArtery.equals("Posterior Communicating right") ||
	            		varArtery.equals("Vertebral Artery left") ||
	            		varArtery.equals("Vertebral Artery right"))
	            {
	            	c.setSelectedItem("unknown");
	            }
	            */
	            else
	            {
	            	c.setSelectedItem(DEFAULT_ARTERY_SHAPE);
	            }
	            this.add(c, _c);
	            i++;
	        }
	        int y = _c.gridy;
	        _c.gridy-=(_variableArteryNames.length-1);
	        _c.gridwidth = 1;
	        _c.weightx = 0;
	        _c.weighty = 0;
	        _c.gridx = 2;
	        _c.gridheight = _variableArteryNames.length;
	        _c.gridwidth = 1;
	        _recordVariableButton = new JButton("Record Variable arteries");
	        _recordVariableButton.setEnabled(false);
	        _recordVariableButton.addActionListener(new ActionListener(){
	            public void actionPerformed(ActionEvent e)
	            {
	                recordVariableArteries();
	                _recordVariableButton.setBackground(Color.YELLOW);
	            }
	        });
	        this.add(_recordVariableButton, _c);
	        _c.gridy = y;
	        
	        _c.gridy++;
	        _c.gridx = 0;
	        _c.gridwidth = GridBagConstraints.REMAINDER;
	        _c.weightx = 1.0;
	        _c.weighty = 0.0;
	        //_dbField = new JTextField(); done earlier 
	        _dbField.setEditable(false);
	        _dbField.setBorder(new BevelBorder(BevelBorder.LOWERED));
	        this.add(_dbField, _c);
		}
		/** Record shape/presence/absence of arteries. */
		public void recordVariableArteries()
	    {
			int subjectId = Integer.parseInt(_subjectIdField.getText());
			int i = 0;
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				Queries q = new Queries(con);
				Inserts ins = new Inserts(con);
				Updates up = new Updates(con);
				for (JComboBox box: _arteryShapeCombos)
				{
					String s = (String)box.getSelectedItem();
					String a = _variableArteryNames[i];
					int arteryshapeId = _arteryShapes.get(s);
				    int arteryId = _variableArteries.get(a);
				    String curArteryShape = q.subjectarteryShape(subjectId, arteryId);
				    
				    if (curArteryShape == null)
				    {
				    	ins.insertSubjectartery(subjectId, arteryId, arteryshapeId);
				    }
				    else
				    {
				    	up.updateSubjectArtery(subjectId, arteryId, arteryshapeId);
				    }
				    
					i++;
				}
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
				    if (con!=null)
				        con.close();
				}
				catch (SQLException e)
				{
					message(e.getMessage());
					e.printStackTrace();
				}
			}
	    }
		public void setDbField(DbConn dbConn)
		{
			_dbField.setText(dbConn.getUrl()+" | "+dbConn.getUsername());
		}
		public void recordSubjectNote()
		{
			String note = _subjectNoteArea.getText();
			int subjectId = Integer.parseInt(_subjectIdField.getText());
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				//setDbField(_dbConn);
				Updates updates = new Updates(con);
				int records = updates.updateSubjectNote(note, subjectId);
				_subjectNoteButton.setBackground(Color.YELLOW);
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
				    if (con!=null)
				        con.close();
				}
				catch (SQLException e)
				{
					message(e.getMessage());
					e.printStackTrace();
				}
			}
			
		}
		public void recordImageNote()
		{
			String note = _imageNoteArea.getText();
			// int imageId = Integer.parseInt(_imageIdField.getText());
			int imageId = _imageId;
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				//setDbField(_dbConn);
				Updates updates = new Updates(con);
				int records = updates.updateImageNote(note, imageId);
				_imageNoteButton.setBackground(Color.YELLOW);
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
				    if (con!=null)
				        con.close();
				}
				catch (SQLException e)
				{
					message(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		
		/** Record image and subject information to database. */
		public void record()
		{
			_filename = _filenameField.getText();
			_directory = _directoryField.getText();
			String mri = _mriNames[_mrisCombo.getSelectedIndex()];
			int mriId = _mris.get(mri);
			int subjectId = Integer.parseInt(_subjectIdField.getText());
			float xRes = Float.parseFloat(_xResField.getText());
			float yRes = Float.parseFloat(_yResField.getText());
			float zRes = Float.parseFloat(_zResField.getText());
			String examDate = _examDateField.getText();
			String birthDate = _birthDateField.getText();
			String sex = _sexField.getText();
			float patientSize = Float.parseFloat(_patientSizeField.getText());
			float patientWeight = Float.parseFloat(_patientWeightField.getText());
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				//setDbField(_dbConn);
				Inserts inserts = new Inserts(con);
				
				boolean newSubject = inserts.insertNewSubject(subjectId, sex, birthDate);
				if (newSubject)
				{
					_subjectResult.setText("New subject ID: "+subjectId);
				}
				else
				{
					_subjectResult.setText("Existing subject ID: "+subjectId);
				}
				System.out.println("directory: "+_directory+" imageBase: "+_imageBase);
				String relativeDirectory = _directory.replace(_imageBase, "");
				_imageId = inserts.insertNewImage(_filename, relativeDirectory, mriId, xRes, yRes, zRes, 
						subjectId, examDate, patientSize, patientWeight, _imageData3D.getAgeAtExam());
				
				if (_imageId != 0)
				{
					_temp.setProperty("image_id", _imageId+"");
					_temp.setProperty("xres", xRes+"");
					_temp.setProperty("yres", yRes+"");
					_temp.setProperty("zres", zRes+"");
					_recordVariableButton.setEnabled(true);
					_imageNoteButton.setEnabled(true);
					_subjectNoteButton.setEnabled(true);
					Queries queries = new Queries(con);
					String changedIm = queries.imageChangedimage(_imageId);
					if (changedIm != null)
					{
						_changedImageField.setText(changedIm);
					}
					String imageNote = queries.imageNote(_imageId);
					if (imageNote != null)
					{
						_imageNoteArea.setText(imageNote);
					}
				}
				String changedimage = _changedImageField.getText();
				if (changedimage != null && changedimage.length() > 0)
				{
					Updates up = new Updates(con);
					up.updateImageChangedimage(_imageId, changedimage);
				}
				String newImage="New image ID: ";
				if(!inserts.isNewImage()) newImage="Existing image ID: ";
				_imageIdField.setText(newImage+_imageId);
				
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
				    if (con!=null)
				        con.close();
				}
				catch (SQLException e)
				{
					message(e.getMessage());
					e.printStackTrace();
				}
			}
		}
		public void load()
		{
			_filenameField.setText(_filename);
			_directoryField.setText(_directory);
			int subjectId = _imageData3D.getSubjectId();
			_subjectIdField.setText(subjectId+"");
			Connection con = null;
			try
			{	
				con = _dbConn.connect(_fullProp);
				Queries q = new Queries(con);
				String subjectNote = q.subjectNote(subjectId);
				if (subjectNote != null)
				{
					_subjectNoteArea.setText(subjectNote);
				}
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
				    if (con!=null)
				        con.close();
				}
				catch (SQLException e)
				{
					message(e.getMessage());
					e.printStackTrace();
				}
			}
			_xResField.setText(_imageData3D.getXRes()+"");
			_yResField.setText(_imageData3D.getYRes()+"");
			_zResField.setText(_imageData3D.getZRes()+"");
			_sexField.setText(_imageData3D.getSex());
			_birthDateField.setText(_imageData3D.getBirthDate());
			_examDateField.setText(_imageData3D.getExamDate());
			_patientSizeField.setText(_imageData3D.getPatientSize()+"");
			_patientWeightField.setText(_imageData3D.getPatientWeight()+"");
			
			int mriType = _imageData3D.getMRIType();
			if (mriType == ImageData3D.GE)
			{
				_mrisCombo.setSelectedItem(ImageData3D.GE_NAME);
			}
			else if (mriType == ImageData3D.SIEMENS)
			{
				_mrisCombo.setSelectedItem(ImageData3D.SIEMENS_NAME);
			}
			message(_imageData3D.toString());
			try 
			{
				_temp.setProperty("xres", _imageData3D.getXRes()+"");
				_temp.setProperty("yres", _imageData3D.getYRes()+"");
				_temp.setProperty("zres", _imageData3D.getZRes()+"");
			} 
			catch (IOException e) 
			{
				message(e.getMessage());
				e.printStackTrace();
			}
		}
	}
}

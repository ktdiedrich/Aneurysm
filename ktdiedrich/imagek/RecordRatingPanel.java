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
import ij.gui.ImageWindow;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import ktdiedrich.db.DBStatus;
import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.db.aneurysm.Queries;
import ktdiedrich.db.aneurysm.SubjectArtery;
import ktdiedrich.db.aneurysm.SubjectArteryValue;
import ktdiedrich.db.aneurysm.Updates;
import ktdiedrich.util.TempProperties;

public class RecordRatingPanel extends JPanel implements DBStatus 
{
	public static final String NONE = "None";
	public static final String HIDE_SUBJECT = "XXX";
	public static final int SCREEN_WIDTH=1000;
	public static final int INITIAL_Y = 300;
	public static final String ALL_ARTERY = "All arteries";
	private GridBagConstraints _c;
    private Map<String, Integer> _raters;
    
    private JComboBox _ratersCombo, _rateScaleCombo;
    private JButton _recordButton;
    private String _fullProp;
    private JButton _nextB;
    
    private DbConn _dbConn;
    private TempProperties _temp;
    private Map<String, Integer> _rateScales;
    private String[] _rateScaleNames;
    private JTextField _dbField;
    private JTextField _ratingField;
    private int _curLow, _curHigh;
    private String _rater;
    private String _rateScale;
    private int _raterId;
    private int _subjectId;
    private int _arteryId;
    private String _artery;
    private JTextField _subjectField;
    private JTextField _arterynameField;
    private JComboBox _arteryCombo;
    private JComboBox _allMeasureArteryCombo;
    private Map<String, Integer> _arteries;
    private Map<String, Integer> _allMeasureArteries;
    private ArteryViewer _arteryViewer;
    private List<ImagePlus> _displayImages;
    private Queue<SubjectArtery> _toRate;
    private SubjectArtery _nextSubArt;
    private JCheckBox _secreteCheck;
    private List<SubjectArteryValue> _trainingSet;
    private ListIterator<SubjectArteryValue> _trainingIterator;
    private JTextField _logField;
    public static JFrame makeRatingFrame()
	{
		JFrame ratingFrame = new JFrame();
		ratingFrame.setSize(650,300);
		ratingFrame.setTitle("Record_rating_aneurysm_database");
		ratingFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container contentPane = ratingFrame.getContentPane();
		RecordRatingPanel ratingPanel = new RecordRatingPanel();
		
		contentPane.add(ratingPanel);	
		return ratingFrame;
	}
    
    public RecordRatingPanel()
    {
    	this(DbConn.PROPERTIES);
    }
    public RecordRatingPanel(String properties)
    {
    	super();
    	_dbField = new JTextField();
    	JLabel label = null;
    	_arteryViewer = new ArteryViewer();
        try
        {
        	_temp = new TempProperties(TempProperties.ANERUYSM_TEMP);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
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
        _secreteCheck = new JCheckBox("Hidden mode");
        _secreteCheck.setSelected(true);
        add(_secreteCheck, _c);
        
        // TODO rate experiment artery 
        _c.gridx = 1;
        label = new JLabel("Rate artery");
        add(label, _c);
        
        _c.gridx=2;
        Connection con = null;
        try
        {   
            con = _dbConn.connect(_fullProp);
            Queries queries = new Queries(con);
            _allMeasureArteries = queries.measureArteries();
            _allMeasureArteries.put(ALL_ARTERY, 0);
            Set<String> keys = _allMeasureArteries.keySet();
        	_allMeasureArteryCombo = new JComboBox();
        	// TODO 
        	
        	_allMeasureArteryCombo.addItem(ALL_ARTERY);
        	for (String art: keys)
        	{	
        		_allMeasureArteryCombo.addItem(art);
        	}
        	_allMeasureArteryCombo.setSelectedItem(ALL_ARTERY);
        	_allMeasureArteryCombo.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		setRating();
            	}
            });
        	
        	add(_allMeasureArteryCombo, _c);
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
        _c.gridx = 3;
        _nextB = new JButton("Next rate display");
        _nextB.setEnabled(false);
        _nextB.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e)
        	{
        		if (_toRate == null)
        		{
        			setRating();
        		}
        		_nextSubArt = _toRate.poll();
        		if (!_secreteCheck.isSelected()) IJ.log("Next: "+_nextSubArt.toString());
        		setSubjectId( _nextSubArt.getSubjectId() );
        		setArtery( _nextSubArt.getArteryId(), _nextSubArt.getArterySideName() );
        	}
        });
        add(_nextB, _c);
        
        _c.gridy++;
        
        
        _rater = null;
        _rateScale = null;
        _c.gridx=0;
        label = new JLabel("Subject ID");
        this.add(label, _c);
        
        _c.gridx = 1;
        _c.gridwidth = 2;
        _c.weightx=1;
        _subjectField = new JTextField();
        _subjectField.setEditable(true);
        _subjectField.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e)
        	{
        		int subjectId = Integer.parseInt(_subjectField.getText());
        		setSubjectId(subjectId);
        		if (_arteries.containsKey(_artery))
        		{
        			_arteryCombo.setSelectedItem(_artery);
        		}
        		else if (_arteries.size() > 0 && _arteryCombo.getItemCount() > 0)
        		{
        			_arteryCombo.setSelectedIndex(0);
        		}
        	}
        } );
        
        this.add(_subjectField, _c);
        
        try
        {
        	 _rater = _temp.getProperty("rater");
        	 _rateScale = _temp.getProperty("ratescale");
        }
        catch (IOException e)
        {
        	IJ.log(e.getMessage());
        	e.printStackTrace();
        }

        con = null;
        try
        {   
            con = _dbConn.connect(_fullProp);
            Queries queries = new Queries(con);
            
            _trainingSet = queries.tortuosityTrainingSet();
            _trainingIterator = _trainingSet.listIterator();
            _raters = queries.raters();
            _raters.put(NONE, 0);
            Set<String> keys = _raters.keySet();
            String[] raterNames = keys.toArray(new String[keys.size()]);
            
            _c.gridy++;
            _c.gridx = 0;
            _c.weightx = 0.0;
            _c.gridwidth = 1;
            label = new JLabel("Raters");
            this.add(label, _c);
            
            _c.gridx = 1;
            _c.gridwidth = 2;
            _ratersCombo = new JComboBox(raterNames);
            _ratersCombo.setSelectedItem(NONE);
            _ratersCombo.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		setRating();
            	}
            });
            
            this.add(_ratersCombo, _c); 
            
            _c.gridy++;
            _c.gridx = 0;
            label = new JLabel("Artery name");
            this.add(label, _c);
            
            _c.gridx = 1;
            _c.gridwidth = 2;
            _arterynameField = new JTextField();
            _arterynameField.setEditable(false);
            _arteryCombo = new JComboBox();
            _arteryCombo.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		String artery = (String)_arteryCombo.getSelectedItem();
            		if (_arteries != null && artery != null)
            		{
            			int arteryId = _arteries.get(artery);
            			setArtery(arteryId, artery);
            		}
            	}
            });
            this.add(_arteryCombo, _c);
            
            _rateScales = queries.rateScales();
            keys = _rateScales.keySet();
            _rateScaleNames = new String[keys.size()+1];
            String[] vals = keys.toArray(new String[keys.size()]); 
            for (int i=0; i<keys.size(); i++)
            {
            	_rateScaleNames[i] = vals[i];
            }
            	
            _c.gridy++;
            _c.gridx = 0;
            _c.gridwidth = 1;
            _c.weightx = 0.0;
            
            label = new JLabel("Rating scales");
            this.add(label, _c);
            
            _c.gridx = 1;
            _c.gridwidth = 2;
            _rateScaleCombo = new JComboBox(_rateScaleNames);
            _rateScaleCombo.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		setRating();
            		String rs = (String)_rateScaleCombo.getSelectedItem();
            		
                    Pattern subject = Pattern.compile("(\\d+)\\-(\\d+)");
                	Matcher mat = subject.matcher(rs);
                	while (mat.find())
                	{
                		_curLow= Integer.parseInt(mat.group(1));
                		_curHigh = Integer.parseInt(mat.group(2));
                		IJ.log("Low: "+_curLow+" High: "+_curHigh);
                		
                	}
                	
            	}
            });
            if (_rateScale != null)
            {
            	_rateScaleCombo.setSelectedItem(_rateScale);
            }
            else
            {
            	_rateScaleCombo.setSelectedIndex(0);
            }
            this.add(_rateScaleCombo, _c); 
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
        
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = 1;
        label = new JLabel("Rating");
        this.add(label, _c);
        
        _c.gridx=1;
        _c.gridwidth = GridBagConstraints.REMAINDER;
        _ratingField = new JTextField();
        this.add(_ratingField, _c);
        
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = GridBagConstraints.REMAINDER;
        _recordButton = new JButton("Record rating to database");
        _recordButton.addActionListener(new RecordAction());
        _recordButton.setEnabled(false);
        this.add(_recordButton, _c);
        
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = GridBagConstraints.REMAINDER;
        _c.weightx = 1.0;
        _c.weighty = 0.0;
        _dbField.setEditable(false);
        _dbField.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.add(_dbField, _c);
        
        if (_rater != null)
        {
        	_ratersCombo.setSelectedItem(_rater);
        }
        JButton forwardTraining = new JButton("Next training");
        forwardTraining.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		if (_trainingIterator.hasNext())
            		{
            			SubjectArteryValue trainer = _trainingIterator.next();
            			logTrainer(trainer);
            		}
            	}
            	});
        
        JButton backTraining = new JButton("Previous training");
        backTraining.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		if (_trainingIterator.hasPrevious())
            		{
            			SubjectArteryValue trainer = _trainingIterator.previous();
            			logTrainer(trainer);
            		}
            	}
            	});
        _c.gridy++;
        _c.gridwidth = 1;
        _c.gridx = 0;
        this.add(forwardTraining, _c);
        _c.gridx = 1;
        this.add(backTraining, _c);
        this.add(backTraining);
        
        _logField = new JTextField("");
        _logField.setEditable(false);
        _logField.setBorder(new BevelBorder(BevelBorder.LOWERED));
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(_logField, _c);
    }
   
    class RecordAction implements ActionListener
    {
		public void actionPerformed(ActionEvent arg0) 
		{
			boolean b = record();
			if (b) 
			{	//_recordButton.setEnabled(false);
				_recordButton.setBackground(Color.YELLOW);
			}
		}
    }
    private void logTrainer(SubjectArteryValue trainer)
    {
    	setSubjectId(trainer.getSubjectId());
    	setArtery(trainer.getArteryId(), trainer.getArteryName()+" "+trainer.getSide());
    	
    	_logField.setText("Training: Subject ID: "+trainer.getSubjectId()+", Artery: "+
    			trainer.getArteryName()+" "+trainer.getSide()+", Value: "+trainer.getValue());
    }
    private void setRating()
    {
    	
    	//if (_ratersCombo!=null)
    //	{
	    	_rater = (String)_ratersCombo.getSelectedItem();
	    	_raterId = _raters.get(_rater);
	    	IJ.log("Rater: "+_rater+" "+_raterId);
	    	if (_raterId > 0)
	    	{
		    	String rateScale = (String)_rateScaleCombo.getSelectedItem();
		    	int rateScaleId = _rateScales.get(rateScale);
		    	String measureArtery = (String)_allMeasureArteryCombo.getSelectedItem();
		    	int measureArteryId = _allMeasureArteries.get(measureArtery);
		    	IJ.log("Artery: "+measureArtery+" "+measureArteryId);
		    	Connection con = null;
		        try
		        {   
		            con = _dbConn.connect(_fullProp);
		            
		            
		            Queries queries = new Queries(con);
		            int rateExperimentId = queries.rateExperimentToday(_raterId, rateScaleId);
		            _toRate = queries.subjectArteryToRate(_raterId, rateExperimentId, measureArteryId);
		            IJ.log("Samples to rate: "+_toRate.size());
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
		        _nextB.setEnabled(true);
		        _recordButton.setEnabled(true);
	    	}
	    	else
	    	{
	    		if (_nextB != null) _nextB.setEnabled(false);
	    		if (_recordButton != null) _recordButton.setEnabled(false);
	    	}
    //	}
    }
    /** @return true on successful rate recording .*/
    private boolean record()
    {
    	boolean success = false;
    	String rater = (String)_ratersCombo.getSelectedItem();
    	int raterId = _raters.get(rater);
    	
    	String rateScale = (String)_rateScaleCombo.getSelectedItem();
    	int rateScaleId = _rateScales.get(rateScale);
    	int rating = Integer.parseInt(_ratingField.getText());
    	if (!_secreteCheck.isSelected())
    	{
    		IJ.log("Record rater ID: "+raterId+", Rater: "+rater+", Rate scale ID: "+rateScaleId+
    			", scale: "+rateScale+", rating: "+rating+", subjectID: "+_subjectId+", artery ID: "+
    			_arteryId+" "+_artery);
    	}
    	if (rating >= _curLow && rating <= _curHigh)
    	{
    		int rateExperimentId = 0; 
    		Connection con = null;
            try
            {   
                con = _dbConn.connect(_fullProp);
                Inserts ins = new Inserts(con);
                rateExperimentId = ins.enterRateExperiment(rateScaleId, raterId);
                int insRet = ins.insertRating(rateExperimentId, _subjectId, _arteryId, rating);
                //IJ.log("Insert: "+insRet);
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
            
    		success = true;
    		try
    		{
    			_temp.setProperty("rater", _rater);
    			_temp.setProperty("rater_id", ""+_raterId);
    			_temp.setProperty("ratescale", rateScale);
    		}
    		catch (IOException e)
    		{
    			IJ.log("Setting temporay properties: "+e.getMessage());
    			e.printStackTrace();
    		}
    	}
    	if (_nextSubArt != null && _nextSubArt.getSubjectId() == _subjectId && 
    			_nextSubArt.getArteryId() == _arteryId)
    	{
    		if (_toRate != null && _toRate.size() > 0)
    		{
    			// TODO  decide when to remove elements from rating queue 
    			// _toRate.remove(0);
    		}
    	}
    	return success;
    }
	public void setDbField(DbConn dbConn)
	{
		_dbField.setText(dbConn.getUrl()+" | "+dbConn.getUsername());
    }
	public int getSubjectId() {
		return _subjectId;
	}
	public void setSubjectId(int subjectId) {
		_arteryCombo.removeAllItems();
		_subjectId = subjectId;
		if (_secreteCheck.isSelected())
		{
			_subjectField.setText(HIDE_SUBJECT);
		}
		else
		{
			_subjectField.setText(subjectId+"");
		}
		// IJ.log("Current subjectId: "+_subjectId);
		
		Connection con = null;
        try
        {   
            con = _dbConn.connect(_fullProp);
            Queries queries = new Queries(con);
            _arteries = queries.arteriesForSubject(subjectId);
            if (_arteries.size() > 0)
            {
            	Set<String> keys = _arteries.keySet();
            	String[] arteryNames = keys.toArray(new String[keys.size()]);
            	
            	for (String art: arteryNames)
            	{	
            		_arteryCombo.addItem(art);
            	}
            }
            else
            {
            	_arteries.put("None", 0);
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
	public void setArtery(int arteryId, String artery)
	{
		_arteryId = arteryId;
		_artery = artery;
		IJ.log("Set artery: "+_arteryId+" "+_artery);
		_arteryCombo.setSelectedItem(artery);
		if (_displayImages != null)
		{
			for (ImagePlus dim: _displayImages)
			{
				dim.close();
			}
		}
		if (arteryId > 0)
		{
			_displayImages = _arteryViewer.makeArteryDisplays(_subjectId, _arteryId, _artery);
			int locX = 0;
			int locY = INITIAL_Y;
			if (_secreteCheck.isSelected())
			{
				for (ImagePlus im: _displayImages)
				{
					hideSubjectId(im);
				}
			}
			
			for (ImagePlus im: _displayImages)
			{
				im.show();
				ImageWindow win = im.getWindow();
				win.setLocation(locX, locY);
				win.setVisible(true);
				im.updateAndDraw();
				
				locX+=win.getWidth();
				if (locX > SCREEN_WIDTH)
				{
					locX=0;
					locY+=win.getHeight();;
				}
			}
			
		}
		_recordButton.setBackground(Color.LIGHT_GRAY);
	}
	public void hideSubjectId(ImagePlus image)
	{
		String title = image.getTitle();
		String hidden = title.replaceAll("\\d+", HIDE_SUBJECT);
		image.setTitle(hidden);
	}
}

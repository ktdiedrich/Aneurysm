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

import ij.ImagePlus;
import ij.ImageStack;
import ij.io.OpenDialog;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.BevelBorder;

import java.awt.*;
import java.awt.event.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.List;

import ktdiedrich.db.DBStatus;
import ktdiedrich.db.DbConn;
import ktdiedrich.db.aneurysm.Inserts;
import ktdiedrich.db.aneurysm.Queries;
import ktdiedrich.util.TempProperties;
/** 
 * @author ktdiedrich@gmail.com
 * */
public class RecordDFMTortuosityPanel extends JPanel implements Message, DBStatus 
{
    private GridBagConstraints _c;
    private Map<String, Integer> _measureArteries;
    private String[] _measureArteryNames;
    
    private Point2PointDFM _p2pdfm;
    private JTextField _segmentationIdField, _tortuosityIdField, _centerlineIdField;
    private JComboBox _arteriesCombo, _selectCombo;
    private JButton _recordButton;
    private String _fullProp;
    
    private int _centerlineId;
    private JTextArea _messageArea;
    private Centerlines _centerlines;
    private DbConn _dbConn;
    private TempProperties _temp;
    private Map<String, Integer> _selectArteries;
    private String[] _selectArteryNames;
	public RecordDFMTortuosityPanel(String properties)
    {
        super();
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
        _centerlineId = 0;
        _recordButton = new JButton("Record DFM scores to database");
        
        this.setLayout(new GridBagLayout());
        _c = new GridBagConstraints();
        _c.weightx = 0.0;
        _c.weighty = 0.0;
        _c.fill = GridBagConstraints.BOTH;
        _c.gridy = 0;
        _c.gridx = 0;
        _dbField = new JTextField();
        
        JLabel label = new JLabel("Segmentation_id");
        this.add(label, _c);
        
        _c.gridx = 1;
        _c.weightx = 1.0;
        _segmentationIdField = new JTextField();
        try
        {
        	String segId = _temp.getProperty("segmentation_id");
        	_segmentationIdField.setText(segId);
        }
        catch (IOException e)
        {
        	e.printStackTrace();
        }
        this.add(_segmentationIdField, _c);

        Connection con = null;
        try
        {   
            con = _dbConn.connect(_fullProp);
            Queries queries = new Queries(con);
            
            _measureArteries = queries.measureArteries();
            Set<String> keys = _measureArteries.keySet();
            _measureArteryNames = keys.toArray(new String[keys.size()]);
            Arrays.sort(_measureArteryNames);
            _c.gridy++;
            _c.gridx = 0;
            _c.weightx = 0.0;
            label = new JLabel("Measure artery");
            this.add(label, _c);
            
            _c.gridx = 1;
            _arteriesCombo = new JComboBox(_measureArteryNames);
            _arteriesCombo.setSelectedIndex(0);
            _arteriesCombo.addActionListener(new ActionListener()
            {
            	public void actionPerformed(ActionEvent e)
            	{
            		_tortuosityIdField.setText("");
            		_recordButton.setBackground(Color.LIGHT_GRAY);
            		String an = (String)_arteriesCombo.getSelectedItem();
            		measureArtery(an);
            	}
            });
            this.add(_arteriesCombo, _c); 
            
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
        
        
        
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = 1;
        label = new JLabel("Saved centerline_id");
        this.add(label, _c);
        
        _c.gridx = 1;
        _centerlineIdField = new JTextField();
        _centerlineIdField.setEnabled(false);
        _centerlineIdField.setBackground(Color.YELLOW);
        _centerlineIdField.setDisabledTextColor(Color.BLUE);
        this.add(_centerlineIdField, _c);
        
        _c.gridy++;
        _c.gridx = 0;
        label = new JLabel("Saved tortuosity_id");
        this.add(label, _c);
        
        _c.gridx = 1;
        _tortuosityIdField = new JTextField();
        _tortuosityIdField.setEnabled(false);
        _tortuosityIdField.setBackground(Color.YELLOW);
        _tortuosityIdField.setDisabledTextColor(Color.BLUE);
        this.add(_tortuosityIdField, _c);
        
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = 2;
        
        _recordButton.addActionListener(new RecordAction());
        _recordButton.setEnabled(false);
        this.add(_recordButton, _c);
        
        _c.gridy++;
        _c.gridx = 0;
        _c.weighty = 1.0;
        _c.gridwidth = 2;
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
        JButton overlayCenterlineButton = new JButton("Overlay centerline");
        overlayCenterlineButton.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		overlayCenterline();
        	}
        });
       this.add(overlayCenterlineButton, _c);
       try
       {   
           con = _dbConn.connect(_fullProp);
           Queries queries = new Queries(con);
           
           _selectArteries = queries.selectArteries();
           Set<String> keys = _selectArteries.keySet();
           _selectArteryNames = new String[keys.size()+1];
           _selectArteryNames[0] = "None";
           String[] vals = keys.toArray(new String[keys.size()]); 
           for (int i=0; i<keys.size(); i++)
           {
        	   _selectArteryNames[i+1] = vals[i];
           }
           _c.gridy++;
           _c.gridx = 0;
           _c.weightx = 0.0;
           label = new JLabel("Highlight artery");
           this.add(label, _c);
           
           _c.gridx = 1;
           _selectCombo = new JComboBox(_selectArteryNames);
           _selectCombo.setSelectedIndex(0);
           _selectCombo.addActionListener(new ActionListener(){
              public void actionPerformed(ActionEvent e)
              {
           	   String selectArtery = (String)_selectCombo.getSelectedItem();
           	   highlightArtery(selectArtery);
                  
              }
           });
           this.add(_selectCombo, _c);
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
       
       
       
        _c.gridy++;
        _c.gridx = 0;
        _c.gridwidth = GridBagConstraints.REMAINDER;
        _c.weightx = 1.0;
        _c.weighty = 0.0;
        _dbField.setEditable(false);
        _dbField.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.add(_dbField, _c);
        
        
	}
	/** Draw centerline over unsegmented data. */
	public void overlayCenterline()
	{
		OpenDialog od = new OpenDialog("Open Raw 16 bit 3D image...", null);
        
        String directory = od.getDirectory();
        String filename = od.getFileName();
        if (filename==null)
            return; 
        ImageData3D imageData3D = new ImageData3D();
        imageData3D.setDisplay(true);
        
        ImagePlus image = null;
        try
        {
        	image = imageData3D.getImage(directory, filename);
        	
        }
        catch(IOException ex)
        {
        	message(ex.getMessage());
        }
        catch (ParseException ex)
        {
        	message(ex.getMessage());
        }
        if (image != null)
        {
        	Overlay overlay = new Overlay();
        	ImageStack stack = image.getStack();
    	    ImageStack rStack = new ImageStack(image.getHeight(), image.getWidth());
    	    for (int i=1; i<= stack.getSize(); i++)
    	    {
    	    	ImageProcessor ip = stack.getProcessor(i);
    	    	rStack.addSlice(""+i, ip.rotateRight());
    	    }
    	    image.setStack(image.getShortTitle(), rStack);
    	    
    	    int[] red = new int[3];
    	    red[0] = 255; red[1] = 0; red[2] = 0;
        	ImagePlus unsegDFMcenterline = overlay.overlayColorCenterline(image, 
        			_centerlines.getCenterlineGraphs(), red);
        	unsegDFMcenterline.show();
        	unsegDFMcenterline.updateAndDraw();
        }
        
	}
	private JTextField _dbField;
	public void setDbField(DbConn dbConn)
	{
		_dbField.setText(dbConn.getUrl()+" | "+dbConn.getUsername());
    }
    class RecordAction implements ActionListener
    {
		public void actionPerformed(ActionEvent arg0) 
		{
			recordTortuosity();
			_recordButton.setBackground(Color.YELLOW);
		}
    }
    
    public void message(String m)
    {
    	if (_messageArea != null)
    	{
    		_messageArea.append(m+"\n");
    	}
    	else
    	{
    		System.out.println(m+"\n");
    	}
    }
    public void clear()
    {
    	if (_messageArea != null)
    	{
    		_messageArea.setText("");
    	}
    }
    
    /** Select and highlight an artery */ 
    public void highlightArtery(String selectArtery)
    {
    	// TODO names match database aneurysm.selectartery
        
        message("Selected: "+selectArtery+".");
        Centerline arteryCenterline = null;
        IdentifyArteries identifyArteries = _p2pdfm.getIdentifyArteries();
        if (selectArtery.equals("Basilar Artery single"))
        {
     	   //System.out.println("Matched: "+selectArtery);
            arteryCenterline = identifyArteries.getBasilar();
        }
        else if (selectArtery.equals("Internal Carotid Artery left"))
        {
     	   //System.out.println("Matched: "+selectArtery);
            arteryCenterline = identifyArteries.getIcaLeft();
        }
        else if (selectArtery.equals("Internal Carotid Artery right"))
        {
     	   //System.out.println("Matched: "+selectArtery);
            arteryCenterline = identifyArteries.getIcaRight();
        }
        else if (selectArtery.equals("Anterior Cerebral Artery left"))
        {
     	   arteryCenterline = identifyArteries.getAcaLeft();
        }
        else if (selectArtery.equals("Anterior Cerebral Artery right"))
        {
     	   arteryCenterline = identifyArteries.getAcaRight();
        }
        else if (selectArtery.equals("Middle Cerebral Artery left"))
        {
     	   arteryCenterline = identifyArteries.getMcaLeft();
        }
        else if (selectArtery.equals("Middle Cerebral Artery right"))
        {
     	   arteryCenterline = identifyArteries.getMcaRight();
        }
        
        if (arteryCenterline != null)
        {
            //message("Marking: " +arteryCenterline);
            _p2pdfm.markCenterline(arteryCenterline);
        }
    }
    
    public void measureArtery(String arteryName)
    {
    	// TODO match names to database aneurysm.measureartery
    	message("Measure artery: "+arteryName);
    	IdentifyArteries ia = _p2pdfm.getIdentifyArteries();
    	GraphNode a = null, b = null;
    	if (arteryName.equals("L to R Anterior Cerebral single"))
    	{
    		Set<GraphNode> s = ia.getLeftICAMCABifurcation();
    		if (s.size() > 0)
    		{
    			GraphNode[] ar = s.toArray(new GraphNode[s.size()]);
    			a = ar[0];
    		}
    		s = ia.getRightICAMCABifurcation();
    		if (s.size() > 0)
    		{
    			GraphNode[] ar = s.toArray(new GraphNode[s.size()]);
    			b = ar[0];
    		}
    	}
    	if (arteryName.equals("Internal Carotid Artery left"))
    	{
    		Set<GraphNode> s = ia.getLeftICAMCABifurcation();
    		if (s.size() > 0)
    		{
    			GraphNode[] ar = s.toArray(new GraphNode[s.size()]);
    			a = ar[0];
    		}
    		b = ia.getLeftICAend();
    	}
    	if (arteryName.equals("Internal Carotid Artery right"))
    	{
    		Set<GraphNode> s = ia.getRightICAMCABifurcation();
    		if (s.size() > 0)
    		{
    			GraphNode[] ar = s.toArray(new GraphNode[s.size()]);
    			a = ar[0];
    		}
    		b = ia.getRightICAend();
    	}
    	 // Set ends and measure tortuosity.
    	if (a != null  && b != null)
    	{
    		_p2pdfm.searchA(a.col, a.row, a.z);
    		_p2pdfm.searchB(b.col, b.row, b.z);
    	}
    }
    /** Save tortuosity scores to database. */
	public void recordTortuosity()
	{
		int segmentationId = Integer.parseInt(_segmentationIdField.getText());
		
		String artery = _measureArteryNames[_arteriesCombo.getSelectedIndex()];
		int arteryId = _measureArteries.get(artery);
		
		Connection con = null;
		try
		{	
			con = _dbConn.connect(_fullProp);
			Inserts inserts = new Inserts(con);
			if (_centerlineId == 0)
			{
				_centerlineId = inserts.insertCenterline(_centerlines.getDfeThreshold(), 
				_centerlines.getMinLineLength(), segmentationId, _centerlines.getA(), 
				_centerlines.getB(), _centerlines.getXRes(), _centerlines.getYRes(),
				_centerlines.getZRes(), _centerlines.getCenterlineAlgorithm());
			}
			_centerlineIdField.setText(_centerlineId+"");
			GraphNode startNode = _p2pdfm.getNodeA();
			GraphNode endNode = _p2pdfm.getNodeB();
			int tortuosityId = inserts.insertCenterlineTortuosity(arteryId, _centerlineId,
					startNode.col, startNode.row, startNode.z, endNode.col, endNode.row, endNode.z,
					_centerlines.getTortuosityAlg());
			_tortuosityIdField.setText(tortuosityId+"");
			for (DFM dfm: _p2pdfm.getForwardDfms())
			{
				inserts.insertDFM(tortuosityId, dfm.getL(), dfm.getD(), dfm.getDfm(), dfm.getDfe(), "forward");
				inserts.insertCoordinate(tortuosityId, dfm.getX(), dfm.getY(), dfm.getZ());
			}
			//for (DFM dfm: _p2pdfm.getReverseDfms())
			//{
			//	inserts.insertDFM(tortuosityId, dfm.getL(), dfm.getD(), dfm.getDfm(), dfm.getDfe(), "reverse");
			//}
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
	/** make a dialog for saving tortuosity data. */
	public static JFrame makeSaveTortuosityFrame(RecordDFMTortuosityPanel panel)
	{
		JFrame frame = new JFrame();
		frame.setSize(500,400);
		frame.setTitle("Record_DFM_aneurysm_database");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		Container contentPane = frame.getContentPane();
		contentPane.add(panel);	
		return frame;
	}
	public Centerlines getCenterlines() 
	{
		return _centerlines;
	}
	public void setCenterlines(Centerlines centerlines) 
	{
		_centerlines = centerlines;
		if (_centerlines != null && _p2pdfm != null)
		{
			_recordButton.setEnabled(true);
		}
	}
	public void setPoint2PointDFM(Point2PointDFM p2pdfm)
    {
    	_p2pdfm = p2pdfm;
    	if (_centerlines != null && _p2pdfm != null)
    		_recordButton.setEnabled(true);
    }
}


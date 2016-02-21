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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Tracks centerlines of identified arteries. 
 * @author ktdiedrich@gmail.com */
public class IdentifyArteries
{
    private Centerline _icaLeft;
    private Centerline _icaRight;
    private Centerline _basilar;
    private Centerline _acaLeft;
    private Centerline _acaRight;
    private Centerline _acomm;
    private Centerline _mcaLeft;
    private Centerline _mcaRight;
    private Set<GraphNode> _leftICAMCABifurcation;
    private Set<GraphNode> _rightICAMCABifurcation;
    private GraphNode _leftICAend, _rightICAend;
    
    public static final float LOWER_PORTION = 0.15F;
    public static final float MID_PORTION = 0.10F;
    public static final int MCA_MIN_LENGTH = 75;
    public static final float MCA_CENTER_PORTION = 0.30F;
    public static final int ACA_MIN_LENGTH = 40;
    public static final int LOWER_ICA_IN = 0; // now centering the lower end of the ICA 
    public IdentifyArteries(List<CenterlineGraph> centerlineGraphs, int width, int height, int zSize)
    {
        float[] highAveDFE = new float[3];
        Centerline[] highDFECent = new Centerline[3];
        int lowerBound = zSize  - (int)(zSize * LOWER_PORTION);
        int[] top = new int[2];
        for (int i=0; i < top.length; i++)
        {
        	top[i] = Integer.MAX_VALUE;
        }
        Centerline[] topCent = new Centerline[2];
        float midX = ((float)width)/2;
        float midXleft = midX - midX*MID_PORTION;
        float midXright = midX + midX*MID_PORTION;
        int mostMinX = Integer.MAX_VALUE, mostMaxX = 0;
        for (CenterlineGraph centGraph: centerlineGraphs)
        {
            for (Centerline line: centGraph.getCenterlines())
            {
            	List<GraphNode> nodes = line.getCenterlineNodes();
            	// carotids and basilar 
                float aveDFE = line.getAverageDFE();
                int maxZ = line.getMaxZ();
                if (maxZ > lowerBound)
                {
                	if (aveDFE > highAveDFE[0])
                
	                {
	                    highAveDFE[2] = highAveDFE[1];
	                    highDFECent[2] = highDFECent[1];
	                    
	                    highAveDFE[1] = highAveDFE[0];
	                    highDFECent[1] = highDFECent[0];
	                    
	                    highAveDFE[0] = aveDFE;
	                    highDFECent[0] = line;
	                }
	                else if (aveDFE > highAveDFE[1])
	                {
	                    highAveDFE[2] = highAveDFE[1];
	                    highDFECent[2] = highDFECent[1];
	                    highAveDFE[1] = aveDFE;
	                    highDFECent[1] = line;
	                }
	                else if (aveDFE > highAveDFE[2])
	                {
	                    highAveDFE[2] = aveDFE;
	                    highDFECent[2] = line;
	                }
                }
                // ACAs are long and in middle check length 
                int minZ = line.getMinZ();
                float aveX = line.getAverageX();
                if (line.getCenterlineNodes().size() > ACA_MIN_LENGTH && aveX > midXleft && aveX < midXright)
                {
	                if (minZ < top[0])
	                {
	                	top[1] = top[0];
	                	topCent[1] = topCent[0];
	                	
	                	top[0] = minZ;
	                	topCent[0] = line;
	                }
	                else if (minZ < top[1])
	                {
	                	top[1] = minZ;
	                	topCent[1] = line; 
	                }
                }
                // MCAs start proximal and extend out to extremes in the x direction  
                // Check proximal MCA starting points 
                float mcaProxXleft = midX - midX*MCA_CENTER_PORTION;
                float mcaProxXright = midX + midX*MCA_CENTER_PORTION;
                
                // check MCAs mostly moving in X direction
                if (nodes.size() > 0)
                {
	                GraphNode nStart = nodes.get(0);
	            	GraphNode nEnd = nodes.get(nodes.size()-1);
	            	int rise = Math.abs(nStart.row - nEnd.row);
	            	int run = Math.abs(nStart.col - nEnd.col);
	                
	                if ((run > rise) && line.getCenterlineNodes().size() > MCA_MIN_LENGTH)
	                {
	                	int minX = line.getMinX();
	                	int maxX = line.getMaxX();
	                	// MCA left
	                	if (maxX > mcaProxXleft && minX < mostMinX)
	                	{
	                		mostMinX = minX;
	                		_mcaLeft = line;
	                	}
	                	// MCA right 	
	                	if (minX < mcaProxXright && maxX > mostMaxX)
	                	{
	                		mostMaxX = maxX;
	                		_mcaRight = line;
	                	}
	                }
                }
            }
        }
        // set ACAs
        if (topCent[0] != null && topCent[1] != null)
        {
        	if (topCent[0].getAverageX() < topCent[1].getAverageX())
        	{
        		_acaLeft = topCent[0];
        		_acaRight = topCent[1];
        	}
        	else
        	{
        		_acaLeft = topCent[1];
        		_acaRight = topCent[0];
        	}
        }
        float leftICApos=Float.MAX_VALUE, rightICApos=0;
        float basilarY = 0;
        int i = 0;
        int basilarIndex = 0;
        
        // set  Basilar based on position 
        /*
        
        for (Centerline l: highDFECent)
        {
            if (l != null && l.getAverageY() > basilarY)
            {
                basilarY = l.getAverageY();
                _basilar = l;
                basilarIndex = i;
            }
            i++;
        }
        */
        // set basilar on size; smaller radius than ICAs 
        basilarIndex = 2;
        _basilar = highDFECent[basilarIndex];
        
        // set left and right ICA
        i = 0;
        List<Centerline> potentialICAs = new ArrayList<Centerline>(2);
        for (Centerline l: highDFECent)
        {
            if (i != basilarIndex)
            {
                potentialICAs.add(l);
            }
            i++;
        }
        for (Centerline l: potentialICAs)
        {	
        	if (l != null)
        	{		
        		GraphNode n = l.getMaxZnode();
        		float x = n.col;
        		if (x < leftICApos)
        		{
        			leftICApos = x;
        			_icaLeft = l;
        		}
        		if (x > rightICApos)
        		{
        			rightICApos = x;
        			_icaRight = l;
        		}
        	}
        }
        // ICA-MCA bifurcations 
        // System.out.println("Left ICA MCA bifs:");
        Set<GraphNode> leftIcaMca = overlapBifurcations(_icaLeft, _mcaLeft);
        //System.out.println("Right ICA MCA bifs:");
        Set<GraphNode> rightIcaMca = overlapBifurcations(_icaRight, _mcaRight);
        if (leftIcaMca.size() >= 0)
        {
        	_leftICAMCABifurcation = leftIcaMca;
        }
        if (rightIcaMca.size() >= 0)
        {
        	_rightICAMCABifurcation = rightIcaMca;
        }
        // lower ICA ends
        if (_icaLeft != null)
        	_leftICAend = getLowerEnd(_icaLeft, true, midX);
        if (_icaRight != null)
        	_rightICAend = getLowerEnd(_icaRight, false, midX);
    }
    public GraphNode getLowerEnd(Centerline centerline, boolean left, float midX)
    {
    	GraphNode lower = null, upper=null;
    	List<GraphNode> nodes = centerline.getCenterlineNodes();
    	int ia = 0;
    	GraphNode a = nodes.get(ia);
    	int ib = nodes.size()-1;
    	GraphNode b = nodes.get(ib);
    	// greater z is lower
    	// check if only one end is on the correct side 
    	if (left)
    	{
    		if (a.col < midX && b.col > midX) return a;
    		else if (a.col > midX && b.col < midX) return b;	
    	}
    	else //right 
    	{
    		if (a.col > midX && b.col < midX) return a;
    		else if (a.col < midX && b.col > midX) return b;
    	}
    	
    	
    	if (b.z > a.z)
    	{	
    		lower = nodes.get(ib-LOWER_ICA_IN);
    	}
    	else
    	{
    		lower = nodes.get(ia+LOWER_ICA_IN);
    	}
    	// TODO merge centerlines ending at source 
    	
    	return lower;
    }
    public Centerline mergeLines(GraphNode end)
    {
    	Centerline merged = new Centerline();
    	Centerline cent = end.centerline;
    	GraphNode source = cent.getCenterlineGraph().getSourceNode();
    	Set<Centerline> connectedCenterlines = new HashSet<Centerline>();
    	if (end.equals(source))
    	{
    		System.out.println("Lower source node");
    		// TODO get next centerline below 
    		for (GraphNode adj: end.adjacents)
    		{
    			if (adj.centerline != null && !adj.centerline.equals(cent) )
    			{
    				System.out.println("Adjacent centerline");
    				connectedCenterlines.add(adj.centerline);
    			}
    		}
    		if (connectedCenterlines.size() > 0)
    		{
    			Centerline[] lines = connectedCenterlines.toArray(new Centerline[connectedCenterlines.size()]);
    			
    		}
    	}
    	return merged;
    }
    
    public Set<GraphNode> overlapBifurcations( Centerline aCenterline, Centerline bCenterline)
    {
    	
    	Set<GraphNode> overlap = new HashSet<GraphNode>();
    	if (aCenterline == null || bCenterline == null)
    		return overlap;
        List<GraphNode> aBifs  = new LinkedList<GraphNode>();
        List<GraphNode> bBifs = new LinkedList<GraphNode>();
        for (GraphNode cn: aCenterline.getCenterlineNodes())
        {
        	for (GraphNode an : cn.adjacents)
        	{
        		if (an.isBifurcation)
        		{
        			aBifs.add(an);
        		}
        	}
        }
        for (GraphNode cn: bCenterline.getCenterlineNodes())
        {
        	for (GraphNode an : cn.adjacents)
        	{
        		if (an.isBifurcation)
        		{
        			bBifs.add(an);
        		}
        	}
        }
        //System.out.println("A bifs: "+aBifs.size()+" B bifs: "+bBifs.size());
        for (GraphNode a: aBifs)
        {
        	for (GraphNode b: bBifs)
        	{
        		if (a.col==b.col && a.row==b.row && a.z==b.z)
        		{
        			// System.out.println("Overlapping bifurcation");
        			overlap.add(a);
        		}
        	}
        }
        //System.out.println("Overlaps: "+overlap.size());
        return overlap;
    }
    public Centerline getIcaLeft()
    {
        return _icaLeft;
    }
    
    public Centerline getIcaRight()
    {
        return _icaRight;
    }
    
    public Centerline getBasilar()
    {
        return _basilar;
    }
    
    public Centerline getAcaLeft()
    {
        return _acaLeft;
    }
    
    public Centerline getAcaRight()
    {
        return _acaRight;
    }
    
    public Centerline getAcomm()
    {
        return _acomm;
    }
    
    public Centerline getMcaLeft()
    {
        return _mcaLeft;
    }
    
    public Centerline getMcaRight()
    {
        return _mcaRight;
    }
    
	public Set<GraphNode> getLeftICAMCABifurcation() {
		return _leftICAMCABifurcation;
	}
	public Set<GraphNode> getRightICAMCABifurcation() {
		return _rightICAMCABifurcation;
	}
	public GraphNode getLeftICAend() {
		return _leftICAend;
	}
	public GraphNode getRightICAend() {
		return _rightICAend;
	}
}

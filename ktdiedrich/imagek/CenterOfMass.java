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
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.util.LinkedList;
import java.util.List;

/** Find the center of mass. Used for identifying and coloring round aneurysms.
 * @author ktdiedrich@gmail.com
 * */
public class CenterOfMass 
{
	public static short BASE_INTENSITY = 50;
	public static int MIN_MOVES = 30;
	public static double WEIGHT_POWER = 3.0;
	public CenterOfMass(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public float x;
	public float y;
	public float z;
	
	public static float centerOfMassDistance(GraphNode n, CenterOfMass cm, VoxelDistance vd )
	{
		double nx = n.col;
		double ny = n.row;
		double nz = n.z;
		nx = (nx-cm.x)*vd.getXRes();
		ny = (ny-cm.y)*vd.getYRes();
		nz = (nz-cm.z)*vd.getZRes();
		
		return (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
	}
	public static float centerOfMassDistance(CenterOfMass nm, CenterOfMass cm, VoxelDistance vd )
	{
		double nx = nm.x;
		double ny = nm.y;
		double nz = nm.z;
		nx = (nx-cm.x)*vd.getXRes();
		ny = (ny-cm.y)*vd.getYRes();
		nz = (nz-cm.z)*vd.getZRes();
		
		return (float)Math.sqrt(nx*nx + ny*ny + nz*nz);
	}
	/** Find the center of mass throughout the image. Sets the centerOfMass and weight values on the 
	 * GraphNode objects passed in. 
	 * @param imageGraphs The image a series of linked lists for every component.
	 * @param recenterTimes The number of additional time to recalculate the 
	 * center of mass. */
	public static void findCenterOfMass(List<Graph> imageGraphs, int recenterTimes, VoxelDistance vd, 
			int minMoves, double weightPower, boolean dfeWtCOM)
	{
		// first center of mass 
		int[] sMoves = new int[minMoves];
		IJ.log("Is DFE weighted COM? "+dfeWtCOM);
		LinkedList<GraphNode> allGraphNodes = new LinkedList<GraphNode>();
		
		for (Graph g: imageGraphs)
		{
			if (g != null)
			{
				LinkedList<GraphNode> nodes = g.getNodes();
				
	            for (GraphNode n: nodes)
	            {
	            	allGraphNodes.add(n);
	            	int dfeWt = 1;
	            	if (dfeWtCOM)
	            	{
	            		dfeWt = n.dfe;
	            	}
	            	int sumAdjX=n.col*dfeWt, sumAdjY=n.row*dfeWt, sumAdjZ=n.z*dfeWt;
	            	int sumAdjDFE=dfeWt;
	            	for (GraphNode a: n.adjacents)
	            	{
	            		int aDfeWt = 1;
	            		if (dfeWtCOM)
	            		{
	            			aDfeWt = a.dfe;
	            		}
	            		sumAdjDFE += aDfeWt;
	            		sumAdjX += a.col*aDfeWt;
	            		sumAdjY += a.row*aDfeWt;
	            		sumAdjZ += a.z*aDfeWt;
	            	}
	            	CenterOfMass cm = new CenterOfMass(sumAdjX/sumAdjDFE, sumAdjY/sumAdjDFE, 
	            			sumAdjZ/sumAdjDFE);
	            	n.centerOfMass = cm;
	            	n.weight = centerOfMassDistance(n, cm, vd);
	            	
	            	if (n.weight == 0)
	            	{
	            		n.pathLen = 0;
	            		sMoves[0]++;
	            	}
	            	else
	            	{
	            		n.pathLen = 1;
	            		sMoves[1]++;
	            	}
	            }
			}
		}
		StringBuffer sb = new StringBuffer("Center of mass minMoves: "+minMoves+" Weight Power: "+weightPower+" 1: ");
		for (int s=0; s < minMoves; s++)
		{
			sb.append(" pathLen "+s+" count: "+sMoves[s]+" ");
		}
		IJ.log(sb.toString());
        // recalculate center of mass of point
    	// iterate and compute angular deviation 
		int re = 2;
		
		int[] pMoves = sMoves;
		boolean dropping = true;
		int ni = 0;
		int anLen = allGraphNodes.size();
		CenterOfMass[] nextCMs = new CenterOfMass[anLen]; 
		while ( dropping && re <= recenterTimes)
        {
			ni = 0;
			sMoves = new int[minMoves];
        	for (GraphNode node: allGraphNodes)
    		{
                // recalculate center of mass of point
        		float dfeWt = 1;
        		if (dfeWtCOM)
        		{
        			// dfeWt = node.mDFE;
        			dfeWt = node.dfe;
        		}
            	float sumAdjX=node.centerOfMass.x*dfeWt, 
            		sumAdjY=node.centerOfMass.y*dfeWt, 
            		sumAdjZ=node.centerOfMass.z*dfeWt;
            	float sumAdjDFE = dfeWt;
            	for (GraphNode a: node.adjacents)
            	{
            		
            		
            		if (a.centerOfMass != null)
            		{
            			float aDfeWt = 1;
            			if (dfeWtCOM)
            			{
            				// aDfeWt = a.mDFE;
            				aDfeWt = a.dfe;
            			}
            			sumAdjDFE += aDfeWt;
            			sumAdjX += a.centerOfMass.x * aDfeWt;
            			sumAdjY += a.centerOfMass.y * aDfeWt;
            			sumAdjZ += a.centerOfMass.z * aDfeWt;
            		}
            	}
            	CenterOfMass nextCm = new CenterOfMass(sumAdjX/sumAdjDFE, sumAdjY/sumAdjDFE, 
            			sumAdjZ/sumAdjDFE);
            	nextCMs[ni] = nextCm;
            	// node.weight = centerOfMassDistance(node, cm, vd);
            	node.weight+=centerOfMassDistance(node.centerOfMass, nextCm, vd);
            	if (node.weight > 0)
            	{
            		node.pathLen++;
            	}
            	if (node.pathLen < minMoves)
            	{
            		sMoves[node.pathLen]++;
            	}
            	ni++;
                
    		}
        	
        	re++;
        	dropping = false;
        	for (int j=0; j < minMoves; j++)
        	{
        		if (sMoves[j] <  pMoves[j])
        			dropping = true;	
        	}
        	pMoves = sMoves;
        	ni=0;
        	for (GraphNode node: allGraphNodes)
        	{
        		node.centerOfMass = nextCMs[ni];
        		ni++;
        	}
        }
		
		float minDistance = Float.MAX_VALUE;
		float minNonZero = Float.MAX_VALUE;
		float maxDistance = 0;
		for (Graph g: imageGraphs)
		{
			if (g != null)
			{
				LinkedList<GraphNode> nodes = g.getNodes();
	            for (GraphNode n: nodes)
	            {
	                if (n.weight < minDistance) minDistance = n.weight;
	                if (n.weight != 0 && n.weight < minNonZero) minNonZero = n.weight;
	            	if (n.weight > maxDistance) maxDistance = n.weight;
	            }
			}
		}
		
		//float minCumDist = Float.MAX_VALUE;
		//float maxCumDist = 0;
		for (Graph g: imageGraphs)
		{
			if (g != null)
			{
				LinkedList<GraphNode> nodes = g.getNodes();
	            for (GraphNode n: nodes)
	            {
	            	// n.dfe = VoxelDistance.convert2short(maxDistance-n.weight); 
	                n.weight = n.weight / minNonZero;
	                n.weight = (float)Math.pow(n.weight, weightPower);
	                //if (n.weight > maxCumDist) maxCumDist = n.weight;
	                //if (n.weight < minCumDist) minCumDist = n.weight;
	            }
			}
		}
		IJ.log("CenterOfMass times: "+re+" minDistance: "+minDistance+
    			" minNonZero: "+minNonZero+" maxDistance: "+maxDistance); 
		// + " minCumDist: "+minCumDist+" maxCumDist: "+maxCumDist);
	}
	
	public static ImagePlus makeCenterOfMassImage(List<Graph> imageGraphs, int width, int height, int zSize,
			String name)
	{
		
		float[][] voxels = new float[zSize][width*height];
		
		for (Graph g: imageGraphs)
		{
			if (g != null)
			{
				List<GraphNode> nodes = g.getNodes();
			
				for (GraphNode n: nodes)
				{
					voxels[n.z][n.row*width + n.col] = n.weight;
				}
			}
		}
		ImagePlus im = ImageProcess.makeImage(voxels, width, height, name);
		return im;
	}
}

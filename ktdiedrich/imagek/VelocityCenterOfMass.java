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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ktdiedrich.math.VectorMath;

/** Center of Mass of velocity vectors. 
 * @author ktdiedrich@gmail.com */
public class VelocityCenterOfMass 
{
	private VelocityCenterOfMass()
	{
		
	}
			
	public static void findCenterOfMass(List<Graph> imageGraphs, int recenterTimes,  
			VoxelDistance vd, int minMoves, double weightPower, ImagePlus xPC, ImagePlus yPC, ImagePlus zPC)
	{
		PhaseContrast pc = new PhaseContrast(xPC, yPC, zPC);
		// first center of mass 
		
		LinkedList<GraphNode> allGraphNodes = new LinkedList<GraphNode>();
		List<CenterOfMass> allFirstCOM = new ArrayList<CenterOfMass>();
		for (Graph g: imageGraphs)
		{
			if (g != null)
			{
				LinkedList<GraphNode> nodes = g.getNodes();
				
	            for (GraphNode n: nodes)
	            {
	            	double[] velocVec = pc.getPCvelocityVector(n.col, n.row, n.z);
	            	double sumXV = n.col * velocVec[0];
	            	double sumYV = n.row * velocVec[1];
	            	double sumZV = n.z * velocVec[2];
	            	double sumX = velocVec[0];
	            	double sumY = velocVec[1];
	            	double sumZ = velocVec[2];
	            	
	            	for (GraphNode an: n.adjacents)
	            	{
	            		double[] anVV = pc.getPCvelocityVector(an.col, an.row, an.z);
	            		sumXV += an.col * anVV[0];
	            		sumYV += an.row * anVV[1];
	            		sumZV += an.z * anVV[2];
	            		sumX += anVV[0];
	            		sumY += anVV[1];
	            		sumZ += anVV[2];
	            	}
	            	float xv = (float)(sumXV/sumX);
	            	float yv = (float)(sumYV/sumY);
	            	float zv = (float)(sumZV/sumZ);
	            	n.centerOfMass = new CenterOfMass(xv, yv, zv); // this will change 
	            	CenterOfMass firstCOM = new CenterOfMass(xv, yv, zv); // save first COM
	            	// keep these two lists in same order 
	            	allGraphNodes.add(n);
	            	allFirstCOM.add(firstCOM);
	            }
			}
		}
		
		
        // recalculate center of mass of point
    	// iterate and compute angular deviation 
		int re = 2;
		
		int ni = 0;
		int anLen = allGraphNodes.size();
		CenterOfMass[] nextCMs = new CenterOfMass[anLen]; 
		IJ.log("Recenter "+recenterTimes+" times. ");
		while ( re <= recenterTimes)
        {
			ni = 0;
        	for (GraphNode node: allGraphNodes)
    		{
                // recalculate center of mass of point
        		
        		
            	float sumAdjX=node.centerOfMass.x, 
            		sumAdjY=node.centerOfMass.y, 
            		sumAdjZ=node.centerOfMass.z;
            	for (GraphNode a: node.adjacents)
            	{
            		if (a.centerOfMass != null)
            		{	
            			sumAdjX += a.centerOfMass.x;
            			sumAdjY += a.centerOfMass.y;
            			sumAdjZ += a.centerOfMass.z;
            		}
            	}
            	CenterOfMass nextCm = new CenterOfMass(sumAdjX, sumAdjY, sumAdjZ);
            	nextCMs[ni] = nextCm;
            	ni++;
    		}
        	
        	re++;
        	
        	ni=0;
        	for (GraphNode node: allGraphNodes)
        	{
        		node.centerOfMass = nextCMs[ni];
        		ni++;
        	}
        }
		int i = 0;
		for (GraphNode n: allGraphNodes)
		{
			CenterOfMass com = allFirstCOM.get(i);
			n.weight = (float )( Math.pow(n.col-com.x, 2) + Math.pow(n.row-com.y, 2) + Math.pow(n.z-com.z, 2)); 
			i++;
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
}

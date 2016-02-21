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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/** Find false positive centerline ends. These ends are close together and can be joined with a straight line.
 * @author ktdiedrich@gmail.com
 *  */
public class BadCenterlineEnds 
{
	public static final double BAD_END_DISTANCE = 40;
	public static final int MOVE = 20;
	private Map<GraphNode, GraphNode> _movedEndPairs;
	private Map<GraphNode, List<GraphNode> > _endRemoved;
	private Set<GraphNode> _goodEnds;
	/** Updates the treeEnds to the new moved locations.   
	 * */
	BadCenterlineEnds(Set<GraphNode> treeEnds)
	{
		Set<GraphNode> badEnds = new HashSet<GraphNode>();
		_goodEnds = new HashSet<GraphNode>();
		int N = treeEnds.size();
		GraphNode[] ends = treeEnds.toArray(new GraphNode[N]);
		
		Map<GraphNode, Double> endDistance = new HashMap<GraphNode, Double>();
		Map<GraphNode, GraphNode> endPairsAB = new HashMap<GraphNode, GraphNode>();
		int sb = 1;
		for (int a=0; a < N; a++)
		{
			GraphNode teA = ends[a];
			double teAd = Double.MAX_VALUE;
			for (int b=sb; b < N; b++)
			{
				GraphNode teB = ends[b];
				if (teA != teB)
				{
					double d = GraphNode.distance(teA, teB);
					if (d < BAD_END_DISTANCE)
					{
						badEnds.add(teA); badEnds.add(teB);
						double teBd = Double.MAX_VALUE;
						if (endDistance.containsKey(teA))
						{
							teAd = endDistance.get(teA);
							if (d < teAd)
							{
								endDistance.put(teA, d);
								endPairsAB.remove(teA);
								List<GraphNode> remove = new LinkedList<GraphNode>();
								for (GraphNode key: endPairsAB.keySet())
								{
									GraphNode value = endPairsAB.get(key);
									if (value == teA)
									{
										remove.add(key);
									}
								}
								for (GraphNode r: remove)
								{
									endPairsAB.remove(r);
									//IJ.log("Removed A pair: "+r.coordinateString());
								}
							}
						}
						else
						{
							endDistance.put(teA, d);
							//IJ.log("Put A: "+teA.coordinateString()+" distance: "+d);
						}
						// IJ.log("Checking for B distance: "+teB.coordinateString());
						if (endDistance.containsKey(teB))
						{
							teBd = endDistance.get(teB);
							//IJ.log(teB.coordinateString()+" B had distance "+teBd);
							if (d < teBd)
							{
								endDistance.put(teB, d);
								endPairsAB.remove(teB);
								//IJ.log("Removed B: "+teB.coordinateString());
								List<GraphNode> remove = new LinkedList<GraphNode>();
								for (GraphNode key: endPairsAB.keySet())
								{
									GraphNode value = endPairsAB.get(key);
									if (value == teB)
									{
										remove.add(key);
									}
								}
								for (GraphNode r: remove)
								{
									endPairsAB.remove(r);
									//IJ.log("Removed B pair: "+r.coordinateString());
								}
							}
						}
						else
						{
							endDistance.put(teB, d);
							//IJ.log("Put B: "+teB.coordinateString()+ " distance: "+d);
						}
						
						if (d <= teAd && d <= teBd)
						{
							endPairsAB.put(teA, teB);
							//IJ.log("Added bad end pair: "+teA.coordinateString()+" "+
							//		teB.coordinateString()+" distance: "+d);
						}
						
					}
				}
			}
			sb++;
		}
		// save the good ends 
		for (GraphNode tn: treeEnds)
		{
			if (!badEnds.contains(tn))
			{
				_goodEnds.add(tn);
				if (tn.isCenterline == false)
				{
					tn.isCenterline = true;
					IJ.log("Reset as centerline: "+tn.coordinateString());
				}
			}
		}
		_movedEndPairs = moveBadEnds(endPairsAB, treeEnds);
	}
	
	/** Walk the bad ends within the tree ends of the centerlines back to the middle of the segmented arteries. 
	 * @return Pairs of the the new moved bad end points. */
	public Map<GraphNode, GraphNode> moveBadEnds(Map<GraphNode, GraphNode> badEndPairsAB, Set<GraphNode> treeEnds)
	{
		Map<GraphNode, GraphNode> movedPairs = new HashMap<GraphNode, GraphNode>();
		_endRemoved = new HashMap<GraphNode, List<GraphNode> >();
		for (GraphNode badEndA: badEndPairsAB.keySet())
		{
			GraphNode badEndB = badEndPairsAB.get(badEndA);
			GraphNode movedA = BadCenterlineEnds.moveBadEnd(badEndA, treeEnds, _endRemoved);
			GraphNode movedB = BadCenterlineEnds.moveBadEnd(badEndB, treeEnds, _endRemoved);
			movedPairs.put(movedA, movedB);
			IJ.log("Bad End Pair: "+badEndA.coordinateString()+" "+badEndB.coordinateString()+
					" Moved End Pair: "+movedA.coordinateString()+" "+movedB.coordinateString());
		}
		
		return movedPairs;
	}
	public static GraphNode moveBadEnd(GraphNode badEnd, Set<GraphNode> treeEnds, 
			Map<GraphNode, List<GraphNode> > endRemoved )
	{
		List<GraphNode> line = badEnd.centerline.getCenterlineNodes();
		ListIterator<GraphNode> lineItr = line.listIterator();
		// remove nodes past the new end from the centerline graph  
		GraphNode movedEndNodeIn = badEnd;
		List<GraphNode> removed = new LinkedList<GraphNode>();
		//  save backed up centerline for restore 
		for (int i=0; i < MOVE; i++)
		{
			if (lineItr.hasNext())
			{
				removed.add(movedEndNodeIn);
				movedEndNodeIn = lineItr.next();
				movedEndNodeIn.isCenterline = false;
				if (i < MOVE-1)
				{
					lineItr.remove();
					
				}
			}
		}
		treeEnds.remove(badEnd);
		treeEnds.add(movedEndNodeIn);
		endRemoved.put(movedEndNodeIn, removed);
		return movedEndNodeIn;
	}
	public Map<GraphNode, GraphNode> getMovedEndPairs() {
		return _movedEndPairs;
	}

	/** New moved centerline end and a list of centerline nodes removed to get to the new end. This is used for restoring the centerline. */
	public Map<GraphNode, List<GraphNode>> getEndRemoved() {
		return _endRemoved;
	}

	public Set<GraphNode> getGoodEnds() {
		return _goodEnds;
	}
	/** Finds the good ends without moving any ends. */
	public static Set<GraphNode> findGoodEnds(Set<GraphNode> treeEnds)
	{
		Set<GraphNode> badEnds = new HashSet<GraphNode>();
		Set<GraphNode> goodEnds = new HashSet<GraphNode>();
		int N = treeEnds.size();
		GraphNode[] ends = treeEnds.toArray(new GraphNode[N]);
		int sb = 1;
		for (int a=0; a < N; a++)
		{
			GraphNode teA = ends[a];
			for (int b=sb; b < N; b++)
			{
				GraphNode teB = ends[b];
				if (teA != teB)
				{
					double d = GraphNode.distance(teA, teB);
					if (d < BAD_END_DISTANCE)
					{
						badEnds.add(teA); badEnds.add(teB);
					}
				}
			}
		}
		// save the good ends 
		for (GraphNode tn: treeEnds)
		{
			if (!badEnds.contains(tn))
			{
				goodEnds.add(tn);
			}
		}
		return goodEnds;
	}
}


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

import java.util.*;


/** Connected component cluster of image voxels tracked by position. 
 * @author Karl Diedrich <ktdiedrich@gmail.com> 
 * */
public class Cluster implements Comparable<Cluster> 
{
    private List<Position> _positions;
    public Cluster(short label)
    {
        _label = label;
        _size = 0;
        _positions = new LinkedList<Position>();
    }
    private short _label;
    private int _size;
    public short getLabel()
    {
        return _label;
    }
    public int getSize()
    {
        return _size;
    }
    /** Add to member count */
    public void addOneSize()
    {
        _size++;
    }
    /** Add the position member and add to the count. Don't call addOneSize to increment. */
    public void addPosition(Position pos)
    {
        _positions.add(pos);
        _size++;
    }
    public List<Position> getPositions()
    {
        return _positions;
    }
    public String toString()
    {
    	return "Cluster label: "+_label+" size: "+_size;
    }
	/** Sort smallest to largest */
	public int compareTo(Cluster arg0) 
	{
		if (this._size > arg0._size)
            return 1;
        if (this._size == arg0._size)
            return 0;
        return -1;
	}
	/** Test cluster sorting */
	public static void main(String[] args)
	{
		List<Cluster> clusters = new LinkedList<Cluster>();
		Cluster a = new Cluster((short)1); a._size = 4;
		Cluster b = new Cluster((short)2); b._size = 3;
		Cluster c = new Cluster((short)3); c._size = 5;
		Cluster d = new Cluster((short)4); d._size = 2;
		Cluster e = new Cluster((short)5); e._size = 3;
		clusters.add(a); clusters.add(b); clusters.add(c); clusters.add(d); clusters.add(e); 
		for (Cluster clst: clusters)
		{
			System.out.println(clst);
		}
		Collections.sort(clusters);
		System.out.println("Sort");
		for (Cluster clst: clusters)
		{
			System.out.println(clst);
		}
	}
}

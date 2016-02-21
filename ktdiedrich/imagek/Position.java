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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/* Column, row and z position in a 3-D image. 
 * @author Karl Diedrich <ktdiedrich@gmail.com> 
 */
public class Position 
{
    private int _row;
    private int _column;
    private int _z;
    public boolean visited;
    public Position()
    {
        
    }
    public Position(GraphNode node)
    {
    	_column = node.col;
    	_row = node.row;
        
        _z = node.z;
    }
    public Position(int col, int row)
    {
    	_column= col;
    	_row = row;
    }
    public Position(int col, int row, int z)
    {
    	_column= col;
        _row = row;
        _z = z;
    }
    public int getRow()
    {
        return _row;
    }
    public void setRow(int row)
    {
        _row = row;
    }
    public int getColumn()
    {
        return _column;
    }
    public void setColumn(int column)
    {
        _column = column;
    }
    public int getZ()
    {
        return _z;
    }
    public void setZ(int z)
    {
        _z = z;
    }
    public String toString()
    {
        return "("+_column+", "+_row+", "+_z+")";
    }
    /** @return a unique string key for the position */
    public String key()
    {
        return _column+"_"+_row+"_"+_z;
    }
    public boolean equals(Object obj)
    {
        if (obj instanceof Position)
        {
            Position p = (Position)obj;
            if (this._column == p._column && this._row == p._row && this._z == p._z)
                return true;
        }
        return false;
    }
    public Queue<Position> getNeighbors(int width, int height, int zSize)
    {
        Queue<Position> q = new LinkedList<Position>();
        if (_row+1 < height)
        	q.add(new Position(_column, _row+1, _z));
        if (_row-1 >= 0)
        	q.add(new Position(_column, _row-1, _z));
        if (_column+1 < width)
        	q.add(new Position(_column+1, _row, _z));
        if (_column-1 >=0)
        	q.add(new Position(_column-1, _row, _z));
        if (_z+1 < zSize)
        	q.add(new Position(_column, _row, _z+1));
        if (_z-1 >=0)
        	q.add(new Position(_column, _row, _z-1));
        
        if (_column+1 < width && _row+1 < height)
        	q.add(new Position(_column+1, _row+1, _z));
        
        if (_column+1 < width && _row-1 >= 0)
        	q.add(new Position(_column+1, _row-1, _z));
        
        if (_column-1 >= 0 && _row+1 < height)
        	q.add(new Position(_column-1, _row+1, _z));
        
        if (_column-1 >= 0 && _row-1 >= 0)
        	q.add(new Position(_column-1, _row-1, _z));
        
        if (_row+1 < height && _z+1 < zSize)
        	q.add(new Position(_column, _row+1, _z+1));
        
        if (_row-1 >= 0 && _z < zSize)
        	q.add(new Position(_column, _row-1, _z+1));
        
        if (_column+1 < width && _z+1 < zSize)
        	q.add(new Position(_column+1, _row, _z+1));
        
        if (_column+1 < width && _row+1 < height && _z+1 < zSize)
        	q.add(new Position(_column+1, _row+1, _z+1));
        
        if (_column+1 < width && _row-1 >= 0 && _z+1 < zSize)
        	q.add(new Position(_column+1, _row-1, _z+1));
        
        if (_column-1 >= 0 && _z+1 < zSize)
        	q.add(new Position(_column-1, _row, _z+1));
        
        if (_column-1 >= 0 && _row+1 < height && _z+1 < zSize)
        	q.add(new Position(_column-1, _row+1, _z+1));
        
        if (_column-1 >= 0 && _row-1 >= 0 && _z+1 < zSize)
        	q.add(new Position(_column-1, _row-1, _z+1));
        
        if (_column-1 >= 0 && _z-1 >= 0)
        	q.add(new Position(_column-1, _row, _z-1));
        
        if (_row+1 < height && _z-1 >= 0)
        	q.add(new Position(_column, _row+1, _z-1));
        
        if (_row-1 >= 0 && _z-1 >= 0)
        	q.add(new Position(_column, _row-1, _z-1));
        
        if (_column+1 < width && _z-1 >= 0)
        	q.add(new Position(_column+1, _row, _z-1));
        
        if (_column+1 < width && _row+1 < height && _z-1 >= 0)
        	q.add(new Position(_column+1, _row+1, _z-1));
        
        if (_column+1 < width && _row-1 >= 0 && _z-1 >= 0)
        	q.add(new Position(_column+1, _row-1, _z-1));
        
        if (_column-1 >= 0 && _row+1 < height && _z-1 >= 0)
        	q.add(new Position(_column-1, _row+1, _z-1));
        
        if (_column-1 >= 0 && _row-1 >= 0 && _z-1 >= 0)
        	q.add(new Position(_column-1, _row-1, _z-1));
        
        return q;
    }
    
    public Queue<Position> getNeighbors()
    {
        Queue<Position> q = new LinkedList<Position>();
        q.add(new Position(_column, _row+1, _z));
        q.add(new Position(_column, _row-1, _z));
        q.add(new Position(_column+1, _row, _z));
        q.add(new Position(_column-1, _row, _z));
        q.add(new Position(_column, _row, _z+1));
        q.add(new Position(_column, _row, _z-1));
        
        q.add(new Position(_column+1, _row+1, _z));
        q.add(new Position(_column+1, _row-1, _z));
        q.add(new Position(_column-1, _row+1, _z));
        q.add(new Position(_column-1, _row-1, _z));
        q.add(new Position(_column, _row+1, _z+1));
        q.add(new Position(_column, _row-1, _z+1));
        q.add(new Position(_column+1, _row, _z+1));
        q.add(new Position(_column+1, _row+1, _z+1));
        q.add(new Position(_column+1, _row-1, _z+1));
        q.add(new Position(_column-1, _row, _z+1));
        q.add(new Position(_column-1, _row+1, _z+1));
        q.add(new Position(_column-1, _row-1, _z+1));
        q.add(new Position(_column-1, _row, _z-1));
        q.add(new Position(_column, _row+1, _z-1));
        q.add(new Position(_column, _row-1, _z-1));
        q.add(new Position(_column+1, _row, _z-1));
        
        q.add(new Position(_column+1, _row+1, _z-1));
        q.add(new Position(_column+1, _row-1, _z-1));
        q.add(new Position(_column-1, _row+1, _z-1));
        q.add(new Position(_column-1, _row-1, _z-1));
        
        return q;
    }
    /** Add 26 neighbors in 3D to a list of positions */
    public static void addNeighbors(Queue<Position> checkPos, int col, int row, int z)
    {
    	// TODO make Position(x, y, z)
        checkPos.add(new Position(col, row+1, z));
        checkPos.add(new Position(col, row-1, z));
        checkPos.add(new Position(col+1, row, z));
        checkPos.add(new Position(col+1, row+1, z));
        checkPos.add(new Position(col+1, row-1, z));
        checkPos.add(new Position(col-1, row, z));
        checkPos.add(new Position(col-1, row+1, z));
        checkPos.add(new Position(col-1, row-1, z));
        checkPos.add(new Position(col, row, z+1));
        checkPos.add(new Position(col, row+1, z+1));
        checkPos.add(new Position(col, row-1, z+1));
        checkPos.add(new Position(col+1, row, z+1));
        checkPos.add(new Position(col+1, row+1, z+1));
        checkPos.add(new Position(col+1, row-1, z+1));
        checkPos.add(new Position(col-1, row, z+1));
        checkPos.add(new Position(col-1, row+1, z+1));
        checkPos.add(new Position(col-1, row-1, z+1));
        checkPos.add(new Position(col, row, z-1));
        checkPos.add(new Position(col, row+1, z-1));
        checkPos.add(new Position(col, row-1, z-1));
        checkPos.add(new Position(col+1, row, z-1));
        checkPos.add(new Position(col+1, row+1, z-1));
        checkPos.add(new Position(col+1, row-1, z-1));
        checkPos.add(new Position(col-1, row, z-1));
        checkPos.add(new Position(col-1, row+1, z-1));
        checkPos.add(new Position(col-1, row-1, z-1));
    }
    public static void main(String[] args)
    {
    	// TODO make Position(x, y, z)
        Position a1 = new Position(1,1,1);
        Position a2 = new Position(1,1,1);
        Position b = new Position(2,1,1);
        Position c = new Position(1,2,1);
        Position d = new Position(1,1,2);
        Map<String, Position> map = new HashMap<String, Position>();
        map.put(a1.key(), a1);  map.put(b.key(), b); map.put(c.key(), c);
        map.put(d.key(), d); 
        System.out.println("Equals: "+a1.equals(a2));
        System.out.println("Contains (1,1,1): "+map.containsKey(a1.key()));
        System.out.println("Contains (1,1,1): "+map.containsKey(a2.key()));
        
        map.put(a2.key(), a2);
        System.out.println("Size="+map.keySet().size());
        for (String k: map.keySet())
        {
            System.out.println(k+" "+map.get(k));
        }
        Map<Integer, String> map2 = new HashMap<Integer, String>();
        map2.put(1, "One"); map2.put(2, "Two"); map2.put(3, "Three");
        map2.put(1, "Il"); map2.put(2, "Ii");
        for (Integer i: map2.keySet())
        {
            System.out.println(i+" "+map2.get(i));
        }
        System.out.println("Contains 2: "+map2.containsKey(2));
        
        Map<String, Integer> map3 = new HashMap<String, Integer>();
        map3.put("One", 1); map3.put("Two", 2); map3.put("Three", 3);
        map3.put("One", 1); map3.put("Three", 3);
        for (String k: map3.keySet())
        {
            System.out.println(k+" "+map3.get(k));
        }
        System.out.println("Contains Two: "+map3.containsKey("Two"));
    }
}

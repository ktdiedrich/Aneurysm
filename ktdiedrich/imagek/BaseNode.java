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


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.lang.Comparable;

/** Basic GraphNode. Comparable on the value type T.  
* @author Karl Diedrich <ktdiedrich@gmail.com>
* */
public class BaseNode<T extends Comparable<T> > implements Comparable<BaseNode<T> >
{
	private Position _position;
	private T _value;
	private Set<BaseNode<T> > _adjacents;
	private boolean _graphed;
  public BaseNode(T value, Position position)
  {
      _value = value;
      _position = position;
      _adjacents = new HashSet<BaseNode<T> >();
  }
  public int compareTo(BaseNode<T> a)
  {   
      return _value.compareTo(a.getValue());
  }
  
  public void addAdjacent(BaseNode<T> node)
  {
      _adjacents.add(node);
  }
 
  public Position getPosition()
  {
      return _position;
  }
  public T getValue()
  {
      return _value;
  }
  public void setValue(T value)
  {
      _value = value;
  }
  public Set<BaseNode<T> > getAdjacents()
  {
      return _adjacents;
  }
  
  public String toString()
  {
      StringBuffer sb = new StringBuffer();
      sb.append("Pos=");
      sb.append(_position.toString());
      sb.append(" Val=");
      sb.append(_value.toString());
      sb.append(" Adj=");
      sb.append(_adjacents.size());
      
      return sb.toString();
  } 
  
  public boolean isGraphed()
  {
      return _graphed;
  }
  public void setGraphed(boolean graphed)
  {
      _graphed = graphed;
  }
  public static void main(String[] args)
  {
	  // TODO make Position(x, y, z)
      BaseNode<Short> one = new BaseNode<Short>(new Short((short)1), new Position(1,1,1));
      BaseNode<Short> two = new BaseNode<Short>(new Short((short)2), new Position(2,1,1));
      BaseNode<Short> three = new BaseNode<Short>(new Short((short)3), new Position(1,2,1));
      BaseNode<Short> four = new BaseNode<Short>(new Short((short)4), new Position(1,1,2));
      BaseNode<Short> five = new BaseNode<Short>(new Short((short)5), new Position(2,2,1));
      BaseNode<Short> oneB = new BaseNode<Short>(new Short((short)1), new Position(1,2,2));
      List<BaseNode<Short> > shortNodes= new LinkedList<BaseNode<Short> >();
      shortNodes.add(one); shortNodes.add(two); shortNodes.add(three); shortNodes.add(four); 
      shortNodes.add(five); shortNodes.add(oneB);
      
      assert one.compareTo(two) < 0;
      assert one.compareTo(oneB) == 0;
      assert four.compareTo(three) > 0;
      one.addAdjacent(two);
      one.addAdjacent(oneB);
      two.addAdjacent(three);
      three.addAdjacent(four);
      four.addAdjacent(five);
      for (BaseNode<Short> node: shortNodes)
      {
          System.out.println(node);
      }
  }
}
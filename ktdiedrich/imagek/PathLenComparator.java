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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/** Sort GraphNode on path length greatest to smallest. 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class PathLenComparator implements Comparator<GraphNode> 
{
    public int compare (GraphNode a, GraphNode b)
    {	
        if (a.pathLen < b.pathLen)
            return 1;
        if (a.pathLen == b.pathLen)
            return 0;
        // TODO ordering nodes by path cost 
        /*
        if (a.pathCost < b.pathCost)
            return 1;
        if (a.pathCost == b.pathCost)
            return 0;
        */
        return -1;
    }
    public static void main(String[] args)
    {
    	// TODO make Position(x, y, z)
        GraphNode a = new GraphNode(new Position(1,1,1));
        a.pathCost = 4.0F;
        a.pathLen = 3;
        GraphNode b = new GraphNode(new Position(1,2,1));
        b.pathCost = 2.0F;
        b.pathLen = 4;
        GraphNode c = new GraphNode(new Position(1,1,2));
        c.pathCost = 0.0F;
        c.pathLen = 2;
        GraphNode d = new GraphNode(new Position(2,1,1));
        d.pathCost = 3.0F;
        d.pathLen = 0;
        GraphNode e = new GraphNode(new Position(2,2,1));
        e.pathCost = 1.0F;
        e.pathLen = 1;
        List<GraphNode> nodes = new LinkedList<GraphNode>();
        nodes.add(a); nodes.add(b); nodes.add(c); nodes.add(d); nodes.add(e);
        print(nodes);
        Collections.sort(nodes);
        print(nodes);
        Collections.sort(nodes, new PathLenComparator());
        print(nodes);
    }
    public static void print(List<GraphNode> nodes)
    {
        for (GraphNode node: nodes)
            System.out.println(GraphNode.toString(node));
        System.out.println();
    }
}

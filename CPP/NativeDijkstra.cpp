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

#include "ktdiedrich_math_NativeDijkstra.h"
#include <algorithm>
#include <vector>
#include <iostream>
#include <queue>

using namespace std;

class GraphNode
{
	friend bool operator<(GraphNode const & a, GraphNode const & b)
	{
		return a.pathCost < b.pathCost;
	}
	friend bool operator>(GraphNode const & a, GraphNode const & b)
	{
		return a.pathCost > b.pathCost;
	}
	friend std::ostream& operator<<(std::ostream& os, GraphNode const & n) {
	  os << "ID: " << n.id << " pathCost: " << n.pathCost << " weight: " << n.weight << " pathLen: "
		  << n.pathLen << " adjacent: ";
	  for (int i=0; i < n.adjacents.size(); i++)
	  {
		  os << n.adjacents[i] << ", ";
	  }
	  return os;
	}


public:
	GraphNode(int i, float pc, float w) : id(i), pathCost(pc), weight(w)
	{
		pathLen = 0;
		predecessor = NULL;
	}
	int id;
	float pathCost;
	float weight;
	GraphNode* predecessor;
	int pathLen;
	std::vector<int> adjacents;
};

bool relax(GraphNode u, GraphNode v)
{
    if (v.pathCost > (u.pathCost+v.weight))
    {
    	v.pathCost = (u.pathCost+v.weight);
        v.predecessor = (&u);
        v.pathLen = (u.pathLen+1);
        return true;
    }
    return false;
}

void dijkstra(priority_queue<GraphNode, vector<GraphNode>, greater<vector<GraphNode>::value_type > >  & queue)
{
	int length = queue.size();
	cout << "Length: " << length << "\n";
	priority_queue<float> fq;
	fq.push(2.3f); fq.push(1.2f); fq.push(4.6f); fq.push(5.4f); fq.push(0.7);
	while (!fq.empty())
	{
		float n = fq.top();
		fq.pop();
		cout << n << "\n";
	}
	while (!queue.empty())
	{
		GraphNode n = queue.top();
		queue.pop();
		cout << n << "\n";
	}
	/*
	Collections.sort(queue);
	std::vector<GraphNode> g;
	source.pathCost = (0.0F);
	g.setSourceNode(source);
	LinkedList<GraphNode> S = new LinkedList<GraphNode>();
	while (queue.size() > 0)
	{
		GraphNode u = queue.poll();
	    S.add(u);
	    for (GraphNode v: u.adjacents)
	    {
	        if (relax(u, v))
	        {
	         	// TODO rate limiting step
	            Collections.sort(queue);
	        }
	    }
	 }
	 g.setNodes(S);
	 return g;
	 */

}

JNIEXPORT void JNICALL Java_ktdiedrich_math_NativeDijkstra_dijkstra
  (JNIEnv * env, jclass thisClass, jobjectArray graphNodes, jint length)
{
	priority_queue<GraphNode, vector<GraphNode>, greater<vector<GraphNode>::value_type > > nodeQueue;
	jclass graphNodeClass = env->FindClass("ktdiedrich/imagek/GraphNode");
	if (env->ExceptionOccurred())
	{
		env->ExceptionDescribe();
	}
	// cout << "GraphNode class: " << graphNodeClass << "\n";
	for (int i=0; i < length; i++)
	{
		jobject graphNode = env->GetObjectArrayElement(graphNodes, i);
		jfieldID fid = env->GetFieldID(graphNodeClass, "pathCost", "F");
		float pathCost = env->GetFloatField(graphNode, fid);

		fid = env->GetFieldID(graphNodeClass, "weight", "F");
		float weight = env->GetFloatField(graphNode, fid);

		fid = env->GetFieldID(graphNodeClass, "adjacents", "Ljava/util/Set;");
		jobject adjacents = env->GetObjectField(graphNodeClass, fid);
		cout << "adjacents: " << adjacents << "\n";
		GraphNode node(i, pathCost, weight);
		nodeQueue.push(node);
	}
	dijkstra(nodeQueue);
}

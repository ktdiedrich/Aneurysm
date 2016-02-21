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

import java.util.Comparator;

/** Sorts Clusters from largest to smallest 
 * @author ktdiedrich@gmail.com 
 * */
public class ClusterLargerComp implements Comparator<Cluster>
{
	 public int compare (Cluster a, Cluster b)
	    {
		 	int az = a.getSize();
		 	int bz = b.getSize();
	        if (az < bz)
	            return 1;
	        if (az == bz)
	            return 0;
	        return -1;
	    }
}

/* native comparator sorts smallest to largest 

if (this._size > arg0._size)
            return 1;
        if (this._size == arg0._size)
            return 0;
        return -1;
        
*/
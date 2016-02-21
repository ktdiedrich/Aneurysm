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

package ktdiedrich.math;


/** Vector math utility class.
 * @author ktdiedrich@gmail.com */
public class VectorMath 
{
	private VectorMath()
	{
		
	}
	public static final double[] crossProduct3(double[] a, double[] b)
    {
    	double[] cross = new double[3];
    	cross[0] = a[1]*b[2] - a[2]*b[1];
    	cross[1] = a[2]*b[0] - a[0]*b[2];
    	cross[2] = a[0]*b[1] - a[1]*b[0];
    	return cross;
    }
    public static final double magnitude(double[] v)
    {
    	double s2 = 0;
    	for (int i=0; i < v.length; i++)
    	{
    		s2 += v[i]*v[i];
    	}
    	return Math.sqrt(s2);
    }
    /** Dot product of vectors a and a. Vectors a and b have to be the same length. */
    public static final double dotProduct3(double[] a, double[] b)
    {
    	return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    
    }
}

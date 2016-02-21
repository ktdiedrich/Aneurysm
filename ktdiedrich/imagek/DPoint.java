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

/* Double precision point 
 * @author ktdiedrich@gmail.com 
 * */
public class DPoint 
{
	public double x, y, z, r;
	public DPoint(double x, double y, double z, double r)
	{
		this.x=x;
		this.y=y;
		this.z=z;
		this.r=r;
	}
	public DPoint(double x, double y, double z)
	{
		this(x, y, z, 0);
	}
	public String toString()
	{
		return x+", "+y+", "+z+", "+r;
	}
}

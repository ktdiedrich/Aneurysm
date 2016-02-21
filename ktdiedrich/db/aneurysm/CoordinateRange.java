/*=========================================================================
 *
 *  Copyright (c)   Karl T. Diedrich 
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

package ktdiedrich.db.aneurysm;

/** Information on artery centerline coordinate range. 
 * @author ktdiedrich@gmail.com 
 * */
public class CoordinateRange 
{
	private int _minX, _maxX, _minY, _maxY, _minZ, _maxZ;
	public CoordinateRange(int minX, int maxX, int minY, int maxY, int minZ, int maxZ)
	{
		_minX = minX; _maxX = maxX; _minY = minY; _maxY = maxY; _minZ = minZ; _maxZ = maxZ; 
	}
	public int cubicSize()
	{
		int xr = _maxX-_minX;
		int yr = _maxY-_minY;
		int zr = _maxZ-_minZ;
		return xr*yr*zr;
	}
	public int getMinX() {
		return _minX;
	}
	public int getMaxX() {
		return _maxX;
	}
	public int getMinY() {
		return _minY;
	}
	public int getMaxY() {
		return _maxY;
	}
	public int getMinZ() {
		return _minZ;
	}
	public int getMaxZ() {
		return _maxZ;
	}
	public String toString()
	{
		return "("+_minX+"-"+_maxX+", "+_minY+"-"+_maxY+", "+_minZ+"-"+_maxZ+")";
	}
}

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

/** Distance Factor Metric score
 * @author ktdiedrich@gmail.com 
 * */
public class DFM 
{
	private float _L;
	private float _d;
	private float _dfm;
	private float _dfe;
	private int _x;
	private int _y; 
	private int _z;
	public DFM()
	{
		
	}
	public DFM(float L, float d, float dfm, float dfe)
	{
		_L = L;
		_d = d;
		_dfm = dfm;
		_dfe = dfe;
	}
	public DFM(float L, float d, float dfm, float dfe, int x, int y, int z)
	{
		_L = L;
		_d = d;
		_dfm = dfm;
		_dfe = dfe;
		_x = x;
		_y = y;
		_z = z;
	}
	
	public float getL() 
	{
		return _L;
	}
	public void setL(float l) 
	{
		_L = l;
	}
	public float getD() 
	{
		return _d;
	}
	public void setD(float d) 
	{
		this._d = d;
	}
	public float getDfm() 
	{
		return _dfm;
	}
	public void setDfm(float dfm) 
	{
		this._dfm = dfm;
	}
	public float getDfe() 
	{
		return _dfe;
	}
	public void setDfe(float dfe) 
	{
		_dfe = dfe;
	}
	public String toString()
	{
		StringBuffer sb = new StringBuffer(_L+"");
		sb.append(" "); sb.append(_d); sb.append(" "); sb.append(_dfm); sb.append(" ");	sb.append(_dfe);
		sb.append(" "); sb.append(_x); sb.append(" "); sb.append(_y); sb.append(" "); sb.append(_z);
		
		return sb.toString();
	}
	public int getX() {
		return _x;
	}
	public void setX(int x) {
		_x = x;
	}
	public int getY() {
		return _y;
	}
	public void setY(int y) {
		_y = y;
	}
	public int getZ() {
		return _z;
	}
	public void setZ(int z) {
		_z = z;
	}
}

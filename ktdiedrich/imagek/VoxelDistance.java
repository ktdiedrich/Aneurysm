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

import ij.IJ;

/** The distances across a voxel calculated using the resolution.
 * Distances are round and multiplied by a factor to make them shorts to save memory 
 * @author ktdiedrich@gmail.com */
public class VoxelDistance 
{
	// TODO may change to increase accuracy 
	public static final float DISTANCE_PRECISION = 100.0f;
	private float _xRes, _yRes, _zRes;
	private short _col, _row, _z;
    private short _colRow, _colZ, _rowZ;
    private short _colRowZ;
    
	public VoxelDistance(float xRes, float yRes, float zRes)
	{
		_xRes = xRes;
		_yRes = yRes;
		_zRes = zRes;
		_col = (short)Math.round(_yRes*DISTANCE_PRECISION);
    	_row = (short)Math.round(_xRes*DISTANCE_PRECISION);
    	_z = (short)(_zRes*DISTANCE_PRECISION);
    	
    	_colRow = (short)Math.round( Math.sqrt(_xRes*_xRes + _yRes*_yRes) * DISTANCE_PRECISION );
    	_colZ = (short)Math.round( Math.sqrt(_xRes*_xRes + _zRes*_zRes) * DISTANCE_PRECISION );
    	_rowZ = (short)Math.round( Math.sqrt(_yRes*_yRes + _zRes*_zRes) * DISTANCE_PRECISION );
    	
    	_colRowZ = (short)Math.round( Math.sqrt( _xRes*_xRes + _yRes*_yRes + _zRes*_zRes ) * DISTANCE_PRECISION );
    	
    	IJ.log("col="+_col+" row="+_row+" z="+_z+" colRow="+_colRow+" colZ="+_colZ+" rowZ="+_rowZ+
    			" colRowZ="+_colRowZ);
	}
	public static float maxRes(float x, float y, float z)
	{
		float mRes = z;
		if (x > mRes) mRes = x;
		if (y > mRes) mRes = y;
		return mRes;
	}
	/** Convert to short precision number. Multiplies the floating point value and rounds */
	public static short convert2short(float value)
	{
		return (short)Math.round(value*VoxelDistance.DISTANCE_PRECISION);
	}
	public static float convert2float(short value)
	{
		return (float)(value)/DISTANCE_PRECISION;
	}
	/** The Euclidean distance between two graph nodes is their edge cost using voxel resolutions. */
	public double distance(final GraphNode nodeA, final GraphNode nodeB)
	{
		
		double aCol = ((double)nodeA.col) * _xRes;
		double bCol = ((double)nodeB.col) * _xRes;
		
		double aRow = ((double)nodeA.row) * _yRes;
		double bRow = ((double)nodeB.row) * _yRes;
		
		double aZ = ((double)nodeA.z) * _zRes;
		double bZ = ((double)nodeB.z) * _zRes;
		
		double dCol = aCol-bCol;
		double dRow = aRow-bRow;
		double dZ = aZ-bZ;
		double d = Math.sqrt(dCol*dCol + dRow*dRow + dZ*dZ);
		return d;
	}
	public double distance(final GraphNode nodeA, int x, int y, int z)
	{
		
		double aCol = ((double)nodeA.col) * _xRes;
		double bCol = ((double)x) * _xRes;
		
		double aRow = ((double)nodeA.row) * _yRes;
		double bRow = ((double)y) * _yRes;
		
		double aZ = ((double)nodeA.z) * _zRes;
		double bZ = ((double)z) * _zRes;
		
		double dCol = aCol-bCol;
		double dRow = aRow-bRow;
		double dZ = aZ-bZ;
		double d = Math.sqrt(dCol*dCol + dRow*dRow + dZ*dZ);
		return d;
	}
	public double distance(final GraphNode nodeA, double x, double y, double z)
	{
		
		double aCol = ((double)nodeA.col) * _xRes;
		double bCol = (x) * _xRes;
		
		double aRow = ((double)nodeA.row) * _yRes;
		double bRow = (y) * _yRes;
		
		double aZ = ((double)nodeA.z) * _zRes;
		double bZ = (z) * _zRes;
		
		double dCol = aCol-bCol;
		double dRow = aRow-bRow;
		double dZ = aZ-bZ;
		double d = Math.sqrt(dCol*dCol + dRow*dRow + dZ*dZ);
		return d;
	}
	
	public double distance(final GraphNode nodeA, final Position pos)
	{
		
		double aCol = ((double)nodeA.col) * _xRes;
		double bCol = ((double)pos.getColumn()) * _xRes;
		
		double aRow = ((double)nodeA.row) * _yRes;
		double bRow = ((double)pos.getRow()) * _yRes;
		
		double aZ = ((double)nodeA.z) * _zRes;
		double bZ = ((double)pos.getZ()) * _zRes;
		
		double dCol = aCol-bCol;
		double dRow = aRow-bRow;
		double dZ = aZ-bZ;
		double d = Math.sqrt(dCol*dCol + dRow*dRow + dZ*dZ);
		return d;
	}
	
	public double distance(final Position posA, final Position posB)
	{
		
		double aCol = ((double)posA.getColumn()) * _xRes;
		double bCol = ((double)posB.getColumn()) * _xRes;
		
		double aRow = ((double)posA.getRow()) * _yRes;
		double bRow = ((double)posB.getRow()) * _yRes;
		
		double aZ = ((double)posA.getZ()) * _zRes;
		double bZ = ((double)posB.getZ()) * _zRes;
		
		double dCol = aCol-bCol;
		double dRow = aRow-bRow;
		double dZ = aZ-bZ;
		double d = Math.sqrt(dCol*dCol + dRow*dRow + dZ*dZ);
		return d;
	}
	
	public short getCol() {
		return _col;
	}

	public short getRow() {
		return _row;
	}

	public short getColRow() {
		return _colRow;
	}

	public short getColZ() {
		return _colZ;
	}

	public short getRowZ() {
		return _rowZ;
	}

	public short getColRowZ() {
		return _colRowZ;
	}

	public short getZ() {
		return _z;
	}
	public float getXRes() {
		return _xRes;
	}
	public float getYRes() {
		return _yRes;
	}
	public float getZRes() {
		return _zRes;
	}
	
}

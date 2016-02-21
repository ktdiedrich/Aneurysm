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

import java.util.Random;

/** Defines integer arrays of colors.
 * @author ktdiedrich@gmail.com 
 * */
public class Colors 
{
	public static final int RGB_HIGH = 83;
	public final int[] bifurcation;
	public final int[] centerline;
	public final int[] source;
	public final int[] magenta;
	public final int[] cyan;
	public final int[] surround;
	public final int[] mark;
	public final int[] off;
	public final int[] blue;
	public final int[] gray;
	public final int[] orange;
	public final int[] firstLine;
	public final int[] cycle;
	public final int[] spring;
	public final int[] orchid;
	private static Colors _colors;
	private static Random _random;
	
	/** Get colors returns a reference to a single instance of Colors to save memory by preventing duplication */
	public static Colors getColors()
	{
		if (_colors == null)
			_colors = new Colors();
		return _colors;
	}
	private Colors()
	{
		_random = new Random();
		// green bifurcation is brighter than red centerline 
        centerline = new int[3];
        centerline[0] = 255; centerline[1] = 50; centerline[2] = 50; // reddish
        
        bifurcation = new int[3];
        bifurcation[0]= 51; bifurcation[1] = 255; bifurcation[2] = 51; // greenish  
        // mark ICA artery lower ends 
        magenta = new int[3];
        magenta[0] = 255; magenta[1] = 100; magenta[2] = 255; // magenta
        cyan = new int[3];
        cyan[0] = 0; cyan[1] = 254; cyan[2] = 254; // cyan
        
        // Graph Source node color 
        source = new int[3];
        source[0] = 255; source[1] = 255; source[2] = 25; // yellow
        
        surround = new int[3];
        surround[0] = 50; surround[1] = 50; surround[2] = 50;
        
        mark = new int[3];
        mark[0] = 255; mark[1] = 255; mark[2] = 255; // white
        
        off = new int[3];
        off[0] = 84; off[1] = 84; off[2] = 84; // gray
        
        blue = new int[3];
        blue[0] = 0; blue[1] = 0; blue[2] = 251;
        
        gray = new int[3];
        gray[0] = 20; gray[1] = 20; gray[2] = 20;
        
        orange = new int[3];
        orange[0] = 255; orange[1] = 165; orange[2] = 0;
        
        firstLine = new int[3];
        firstLine[0] = 255; firstLine[1]=130; firstLine[2]=71;
     
        cycle = new int[3];
        cycle[0] = 255; cycle[1]=165; cycle[2]=0; 
        
        spring = new int[3];
        spring[0]=0; spring[1]=250; spring[2]=154;
        
        orchid = new int[3];
        orchid[0] = 255; orchid[1]=  	131; orchid[2] =  	250;
	}
	public int[] randomColor(int high)
	{
		int[] color = new int[3];
        
        color[0] = _random.nextInt(high);
		color[1] = _random.nextInt(high);
		color[2] = _random.nextInt(high);
		return color;
	}
	/** Makes a new stronger color adding strength to R, G and B channels to a maximum of 255 for each channel. */
	public static int[] strengthen(final int[] color, int strength)
	{
		int[] newColor = new int[color.length];
		for (int i=0; i < color.length; i++)
		{
			int newC = color[i]+strength;
			if (newC > 255)
			{
				newC = 255;
			}
			newColor[i] = newC;
		}
		return newColor; 
	}
}

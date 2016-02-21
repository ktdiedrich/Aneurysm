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

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Creates a threshold for of a window of an image using a histogram and lower
 * fractional threshold of the histogram. Used for picking windowed thresholds
 * in the seed image from region growing.
 * @author ktdiedrich@gmail.com */
public class WindowThreshold 
{
	private double _thresholdFraction;
	private int _windowRadius;
	private short[] _imagePixels;
	private int _height, _width;
	public WindowThreshold(ImageProcessor image, double thresholdFraction, int windowRadius)
	{
		_thresholdFraction = thresholdFraction;
		_windowRadius = windowRadius;
		_imagePixels = (short[])image.getPixels();
		_height = image.getHeight();
		_width = image.getWidth();
	}
	/** @param imagePixels 1-D array of row major order 2-D image pixels 
	 * @param x. column of center of window 
	 * @param y. row of center of window */
	public short windowThreshold(int x, int y)
	{
		short thres = 0;
		// int inset = _windowRadius;
		int diameter = (_windowRadius*2) + 1;
        List<Short> window = new ArrayList<Short>(diameter*diameter);
		
		int left = x - _windowRadius;
		if (left < 0) left = 0;
		int right = x  + _windowRadius;
		if (right > _width-1) right = _width-1;
		int top = y - _windowRadius;
		if (top < 0) top = 0;
		int bottom = y + _windowRadius;
		if (bottom > _height-1) bottom = _height -1;
		for (int c = left; c <= right; c++)
		{
			for (int r = top; r <= bottom; r++)
			{
				short val = _imagePixels[r*_width+c];
				if (val > 0)
				{
					window.add(val);
				}
			}
		}
		Collections.sort(window);
		int windowSize = window.size();
		int nTile = (int)Math.round((windowSize*_thresholdFraction));
		thres = window.get(nTile);
		return thres;
	}
	
	public double getThresholdFraction() {
		return _thresholdFraction;
	}

	public void setThresholdFraction(double thresholdFraction) {
		this._thresholdFraction = thresholdFraction;
	}
	public int getWindowRadius() {
		return _windowRadius;
	}
	public void setWindowRadius(int windowRadius) {
		_windowRadius = windowRadius;
	}
	
}


/*
for (int row=inset; row < _height-inset; row++)
{
    boolean madeFirst = false;
    List<Short> windowVals = new ArrayList<Short>(diameter*diameter);
    for (int col=inset; col<_width-inset; col++)
    {
        if (_imagePixels[row*_width+col] > 0)
        {
            if (madeFirst == false) // fill first window in row 
            {
                for (int i=-_windowRadius; i<=_windowRadius; i++)
                {
                    for (int j=-_windowRadius; j <=_windowRadius; j++)
                    {
                        short p = _imagePixels[((row+i)*_width)+(col+j)];
                        if (p>0)
                            windowVals.add(p);
                    }
                }
                madeFirst = true;
            }
            else // shift columns
            {
                for (int i=-_windowRadius; i<=_windowRadius; i++)
                {
                    short p = 0;
                    p = _imagePixels[((row+i)*_width)+(col-_windowRadius-1)];
                    if (p > 0)
                       windowVals.remove((Short)p);
                    p = _imagePixels[((row+i)*_width)+(col+_windowRadius)];
                    if (p>0)
                        windowVals.add(p);
                }
            }
            Collections.sort(windowVals);
            int ln = windowVals.size();
            int cn = ln/2;
            Short val = 0;
            if (ln%2 == 1)
                val = windowVals.get(cn);
            else
                val = (short) ((windowVals.get(cn) + windowVals.get(cn-1))/2);
            //medianPixels[row*width+col] = val.shortValue(); 
        }   
    }
}
*/
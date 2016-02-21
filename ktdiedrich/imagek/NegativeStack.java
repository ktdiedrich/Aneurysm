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

import ij.ImageStack;
import ij.process.ImageProcessor;

/** Make a negative stack by removing voxels included in the artery reconstruction. 
 * This is to check for missed blood vessels. 
 * @author Karl T. Diedrich <ktdiedrich@gmail.com> */
public class NegativeStack
{
    public NegativeStack()
    {
        
    }
    public void negate(ImageStack origStack, ImageStack reconStack)
    {
        int height = origStack.getHeight();
        int width = origStack.getWidth();
        //ImageStack negStack = new ImageStack(width, height);
        for(int i=1; i<= origStack.getSize(); i++)
        {
            ImageProcessor reconProc = reconStack.getProcessor(i);
            short[] reconPix = (short[])reconProc.getPixels();
            ImageProcessor origProc = origStack.getProcessor(i);
            short[] origPix = (short[])origProc.getPixels();
            for (int j=0; j<reconPix.length; j++)
            {
                if (reconPix[j] > 0)
                {
                    origPix[j] = 0;
                }
            }
        }
        
    }
}

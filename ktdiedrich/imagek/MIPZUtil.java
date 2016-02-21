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

import ij.*;
import ij.process.*;

/** Maximum Intensity Projection Z buffer 
 * @author Karl Diedrich <ktdiedrich@gmail.com>*/
public class MIPZUtil 
{
    private MIPZUtil()
    {
    }
    public static ImagePlus createMIPZImage(ImagePlus image)
    {
        return createMIPZImage(image, 0);
    }
    public static ImagePlus createMIPZImage(ImagePlus image, int threshold)
    {
    	ImageStack stack = image.getStack();
        int height = stack.getHeight();
        int width = stack.getWidth();
        int slices = stack.getSize();       
        Object slice1 = stack.getPixels(1);
        String dataType = slice1.getClass().getName();
        // TODO handle other types besides short 
        short[] slice1data = (short[])stack.getPixels(1); 
        
        short[] mipData = new short[slice1data.length];
        short[] sliceData;
        short[] zData = new short[slice1data.length];
        for (short i=1; i <= slices; i++)
        {
            sliceData = (short[])stack.getPixels(i);   
            for (int j=0; j<sliceData.length; j++)
            {
                if(sliceData[j] > threshold && sliceData[j] > mipData[j])
                {
                    mipData[j] = sliceData[j];       
                    zData[j] = i;
                }
            }
        }
        
        ImageProcessor mipProc = new ShortProcessor(width, height, zData, null);
        ImagePlus mip = new ImagePlus(image.getShortTitle()+"MIP_ZBuffer", mipProc);
        return mip;
    }
}

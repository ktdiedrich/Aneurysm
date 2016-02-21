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
import ktdiedrich.math.MatrixUtil;

/** Matrix Utilities specific to ImageJ
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class MatrixJUtil
{
    protected MatrixJUtil()
    {
        
    }
    public static short[][][] getCube(ImageStack stack)
    {
        int zSize = stack.getSize();
        int height = stack.getHeight();
        int width = stack.getWidth();
        short[][][] cube = new short[zSize][][];
        for (int i=0; i<zSize; i++)
        {
            cube[i] = MatrixUtil.array2Square((short[])stack.getPixels(i+1), height, width );
        }
        return cube;
    }
}

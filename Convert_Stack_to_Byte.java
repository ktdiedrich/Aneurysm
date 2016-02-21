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

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/** Maximum Intensity Projection Z-Buffer  
* @author Karl Diedrich <ktdiedrich@gmail.com> 
*/
public class Convert_Stack_to_Byte implements PlugInFilter {
    protected ImagePlus _imp;

    public int setup(String arg, ImagePlus imp) 
    {
        _imp = imp;
        return DOES_ALL+STACK_REQUIRED;
    }
    
    public void run(ImageProcessor ip) 
    {
        
        ImageStack stack = _imp.getStack();
        ImageStack byteStack = new ImageStack(stack.getWidth(), stack.getHeight());
        for (int i=1; i<=stack.getSize(); i++)
        {
            byteStack.addSlice(""+i, stack.getProcessor(i).convertToByte(true));
        }
        ImagePlus byteIm = new ImagePlus();
        byteIm.setStack("8Bit", byteStack);
        byteIm.show();
        byteIm.updateAndDraw();
    }

}


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

import java.awt.Color;

// import jRenderer3D.JRenderer3D;
import ij.ImagePlus;
import ij.plugin.PlugIn;


/** @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Drawing_3D implements PlugIn
{
    public void run(String arg)
    {
        /* 
         * 
        JRenderer3D render = new JRenderer3D(200, 200, 200);
        render.add3DCube(20, 30, 40, 120, 130, 140, Color.blue);
        render.addPoint3D(30, 40, 50, 10, Color.magenta, JRenderer3D.POINT_SPHERE);
        render.setSurfacePlotLut(JRenderer3D.LUT_ORANGE); // select a LUT
        render.setSurfacePlotLight(0.1); // set lighting
        render.setSurfacePlotMode(JRenderer3D.SURFACEPLOT_FILLED); 
        render.doRendering();
        ImagePlus imp = new ImagePlus("Drawing 3D", render.getImage());
        imp.show();
        imp.updateAndDraw();
        */
    }
}

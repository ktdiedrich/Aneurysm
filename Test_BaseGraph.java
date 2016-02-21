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

import java.util.List;

import ktdiedrich.imagek.BaseGraph;
import ktdiedrich.imagek.ImageProcess;
import ktdiedrich.math.MatrixUtil;
import ij.plugin.*;
import ij.text.TextWindow;

/** Test building a graph from a 3D image 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Test_BaseGraph implements PlugIn
{
    
    public void run(String s) 
    {        
        short[][][] voxels1 = {
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,1,1,1,1,1,1,1,1,1,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,6,6,0,0,0,0,0,0,0,0},
                    {0,6,6,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,2,0}, 
                    {0,0,0,0,0,0,0,0,0,3,0},
                    {0,0,0,0,0,0,0,0,0,4,0},
                    {0,0,0,0,0,0,0,0,0,5,0},
                    {0,0,0,0,0,0,0,0,0,6,0},
                    {0,5,6,0,0,0,0,0,0,7,0},
                    {0,8,7,0,0,0,0,0,0,8,0},
                    {0,0,0,0,0,0,0,0,0,9,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,3,6,3,6,3,6,3,6,3,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,7,0,0,0,0,0,0},
                    {0,1,2,0,0,8,0,0,0,0,0},
                    {0,4,3,0,0,0,9,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                }
        };
        float[][][] voxels2 = {
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,1,1,1,1,1,1,1,1,1,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,4.1F,5.3F,0,0,0,0,0,0,0,0},
                    {0,6.6F,7.9F,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,2.0F,0}, 
                    {0,0,0,0,0,0,0,0,0,3.1F,0},
                    {0,0,0,0,0,0,0,0,0,4.2F,0},
                    {0,0,0,0,0,0,0,0,0,5.3F,0},
                    {0,0,0,0,0,0,0,0,0,6.4F,0},
                    {0,5,5,0,0,0,0,0,0,7.5F,0},
                    {0,5,5,0,0,0,0,0,0,8.6F,0},
                    {0,0,0,0,0,0,0,0,0,9.7F,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,3,4.9F,3,3,3,3.9F,3,3,3,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,4,4,0,0,3.7F,1.7F,0,0,0,0},
                    {0,4,4,0,0,1.7F,3.7F,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                },
                {
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0}, 
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0},
                    {0,0,0,0,0,0,0,0,0,0,0}
                }
                
                
        };
        test(voxels1);
        //test(voxels2);
    }
    protected void test(short[][][] cubic)
    {
        int zSize = cubic.length;
        int height = cubic[0].length;
        int width = cubic[0][0].length;
        short[][] voxels = MatrixUtil.cube2rect(cubic);
        
        BaseGraph<Short> bg = new BaseGraph<Short>();
        
        List<BaseGraph<Short>> graphs = bg.makeGraphs(voxels, width, height);
        short[][] s = BaseGraph.makeShort(graphs, width, height, zSize);
        ImageProcess.display(s, width, height, "Display_Graph");
    }
    protected void test(float[][][] cubic)
    {
        int zSize = cubic.length;
        int height = cubic[0].length;
        int width = cubic[0][0].length;
        float[][] voxels = MatrixUtil.cube2rect(cubic);
        
        BaseGraph<Float> bg = new BaseGraph<Float>();
        
        List<BaseGraph<Float> > graphs = bg.makeGraphs(voxels, width, height);
        float[][] f = BaseGraph.makeFloat(graphs, width, height, zSize);
        ImageProcess.display(f, width, height, "Display_Graph");
    }
}
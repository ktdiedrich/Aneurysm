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

import ktdiedrich.imagek.TortuosityScore;

import ij.plugin.*;
import ij.text.TextWindow;

/** Dimensionless tortuosity Score 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class Test_Tortuosity_Score implements PlugIn
{
    
    public void run(String s) 
    {        
        TortuosityScore scorer = new TortuosityScore();
        scorer.setBorderDFE(1);
        scorer.setShowSteps(false);
        new TextWindow("Test Tortuosity Score", scorer.tests(), 800, 400);
    }
}

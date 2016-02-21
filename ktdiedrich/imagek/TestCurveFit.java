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

import ij.measure.CurveFitter;

public class TestCurveFit
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        
        double[] x = {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0 };
        double[] y = {1.1, 2.3, 1.9, 3.5, 4.9, 5.8, 4.9, 3.1, 2.3, 1.3 };
        CurveFitter curve = new CurveFitter(x, y);
        curve.doFit(CurveFitter.POLY4);
        double[] params = curve.getParams();
        System.out.println(curve.getResultString());
        for (int i=0; i < x.length; i++)
        {
            double fit = params[0]+ params[1]*x[i] + params[2]*Math.pow(x[i], 2) + params[3]*Math.pow(x[i], 3) + 
                params[4]*Math.pow(x[i], 4);
            System.out.println(x[i]+"\t"+y[i]+"\t"+fit);
        }
    }

}

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Text Utilities
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class TextUtil
{
	private Pattern _xPat, _yPat, _zPat;
    public TextUtil()
    {
        _xPat = Pattern.compile("x(\\d+)");
        _yPat = Pattern.compile("y(\\d+)");
        _zPat = Pattern.compile("z(\\d+)");
    }
    public int[] getXYZ(String s)
    {
        int[] xyz = new int[3];
            
        Matcher mat = _xPat.matcher(s);
        while (mat.find())
        {
            xyz[0] = Integer.parseInt(mat.group(1));
            
        }
        mat = _yPat.matcher(s);
        while (mat.find())
        {
            xyz[1] = Integer.parseInt(mat.group(1));
        }
        mat = _zPat.matcher(s);
        while (mat.find())
        {
            xyz[2] = Integer.parseInt(mat.group(1));
        }
        return xyz;
    }
    public static void main(String[] args)
    {
        TextUtil tutil = new TextUtil();
        String fileName = "000047_0183469380x426y454z114.gz";
        
        int[] xyz = tutil.getXYZ(fileName);
        System.out.print("Parse x, y, z from file name: ");
        for (int i=0; i < xyz.length; i++)
        {
            System.out.print(xyz[i]+", ");
        }
        System.out.println();
    }
}

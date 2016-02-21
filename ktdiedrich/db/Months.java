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

package ktdiedrich.db;

/** Month operations
 * @author ktdiedrich@gmail.com 
 * */
public class Months 
{
	/** @return null on failure to convert month string. */
	public static String threeLetter2twoNumber(String mon)
	{
		if (mon.equalsIgnoreCase("jan")) return "01";
		if (mon.equalsIgnoreCase("feb")) return "02";
		if (mon.equalsIgnoreCase("mar")) return "03";
		if (mon.equalsIgnoreCase("apr")) return "04";
		if (mon.equalsIgnoreCase("may")) return "05";
		if (mon.equalsIgnoreCase("jun")) return "06";
		if (mon.equalsIgnoreCase("jul")) return "07";
		if (mon.equalsIgnoreCase("aug")) return "08";
		if (mon.equalsIgnoreCase("sep")) return "09";
		if (mon.equalsIgnoreCase("oct")) return "10";
		if (mon.equalsIgnoreCase("nov")) return "11";
		if (mon.equalsIgnoreCase("dec")) return "12";
		else
			return null;
	}
}

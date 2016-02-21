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

package ktdiedrich.util;

import java.io.File;

/** Home directory on Linux, UNIX or windows */
public class Home 
{
	public static String getHome()
	{
		String unix = System.getenv("HOME");
	    String windows = System.getenv("USERPROFILE");
	    // System.out.print("Unix: "+unix+" Windows:"+windows);
	    if (unix != null && new File(unix).exists())
	        return unix;
	    else if (windows != null && new File(windows).exists())
	        return windows;
	    return null;
	}
	public static void main(String[] args)
	{
		String home = Home.getHome();
		System.out.println(home);
	}
}

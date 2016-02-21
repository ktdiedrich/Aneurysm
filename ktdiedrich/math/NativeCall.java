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

package ktdiedrich.math;

/** Simple native code call to test native library loading is working.  
 * @author ktdiedrich@gmail.com
 * @param args
 */
public class NativeCall 
{	
	public native static void greeting();
	static 
	{
		System.loadLibrary("NativeCall");
	}
	
	public static void main(String[] args) 
	{
		NativeCall.greeting();	
	}
}

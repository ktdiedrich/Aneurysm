#!/bin/sh 

#/*=========================================================================
# *
# *  Copyright (c) Karl T. Diedrich 
# *
# *  Licensed under the Apache License, Version 2.0 (the "License");
# *  you may not use this file except in compliance with the License.
# *  You may obtain a copy of the License at
# *
# *         http://www.apache.org/licenses/LICENSE-2.0.txt
# *
# *  Unless required by applicable law or agreed to in writing, software
# *  distributed under the License is distributed on an "AS IS" BASIS,
# *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# *  See the License for the specific language governing permissions and
# *  limitations under the License.
# *
# *=========================================================================*/
# generate the header 
# javah -jni ktdiedrich.math.NativeCall

g++ -o libNativeCall.so -shared -Wl,-soname,libNativeCall.so -I/usr/lib/jvm/java-1.6.0-sun/include -I/usr/lib/jvm/java-1.6.0-sun/include/linux NativeCall.cpp -static -lc

g++ -o libNativeDijkstra.so -shared -Wl,-soname,libNativeDijkstra.so -I/usr/lib/jvm/java-1.6.0-sun/include -I/usr/lib/jvm/java-1.6.0-sun/include/linux NativeDijkstra.cpp -static -lc

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


g++ -mno-cygwin -I /cygdrive/c/Program\ Files/Java/jdk1.6.0_02/include -I /cygdrive/c/Program\ Files/Java/jdk1.6.0_02/include/win32 -Wl,--add-stdcall-alias -shared -o NativeCall.dll NativeCall.cpp

g++ -mno-cygwin -I /cygdrive/c/Program\ Files/Java/jdk1.6.0_02/include -I /cygdrive/c/Program\ Files/Java/jdk1.6.0_02/include/win32 -Wl,--add-stdcall-alias -shared -o NativeDijkstra.dll NativeDijkstra.cpp

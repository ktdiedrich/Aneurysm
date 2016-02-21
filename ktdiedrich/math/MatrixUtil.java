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
/** @todo Only works on short 
 * @author Karl Diedrich <ktdiedrich@gmail.com>
 * */
public class MatrixUtil
{
    private MatrixUtil()
    {
        
    }
    public static short[][] array2Square(short[] ar, int rows, int cols)
    {
        short[][] square = new short[rows][cols];
        int c=0, r=0;
        
        for (int i=0; i < ar.length; i++)
        {
            if (c == cols)
            {
                c = 0;
                r++;
            }    
            square[r][c] = ar[i];                   
            c++;
        }
        return square;
    }
    public static byte[] square2array(byte[][] square)
    {
        int height = square.length;
        int width = square[0].length;
	
        byte[] ar = new byte[height*width];
        int i=0;
        for (int r = 0; r < height; r++)
	    {
            for (int c=0; c < width; c++)
		    {
                ar[i] = square[r][c];
                i++;
		    }
	    }
	return ar;
	
    }
    public static short[] square2array(short[][] square)
    {
        int height = square.length;
        int width = square[0].length;
	
        short[] ar = new short[height*width];
        int i=0;
        for (int r = 0; r < height; r++)
	    {
            for (int c=0; c < width; c++)
		    {
                ar[i] = square[r][c];
                i++;
		    }
	    }
	return ar;
	
    }
    public static float[] square2array(float[][] square)
    {
        int height = square.length;
        int width = square[0].length;
    
        float[] ar = new float[height*width];
        int i=0;
        for (int r = 0; r < height; r++)
        {
            for (int c=0; c < width; c++)
            {
                ar[i] = square[r][c];
                i++;
            }
        }
    return ar;
    
    }
    public static byte[][] cube2rect(byte[][][] cube)
    {
        int zSize = cube.length;
        byte[][]  rect = new byte[zSize][cube[0].length * cube[0][0].length];
        for (int z=0; z < zSize; z++)
        {
            rect[z] = square2array(cube[z]);
        }
        return rect;
    }
    public static short[][] cube2rect(short[][][] cube)
    {
        int zSize = cube.length;
        short[][]  rect = new short[zSize][cube[0].length * cube[0][0].length];
        for (int z=0; z < zSize; z++)
        {
            rect[z] = square2array(cube[z]);
        }
        return rect;
    }
    public static float[][] cube2rect(float[][][] cube)
    {
        int zSize = cube.length;
        float[][]  rect = new float[zSize][cube[0].length * cube[0][0].length];
        for (int z=0; z < zSize; z++)
        {
            rect[z] = square2array(cube[z]);
        }
        return rect;
    }
    
    public static void main(String args[])
    {
        short[] ar = {3,5,7, 9,11,13, 15,17,19, 21, 23, 25};
        int cols = 3; // width
        int rows = 4; // height
        
        short[][] square = array2Square(ar, rows, cols);
        for (int r=0; r < rows; r++)
        {
            for (int c=0; c < cols; c++)
            {
                System.out.print(square[r][c]+",");
            }
            System.out.println();
        }
        short[] ar2 = square2array(square);
        for (int i=0; i<ar2.length; i++)
	    {
            System.out.print(ar2[i]+", ");
	    }
        System.out.println();
        short[][][] cube = { {
            {0,0,0,0,0}, 
            {0,1,1,0,0}, 
            {0,1,1,0,0},
            {0,0,0,0,0},
        },
        {
            {0,0,0,0,0}, 
            {0,2,2,0,0}, 
            {0,2,2,0,0},
            {0,0,0,0,0}
        }, 
        {
            {0,0,0,0,0}, 
            {0,3,3,0,0}, 
            {0,3,3,0,0},
            {0,0,0,0,0}
        },
        {
            {0,0,0,0,0}, 
            {0,4,4,0,0}, 
            {0,4,4,0,0},
            {0,0,0,0,0}
        },
        {
            {0,0,0,0,0}, 
            {0,6,6,0,0}, 
            {0,6,6,0,0},
            {0,0,0,0,0}
        },
        {
            {0,0,0,0,0}, 
            {0,7,7,0,0}, 
            {0,7,7,0,0},
            {0,0,0,0,0}
        }
        };
        System.out.println("Cube2rect: ");
        short[][] rect = cube2rect(cube);
        for (int i=0; i<rect.length; i++)
        {
            for (int j=0; j<rect[0].length; j++)
            {
                System.out.print(rect[i][j]+", ");
            }
            System.out.println();
        }
    }

}

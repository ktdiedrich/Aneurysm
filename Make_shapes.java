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

import ktdiedrich.imagek.ImageProcess;
import ktdiedrich.imagek.Shapes;
import ktdiedrich.math.MatrixUtil;
import ij.ImagePlus;
import ij.plugin.PlugIn;


/** Make 3-D shapes for testing centerlines and tortuosity scores. 
 * @author ktdiedrich@gmail.com 
 * */
public class Make_shapes implements PlugIn 
{
	
	public void run(String arg0) 
	{
		Shapes shapes = new Shapes();
		int width = 300; 
		int height =300;
		int zSize = 300;
		shapes.setIntensity(Shapes.SEGMENTATION_INT);
		
		//ImagePlus lines = shapes.makeLines(width, height, zSize);
		//lines.show();
		//lines.updateAndDraw();
		
		//ImagePlus circle = shapes.makeSolidCircle(50, 50, 20, 30, 10);
		//circle.show();
		//circle.updateAndDraw();
		
		//ImagePlus mixed = shapes.makeMixed(400,400,400);
		//mixed.show();
		//mixed.updateAndDraw();
		
		//ImagePlus rods = shapes.makeRods(300, 300, 300);
		//rods.show();
		//rods.updateAndDraw();
		
//		ImagePlus sphereA = shapes.makeSphere(width, height, zSize, 10, 150, 150, 150, "sphereA");
//		sphereA.show();
//		sphereA.updateAndDraw();
//		
//		ImagePlus sphereB = shapes.makeSphere(width, height, zSize, 15, 180, 90, 190, "sphereB");
//		sphereB.show();
//		sphereB.updateAndDraw();
//
//		ImagePlus sphereC = shapes.makeSphere(width, height, zSize, 20, 125, 185, 75, "sphereC");
//		sphereC.show();
//		sphereC.updateAndDraw();
						      
		// ImagePlus helix100 = shapes.makeHelix(100, 100, 100, 100*.3, 1.0, 10.0, 2, "helix100", false);
		//helix100.show();
		// helix100.updateAndDraw();
		
		
		/*
		ImagePlus rod5r = shapes.makeRod1(width, height, zSize, false);
		rod5r.show();
		rod5r.updateAndDraw();
		
		ImagePlus rod5rCent = shapes.makeRod1(width, height, zSize, true);
		rod5rCent.show();
		rod5rCent.updateAndDraw();
		
		ImagePlus intersecting1 = shapes.makeIntersecting(width, height, zSize, false);
		intersecting1.show();
		intersecting1.updateAndDraw();
		
		ImagePlus intersectingCent1 = shapes.makeIntersecting(width, height, zSize, true);
		intersectingCent1.show();
		intersectingCent1.updateAndDraw();
		*/
		
		//ImagePlus helix5 = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 5.0, 6, "helix5c", false);
		//helix5.show();
		//helix5.updateAndDraw();
		
		//ImagePlus helix5Cent = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 5.0, 6, "helix5Cent", true);
		//helix5Cent.show();
		//helix5Cent.updateAndDraw();
		
		
		//ImagePlus helix10 = shapes.makeHelix(width, height, zSize, width*.1, 1.0, 10.0, 6, "helix10c", false);
		//helix10.show();
		//helix10.updateAndDraw();
		
		//ImagePlus helix10Cent = shapes.makeHelix(width, height, zSize, width*.1, 1.0, 5.0, 6, "helix10Cent", true);
		//helix10Cent.show();
		//helix10Cent.updateAndDraw();
		
		
		/* 
		ImagePlus helix25 = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 25.0, 6, "helix25c", false);
		helix25.show();
		helix25.updateAndDraw();
		*/ 
		//ImagePlus helix25Cent = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 10.0, 6, "helix25Cent", true);
		//helix25Cent.show();
		//helix25Cent.updateAndDraw();
		
		/* 
		ImagePlus t1 = shapes.makeT(width, height, zSize, 1, false);
		t1.show();
		t1.updateAndDraw();
		
		ImagePlus t1cent = shapes.makeT(width, height, zSize, 1, true);
		t1cent.show();
		t1cent.updateAndDraw();
		
		ImagePlus t2 = shapes.makeT(width, height, zSize, 2, false);
		t2.show();
		t2.updateAndDraw();
		
		ImagePlus t2cent = shapes.makeT(width, height, zSize, 2, true);
		t2cent.show();
		t2cent.updateAndDraw();
		*/
		
		/*
		width = 100; height = 100; zSize = 100;
		ImagePlus t3 = shapes.makeT(width, height, zSize, 3, false);
		t3.show();
		t3.updateAndDraw();
		*/ 
		/*
		width = 100; height = 100;
		ImagePlus t3cent = shapes.makeT(width, height, zSize, 3, true);
		t3cent.show();
		t3cent.updateAndDraw();
		*/
		
		int tubeRadius = 0;
		boolean withCenterline = false;
		
		ImagePlus helix5 = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 5.0, tubeRadius, "helix5p", withCenterline);
		helix5.show();
		helix5.updateAndDraw();
		
		
		ImagePlus helix10 = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 10.0, tubeRadius, "helix10p", withCenterline);
		helix10.show();
		helix10.updateAndDraw();
		
		ImagePlus helix20 = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 20.0, tubeRadius, "helix20p", withCenterline);
		helix20.show();
		helix20.updateAndDraw();
		
		ImagePlus helix40 = shapes.makeHelix(width, height, zSize, width*.3, 1.0, 40.0, tubeRadius, "helix40p", withCenterline);
		helix40.show();
		helix40.updateAndDraw();
		
		
		/*
		int rad = 7;
		int dim = rad*2+1;
		short[][][] solidSphere = Shapes.makeSolidSphereStructure(rad, (short)3);
		short[][] ss = MatrixUtil.cube2rect(solidSphere);
		//short[][] ss = Shapes.makeSolidSphereStructure(rad, (short)3);
		ImagePlus ssIm = ImageProcess.makeImage(ss, dim, dim, "SphereStructure");
		ssIm.show();
		ssIm.updateAndDraw();
		*/
	}
}

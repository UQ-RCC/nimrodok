package org.monash.nimrod.optim;

import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import org.junit.Assert;
import org.junit.Test;

public class SelectPointsTest {

	private static final String CENTRESPACED_PARAMSTRING = "parameter fred label \"fff\" float range from 1 to 22 step 4\n parameter jim integer range from 1 to 22";
	private static final String CENTRESPACED_POINTSTRING = "startingpoints centrespaced 3 by 3";
	
	@Test
	public void centreSpacedTest() throws Exception {
		
		double[][] expectedValues = new double[][] {
			new double[] {4.5, 5.0},
			new double[] {4.5, 12.0},
			new double[] {4.5, 19.0},
			new double[] {11.5, 5.0},
			new double[] {11.5, 12.0},
			new double[] {11.5, 19.0},
			new double[] {18.5, 5.0},
			new double[] {18.5, 12.0},
			new double[] {18.5, 19.0},
		};
		
		SelectPoints selectPoints = new SelectPoints(CENTRESPACED_POINTSTRING);
		selectPoints.generatePoints(SetOfParams.fromString(CENTRESPACED_PARAMSTRING));
		
		Assert.assertEquals(expectedValues.length, selectPoints.pointCount);
		
		for(int i = 0; i < selectPoints.pointCount; ++i) {
			Assert.assertArrayEquals(expectedValues[i], selectPoints.pointArray[i].coords, 0.0001);
		}
	}
	
	private static final String SPECIFIED_PARAMSTRING = "parameter x1 float range from 0.0 to 1.0;\n"
			+ "parameter x2 float range from 0.0 to 1.0;\n"
			+ "parameter x3 float range from 0.0 to 1.0;\n";
	
	private static final String SPECIFIED_POINTSTRING = "startingpoints specified\n"
			+ "{ 0.5 0.5 0.5 }";
	
	@Test
	public void specificPointsTest() throws Exception {
		SelectPoints selectPoints = new SelectPoints(SPECIFIED_POINTSTRING);
		
		SetOfParams sop = SetOfParams.fromString(SPECIFIED_PARAMSTRING);
		
		selectPoints.generatePoints(sop);
	}
}

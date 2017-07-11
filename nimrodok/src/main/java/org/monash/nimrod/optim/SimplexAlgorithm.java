package org.monash.nimrod.optim;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.ConvergenceTest;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import java.util.*;

public class SimplexAlgorithm extends OptimizationAlgorithm {

	public boolean algorithmReady;
	// settings for optimization method
	public double tolerance;
	public boolean originalMethod;
	public int maxIters;

	public int optimIndex;

	public ConvergenceTest conTest;
	public boolean convergent = false;

	// search space and points in it
	// This is now in the parent: public SetOfParams setOfParams;	
	boolean pointsDefined;
	int dimensionality;

	ArrayOfPoints simplexPoints;
	OptimPoint[] simplexPoint;

	ArrayOfPoints alternativePoints;
	OptimPoint[] alternativePoint;
	public ArrayOfPoints evalSet;
	public int pointsEvaluated;

	//ArrayOfPoints resultPoints;
	public OptimPoint resultPoint;

	// current algorithm state
	int iteration;
	boolean evaluated;
	boolean ordered = false;
	public SimplexState simplexState;

	int least;
	/* index for best vertex */
	int greatest;
	/* worst vertex */
	int next_greatest;
	/* second worst vertex */

	double bestObjective;
	double worstObjective;

	private OptimPoint faceCentre;	// centroid of face opposite the worst vertex
	private static final double EPSILON = 1.2e-7;

	public int jobCount;
	public int jobRequestsCount;
	public int batchRequestsCount;
	public int batchCount;
	private Map<String, Integer> jobMap;

	public int debug = 0;

	public SimplexAlgorithm(int maxIters, boolean originalMethod, double tolerance, OptimPoint pnt) {
		super(pnt);
		this.maxIters = maxIters;
		this.originalMethod = originalMethod;
		this.tolerance = tolerance;
		iteration = 0;
		setOfParams = pnt.setOfParams;
		createSimplex(pnt);
		pointsDefined = true;
		algorithmReady = true;
		faceCentre = new OptimPoint(pnt);
		jobMap = new HashMap<>();
		jobCount = 0;
		jobRequestsCount = 0;
		batchRequestsCount = 0;
		batchCount = 0;
	}
	
	public SimplexAlgorithm(SimplexSettings simSettings, OptimPoint pnt) {
		this(simSettings.maxIters, simSettings.originalMethod, simSettings.tolerance, pnt);
	}

	// given one point, creates simplex
	public final void createSimplex(OptimPoint pnt) {
		//Debug.write("In SimplexAlgorithm:createSimplex", debug, 1);
		if(!pnt.defined) {
			throw new Error("OptimPoint is not defined");
		}
		dimensionality = pnt.dimensionality;

		simplexPoints = new ArrayOfPoints(pnt, pnt.dimensionality + 1);
		simplexPoint = simplexPoints.pointArray;

		alternativePoints = new ArrayOfPoints(pnt, 4);
		alternativePoint = alternativePoints.pointArray;

		double mean, decile;

		simplexPoint[0] = pnt;

		for(int i = 1; i <= dimensionality; ++i) /* other vertices are obtained by moves of 1 coord */ {
			simplexPoint[i] = new OptimPoint(pnt);
			for(int j = 0; j < dimensionality; ++j) {
				simplexPoint[i].coords[j] = pnt.coords[j];
			}
			mean = (pnt.max[i - 1] + pnt.min[i - 1]) / 2.0;
			decile = (pnt.max[i - 1] - pnt.min[i - 1]) / 10.0;
			if(simplexPoint[i].coords[i - 1] > mean) {
				simplexPoint[i].coords[i - 1] -= decile;
			} else {
				simplexPoint[i].coords[i - 1] += decile;
			}

		}
		iteration = 0;
		simplexState = SimplexState.NEW_SIMPLEX;
		evalSet = simplexPoints;

	}

	public void storeJobs(ArrayOfPoints array) {
		Integer jobNumber;

		OptimPoint pt = array.pointArray[0];
		++batchRequestsCount;
		boolean found;

		found = false;
		for(int i = 0; i < array.numPoints; i++) {
			++jobRequestsCount;

			pt = array.pointArray[i];
			String evalString = pt.generateEvalString();

			jobNumber = jobMap.get(evalString);
			if(jobNumber == null) // new job
			{
				jobMap.put(evalString, jobCount);
				++jobCount;
				//System.out.println("jobCount is: "+jobCount);
				found = true;
			}
		}
		if(found) {
			++batchCount;
		}

	}

	public void nextSimplex() throws NimrodOKException {
		/*--------------------------------------------------------------
		The downhill simplex method of Nelder and Mead. 
		------------------------------------------------------------*/

		//	double *starting_point;
		//	double toler;
		//	long *iter;
		int contracted_point;
		//OptimPoint[] alternativePoint = null;

		double tryCost;

		int i, j;

		//alternativePoint = new OptimPoint[4];  
		//for(i=1; i < 4; ++i)  /* each vertex */
		//	alternativePoint[i] = new OptimPoint(simplexPoint[0]) ;
		//Debug.write("In SimplexAlgorithm:nextSimplex: simplexState is "+simplexState, debug, 1);
		if(simplexState == SimplexState.NEW_SIMPLEX) {
			//Debug.write("In SimplexAlgorithm:nextSimplex: state is new simplex, iteration "+iteration, debug, 1);

			if(simplexPoints.allPointsEvaluated()) {
				//Debug.write("In SimplexAlgorithm:nextSimplex: all points evaluated ", debug, 1);
				evaluated = true;
				orderVertices();
			} else {
				throw new NimrodOKException("Expecting all points of new simplex to be evaluated");
			}

			//Debug.write("Simplex algorithm: checking convergence, conTest is "+ conTest, debug, 1);
			convergent = conTest.checkConvergence(simplexPoints);
			convergent = convergent || (iteration == maxIters);
			//Debug.write("Simplex algorithm: iteration is " + iteration +  " maxIters is " +maxIters, debug, 1);
			if(convergent) {
				//Debug.write("Simplex algorithm: checking convergence, convergence is true", debug, 1);
				resultPoint = new OptimPoint(simplexPoint[least]);
				return;
			}

			//Debug.write("creating alternative points");
			generateAlternativePoints();

			simplexState = SimplexState.ALT_POINTS;
			if(debug > 1) {
				describeAlternativePoints();
			}
			evalSet = alternativePoints;
		} else if(simplexState == SimplexState.ALT_POINTS) {
			++iteration;
			/* iteration counter */

			//Debug.write("In SimplexAlgorithm:nextSimplex: state is alternative points, iteration "+iteration, debug, 1);
			if(debug > 0) {
				describeAlternativePoints();
			}
			tryCost = alternativePoint[2].cost;
			/* reflection point */
 /* No need to order them, this will be done at top of loop */

 /* Two flavours of the method - the original Nelder/Mead and the modified which avoids infinite looping */
 /* Note how an American spell-checker cannot spell 'flavour' */
			if(originalMethod == true) /* Nelder and Mead */ {
				if(tryCost < simplexPoint[least].cost) {
					if(debug > 0) {
						Debug.write("Reflected point better than best", debug, 1);
					}
					//	if(debug)
					//		filesReportDebugMessage("Reflected point better than best\n", prefix);
					// That was very good, try point 3. 
					// If point's better than the worst, then replace the worst. 
					if(alternativePoint[3].cost < simplexPoint[least].cost) {
						//	if(debug)
						//		filesReportDebugMessage("Replacing worst point by stretch reflected point\n", prefix);

						simplexPoint[greatest].copyPoint(alternativePoint[3]);
						//utilsCopyPoint(alternativePoint[3], &(simplexPoint[greatest]), dimensionality);
					} else {
						//if(debug)
						//	filesReportDebugMessage("Replacing worst point by reflected point\n", prefix);
						//utilsCopyPoint(alternativePoint[2], &(simplexPoint[greatest]), dimensionality);
						simplexPoint[greatest].copyPoint(alternativePoint[2]);
					}

				} else if(tryCost > simplexPoint[next_greatest].cost) /* reflected point worse than second worst */ {
					//if(debug)
					//	filesReportDebugMessage("Reflected point worse than second worst\n", prefix);
					if(tryCost > simplexPoint[greatest].cost) /*	reflected point worse than worst */ {
						contracted_point = 0;			/* the point inside current simplex */
					} else {
						contracted_point = 1;			/* use contracted point outside current simplex */
					}

					if(alternativePoint[contracted_point].cost > simplexPoint[greatest].cost) /* new point not good */ {
						/* general contraction around best point */
 /* First assemble the points, */
						for(i = 0; i <= dimensionality; ++i) /* each vertex */ {
							for(j = 0; j < dimensionality; ++j) /* each component */ {
								simplexPoint[i].coords[j] = 0.5 * (simplexPoint[i].coords[j] + simplexPoint[least].coords[j]);

							}
							simplexPoint[i].evaluated = false;
						}
						simplexState = SimplexState.NEW_SIMPLEX;
						evalSet = simplexPoints;
						return;

						/* then evaluate the points. */
					} else {
						/* contracted point at least better than worst */
 /*if(debug)
						{
							if(contracted_point == 0)			
								filesReportDebugMessage("Replacing worst point by contraction point inside simplex\n", prefix);
							else if (contracted_point == 1)		
									filesReportDebugMessage("Replacing worst point by reflected contraction point\n", prefix);
			
						}
						 */
						//utilsCopyPoint(alternativePoint[contracted_point], &(simplexPoint[greatest]), dimensionality);
						simplexPoint[greatest].copyPoint(alternativePoint[contracted_point]);
					}
				} else {
					/* reflected point between best and second worst */
 /*if(debug)
					{
						filesReportDebugMessage("Reflected point between second worst and best\n", prefix);
						filesReportDebugMessage("Replacing worst point by reflected point\n", prefix);
					}
					 */
					//utilsCopyPoint(alternativePoint[2], &(simplexPoint[greatest]), dimensionality);
					simplexPoint[greatest].copyPoint(alternativePoint[2]);
				}
			} else if(originalMethod == false) /* modified by AL to prevent looping (this is default case) */ {
				if(tryCost < simplexPoint[least].cost) {
					if(debug > 0) {
						Debug.write("Reflected point better than best", debug, 1);
					}

					/* That was very good, try point 3. */
 /* If point's better than the worst, then replace the worst. */
					if(alternativePoint[3].cost < simplexPoint[least].cost) {
						if(debug > 0) {
							Debug.write("Replacing worst point by stretch reflected point", debug, 1);
						}
						//if(debug)
						//	filesReportDebugMessage("Replacing worst point by stretch reflected point\n", prefix);
						//utilsCopyPoint(alternativePoint[3], &(simplexPoint[greatest]), dimensionality);
						simplexPoint[greatest].copyPoint(alternativePoint[3]);
					} else {
						if(debug > 0) {
							Debug.write("Replacing worst point by reflected point", debug, 1);
						}
						//if(debug)
						//	filesReportDebugMessage("Replacing worst point by reflected point\n", prefix);
						//utilsCopyPoint(alternativePoint[2], &(simplexPoint[greatest]), dimensionality);
						simplexPoint[greatest].copyPoint(alternativePoint[2]);
					}

				} else if(tryCost >= simplexPoint[next_greatest].cost) /* reflected point worse than second worst or as bad */ {
					if(debug > 0) {
						Debug.write("Reflected point worse than second worst", debug, 1);
					}
					//if(debug)
					//	filesReportDebugMessage("Reflected point worse than second worst\n", prefix);
					if(tryCost >= simplexPoint[greatest].cost) /*	reflected point worse than worst or as bad */ {
						contracted_point = 0;			/* the point inside current simplex */
					} else {
						contracted_point = 1;			/* use contracted point outside current simplex */
					}

					if(alternativePoint[contracted_point].cost >= simplexPoint[greatest].cost) {		// new point not good 
						// general contraction around best point 
						// First assemble the points, 
						for(i = 0; i <= dimensionality; ++i) /* each vertex */ {
							for(j = 0; j < dimensionality; ++j) /* each component */ {
								simplexPoint[i].coords[j] = 0.5 * (simplexPoint[i].coords[j] + simplexPoint[least].coords[j]);

							}
							simplexPoint[i].evaluated = false;
						}
						simplexState = SimplexState.NEW_SIMPLEX;
						evalSet = simplexPoints;
						return;

					} else {
						/* contracted point at least better than worst */
						if(debug > 0) {
							if(contracted_point == 0) {
								Debug.write("Replacing worst point by contraction point inside simplex", debug, 1);
							} else if(contracted_point == 1) {
								Debug.write("Replacing worst point by reflected contraction point", debug, 1);
							}

						}

						//utilsCopyPoint(alternativePoint[contracted_point], &(simplexPoint[greatest]), dimensionality);
						simplexPoint[greatest].copyPoint(alternativePoint[contracted_point]);
					}
				} else {
					/* reflected point between best and second worst */
					if(debug > 0) {
						Debug.write("Reflected point between second worst and best", debug, 1);
						Debug.write("Replacing worst point by reflected point", debug, 1);
					}
					/*if(debug)
					{
						filesReportDebugMessage("Reflected point between second worst and best\n", prefix);
						filesReportDebugMessage("Replacing worst point by reflected point\n", prefix);
					}
					 */
					//utilsCopyPoint(alternativePoint[2], &(simplexPoint[greatest]), dimensionality);
					simplexPoint[greatest].copyPoint(alternativePoint[2]);
				}
			} /* end of alternative method*/ else {
				System.out.println("Error: method neither original nor modified");
			}
			if(simplexPoints.allPointsEvaluated()) {
				evaluated = true;
				orderVertices();
			} else {
				throw new NimrodOKException("Expecting all points of modified simplex to be evaluated");
			}

			//Debug.write("Simplex algorithm: checking convergence, conTest is "+ conTest, debug, 1);
			convergent = conTest.checkConvergence(simplexPoints);
			convergent = convergent || (iteration == maxIters);
			//Debug.write("Simplex algorithm: iteration is " + iteration +  " maxIters is " +maxIters, debug, 2);
			if(convergent) {
				//Debug.write("Simplex algorithm: checking convergence, convergence is true", debug, 1);
				resultPoint = new OptimPoint(simplexPoint[least]);
				return;
			}

			if(debug > 0) {
				Debug.write("creating alternative points");
			}
			generateAlternativePoints();

			// simplexState remains at SimplexState.ALT_POINTS)
		} // end of (state == State.ALT_POINTS)
		else {
			Debug.write("Error: neither NEW_SIMPLX nor ALT_POINTS");
		}

		//Debug.write("In SimplexAlgorithm:nextSimplex complete: simplexState is "+simplexState, debug, 1);
	}	// end method

	public void orderVertices() {

		least = 0;

		// determine the best, worst and second worst vertices
		if(simplexPoint[0].cost > simplexPoint[1].cost) {
			greatest = 0;
			next_greatest = 1;
		} else {
			greatest = 1;
			next_greatest = 0;
		}
		for(int i = 1; i <= dimensionality; ++i) /* each vertex */ {
			if(simplexPoint[i].cost <= simplexPoint[least].cost) {
				least = i;
			}
			if(simplexPoint[i].cost > simplexPoint[greatest].cost) {
				next_greatest = greatest;
				greatest = i;
			} else if(simplexPoint[i].cost > simplexPoint[next_greatest].cost) {
				if(i != greatest) {
					next_greatest = i;
				}
			}
		}
		ordered = true;
		//describeSimplex("Current simplex is", 1);

		bestObjective = simplexPoint[least].cost;
		worstObjective = simplexPoint[greatest].cost;
	}

	public void generateAlternativePoints() {

		// generate alternative points
		FindFaceCentre(greatest);
		//simplexPoint[greatest].describePoint("generateAlternativePoints: using worst point ", 0);		
		FindAlternativePoint(simplexPoint[greatest], 0.5, alternativePoint[0]);
		FindAlternativePoint(simplexPoint[greatest], 1.5, alternativePoint[1]);
		FindAlternativePoint(simplexPoint[greatest], 2.0, alternativePoint[2]);
		FindAlternativePoint(simplexPoint[greatest], 3.0, alternativePoint[3]);
	}

	public void describeSimplex(String str) {
		String desigStr;

		Debug.write(str);
		for(int i = 0; i <= dimensionality; ++i) {
			simplexPoint[i].describePoint(" ");
			desigStr = "\t not evaluated";
			if(simplexPoint[i].evaluated) {
				desigStr = "\t cost: " + simplexPoint[i].cost;

				if(ordered) {
					if(i == least) {
						desigStr = desigStr + " best point";
					} else if(i == greatest) {
						desigStr = desigStr + " worst point";
					} else if(i == next_greatest) {
						desigStr = desigStr + " second worst point";
					}
				}

			}

			Debug.write(desigStr);

		}

	}

	public void describeSimplex(String str, int levelRequired) {
		if(debug >= levelRequired) {
			describeSimplex(str);
		}
	}

	public void describeAlternativePoints() {
		System.out.printf("Alternative points:\n%s", alternativePoints.describePoints());
	}

	public boolean checkConvergence(int flag) {
		ArrayOfPoints designatedPointSet;
		OptimPoint[] pointArray;
		double max, min;
		double criterion = 0.0;

		designatedPointSet = simplexPoints;
		pointArray = designatedPointSet.pointArray;

		if(!designatedPointSet.allPointsEvaluated()) {
			return (false);
		}

		max = pointArray[0].cost;
		min = pointArray[0].cost;
		for(int i = 1; i < designatedPointSet.numPoints; ++i) {
			if(pointArray[i].cost > max) {
				max = pointArray[i].cost;
			} else if(pointArray[i].cost < min) {
				min = pointArray[i].cost;
			}
		}

		if(flag == 0) {
			criterion = max - min;
		} else if(flag == 1) {
			criterion = 2.0 * (max - min) / (Math.abs(max) + Math.abs(min) + EPSILON);
		}
		if(criterion < tolerance) {
			return (true);
		} else {
			return (false);
		}
	}

	// *********  private
	public enum SimplexState {
		NEW_SIMPLEX,
		ALT_POINTS,
	}

	public SimplexAlgorithm.SimplexState showState() {
		return (simplexState);

	}

	private void FindFaceCentre(int worst) {
		/* computes coords of point on face opposite worst vertex */

		double pntSum;
		/* sum over vertices of the ith coords */
		int i, j;
		//double[] face_centre;

		//face_centre = new double[dimensionality];
		for(i = 0; i < dimensionality; ++i) /* each dimension */ {
			pntSum = 0.0;
			for(j = 0; j <= dimensionality; ++j) /* each vertex */ {
				pntSum += simplexPoint[j].coords[i];
			}

			faceCentre.coords[i] = (pntSum - simplexPoint[worst].coords[i]) / dimensionality;
			//Debug.write("FindFaceCentre: coord "+i+" is "+faceCentre.coords[i]);		
		}

	}

	private void FindAlternativePoint(OptimPoint worstPoint, double parCoord, OptimPoint newPoint) {
		/* Computes point 'result' as the linear combination
		parCoord*face_centre + (1-parCoord)*worst_point
		so that par_coord parametrises the line from the worst point to the face centre */

		int j;

		//newPoint = new OptimPoint(worstPoint) ;
		for(j = 0; j < dimensionality; ++j) /* each dimension */ {
			newPoint.coords[j] = parCoord * faceCentre.coords[j] + (1.0 - parCoord) * worstPoint.coords[j];
		}

		newPoint.confineToDomain();
		newPoint.evaluated = false;

	}

	private double proportionalDifference(double first_value, double second_value) {
		return (2.0 * Math.abs(second_value - first_value) / (Math.abs(first_value) + Math.abs(second_value) + EPSILON));
	}

	public boolean ProportionalDifferenceIsSmall() {
		double prop_change;

		prop_change = proportionalDifference(bestObjective, worstObjective);
		//Debug.write("The prop difference is "+prop_change);

		if(prop_change <= tolerance) {
			return (true);
		} else {
			return (false);
		}

	}

}

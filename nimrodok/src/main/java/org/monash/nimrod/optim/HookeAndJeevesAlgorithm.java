package org.monash.nimrod.optim;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import java.util.*;

/* Hooke and Jeeves
 * Note that the algorithm differs from the published one in two ways:
 * (i) H&J use the same step size for each coordinate, which is rather 
 * silly if the coords differ in ranges. So here the increment for each coord.
 * is the current step size multiplied by the range for that coord.
 * (ii) In the H&J exploratory phase
 */
public class HookeAndJeevesAlgorithm extends OptimizationAlgorithm {

	int dimensionality;
	public int iteration;

	public int maxIters;
	public double tolerance;

	OptimPoint current_base_point;
	OptimPoint prev_base_point;
	OptimPoint new_base_point;

	private double step_size;
	double[] increment;
	double step_size_factor;

	public ArrayOfPoints exploratoryPoints;
	public OptimPoint[] exploratory_point;
	public int pointsEvaluated;

	int algorithmState;	// same state labels as described in H&J original paper

	// settings for optimization method
	public int optimIndex;

	public boolean convergent = false;

	ArrayOfPoints resultPoints;
	public OptimPoint[] resultPoint;

	public int jobCount;
	public int jobRequestsCount;
	public int batchRequestsCount;
	public int batchCount;
	private Map<String, Integer> jobMap;

	public HookeAndJeevesAlgorithm(int maxIters, double tolerance, OptimPoint startingPoint) {
		super(startingPoint);

		this.maxIters = maxIters;
		this.tolerance = tolerance;

		//Debug.write("In HookeAndJeevesAlgorithm:constructor: Initialize");
		dimensionality = startingPoint.dimensionality;
		//Debug.write("In HookeAndJeevesAlgorithm:constructor: dimensionality = "+dimensionality);
		int count = 1;
		for(int i = 0; i < dimensionality; ++i) {
			count *= 3;
		}
		exploratoryPoints = new ArrayOfPoints(startingPoint, count);
		exploratory_point = exploratoryPoints.pointArray;
		//Debug.write("In HookeAndJeevesAlgorithm:constructor: Initialize: will use "+ count+" exploratory points");

		current_base_point = new OptimPoint(startingPoint);
		prev_base_point = new OptimPoint(startingPoint);
		new_base_point = new OptimPoint(startingPoint);

		iteration = 0;
		step_size_factor = 0.5;
		step_size = 0.1;
		increment = new double[dimensionality];

		ComputeInitialIncrements();
		//Debug.showDoubleArray("In HookeAndJeevesAlgorithm:constructor: Initialize: initial increments are ", increment, dimensionality);

		algorithmState = 1;
		jobMap = new HashMap<String, Integer>();
		jobMap.clear();
		jobCount = 0;
		jobRequestsCount = 0;
		batchRequestsCount = 0;
		batchCount = 0;

	}

	public double getStepSize() {
		return step_size;
	}

	public void storeJobs(ArrayOfPoints array) {
		Integer jobNumber;

		OptimPoint pt;
		++batchRequestsCount;

		boolean found = false;
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

	public void generateNextEvalPoints() {
		//Debug.write("In HookeAndJeevesAlgorithm: generateNextEvalPoints state " + algorithmState);
		//current_base_point.describePoint("\t Current base point is ", 0);
		//prev_base_point.describePoint("\t Previous base point is ", 0);
		//new_base_point.describePoint("\t New base point is ", 0);

		if(algorithmState == 1) {
			++iteration;
			ExploratoryMoves();

		} else if(algorithmState == 2) {
			prev_base_point.copyPoint(current_base_point);
			current_base_point.copyPoint(new_base_point);
			PatternMoves();
			//current_base_point.describePoint("\t After pattern move, Current base point is ", 0);
			//prev_base_point.describePoint("\t Previous base point is ", 0);
			//new_base_point.describePoint("\t New base point is ", 0);
			ExploratoryMoves();

		}

	}

	private int ComputeInitialIncrements() {
		int i;

		for(i = 0; i < dimensionality; ++i) {
			increment[i] = step_size * (startingPoint.range[i]);
		}
		return (0);

	}

	private int adjustIncrements() {
		int i;

		for(i = 0; i < dimensionality; ++i) {
			increment[i] *= step_size_factor;
		}
		step_size *= step_size_factor;
		return (0);

	}

	private int pointCount;

	public void ExploratoryMoves() {
		//Debug.showDoubleArray("In HookeAndJeevesAlgorithm: ExploratoryMoves with increments  ", increment, dimensionality);
		OptimPoint target;

		//int i,j;
		target = new OptimPoint(new_base_point);
		target.copyPoint(new_base_point);
		//exploratory_point[0].copyPoint(target);

		pointCount = 0;
		recursivelyGeneratePoints(target, 0);

		//exploratoryPoints.describePoints("ExploratoryMoves ", 0);
	}

	private void recursivelyGeneratePoints(OptimPoint target, int index) {
		//Debug.write("In HookeAndJeevesAlgorithm: recursivelyGeneratePoints index is "+index, 2);
		if(index == dimensionality) {
			exploratory_point[pointCount].copyPoint(target);
			exploratory_point[pointCount].evaluated = false;
			++pointCount;
		} else {
			target.coords[index] = new_base_point.coords[index];
			recursivelyGeneratePoints(target, index + 1);

			target.coords[index] = new_base_point.coords[index] + increment[index];
			if(target.coords[index] > startingPoint.max[index]) {
				target.coords[index] = startingPoint.max[index];
			}

			recursivelyGeneratePoints(target, index + 1);

			target.coords[index] = new_base_point.coords[index] - increment[index];
			if(target.coords[index] < startingPoint.min[index]) {
				target.coords[index] = startingPoint.min[index];
			}

			recursivelyGeneratePoints(target, index + 1);
		}

	}

	public void PatternMoves() {
		int i;

		for(i = 0; i < dimensionality; ++i) {
			new_base_point.adjustCoordinate(i, 2 * current_base_point.coords[i] - prev_base_point.coords[i]);
			//new_base_point.coords[i] = 2*current_base_point.coords[i] - prev_base_point.coords[i];
		}
		new_base_point.confineToDomain();

	}

	public int determineBest(ArrayOfPoints aop) {
		double bestCost;

		bestCost = aop.pointArray[0].cost;
		int bestIndex = 0;

		for(int j = 1; j < aop.numPoints; ++j) {
			if(aop.pointArray[j].cost < bestCost) {
				bestCost = aop.pointArray[j].cost;
				bestIndex = j;
			}
		}

		return bestIndex;
	}

	public void processresults() {
		//exploratoryPoints.describePoints("ExploratoryMoves ", 0);

		int bestIndex = determineBest(exploratoryPoints);
		new_base_point.copyPoint(exploratory_point[bestIndex]);

		if(algorithmState == 1) {
			if(bestIndex == 0) // no improvement
			{
				algorithmState = 3;	// not used, just to agree with published algorithm
				adjustIncrements();
				algorithmState = 1;

			} else {
				algorithmState = 2;
			}

		} else if(algorithmState == 2) {
			if(new_base_point.cost < current_base_point.cost) {
				algorithmState = 2;
			} else // no improvement
			{
				algorithmState = 1;
				new_base_point.copyPoint(current_base_point);
			}

		}
		//System.out.println("H&J algorithm: checking convergence, conTest is "+ conTest);
		//System.out.println("H&J algorithm: iteration is "+ iteration+", max iterations is "+maxIters);

		convergent = (step_size < tolerance) || (iteration == maxIters);
		if(convergent) {
			resultPoints = new ArrayOfPoints(current_base_point, 1);
			resultPoint = resultPoints.pointArray;
			resultPoint[0].copyPoint(current_base_point);
		}

	}

}

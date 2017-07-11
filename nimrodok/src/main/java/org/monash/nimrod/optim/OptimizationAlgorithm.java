package org.monash.nimrod.optim;

import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;

public class OptimizationAlgorithm {

	public SetOfParams setOfParams;
	// TODO: Make startingPoint final
	public OptimPoint startingPoint;	// starting point for this algorithm
	public OptimPoint origStartingPoint;	// starting point for this chain of algorithms

	// TODO: Remove this
	protected OptimizationAlgorithm() {

	}

	protected OptimizationAlgorithm(OptimPoint startingPoint) {
		this.startingPoint = startingPoint;
	}

}

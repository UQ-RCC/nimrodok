package org.monash.nimrod.optim.ganesh;

import au.edu.uq.rcc.nimrod.optim.Configuration;
import au.edu.uq.rcc.nimrod.optim.IAlgorithmDefinition;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PopulationBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Properties;
import au.edu.uq.rcc.nimrod.optim.TrajectoryBaseAlgorithm;

public class GaneshDefinition implements IAlgorithmDefinition {

	private static final int DEFAULT_GENERATIONS = 500;
	private static final float DEFAULT_MUTATION_PROB = 0.333300f;
	private static final float DEFAULT_CROSSOVER_PROB = 0.950000f;
	private static final int DEFAULT_MEM_UPPER_THRESSHOLD = 50000;
	private static final int DEFAULT_NUM_DUPLICATES = Integer.MAX_VALUE;
	private static final float DEFAULT_CROSSOVER_POLY_INDEX = 20.000f;
	private static final float DEFAULT_MUTATION_POLY_INDEX = 20.000f;
	private static final boolean DEFAULT_RUN_TILL_ALL_RANK_ONE = false;
	private static final int DEFAULT_RUN_TILL_PASS_MINUTES = 10000;
	private static final boolean DEFAULT_NSGA2_OP_ORDER = false;

	private static final Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		PROPERTIES.setProperty("ga.max_gens", Integer.toString(DEFAULT_GENERATIONS));
		PROPERTIES.setProperty("ga.mutation_prob", Float.toString(DEFAULT_MUTATION_PROB));
		PROPERTIES.setProperty("ga.crossover_prob", Float.toString(DEFAULT_CROSSOVER_PROB));
		PROPERTIES.setProperty("ga.mem_upper_threshold", Integer.toString(DEFAULT_MEM_UPPER_THRESSHOLD));
		PROPERTIES.setProperty("ga.num_duplicates", Integer.toString(DEFAULT_NUM_DUPLICATES));
		PROPERTIES.setProperty("ga.crossover_poly_index", Float.toString(DEFAULT_CROSSOVER_POLY_INDEX));
		PROPERTIES.setProperty("ga.mutation_poly_index", Float.toString(DEFAULT_MUTATION_POLY_INDEX));
		PROPERTIES.setProperty("ga.run_till_all_rank1", Boolean.toString(DEFAULT_RUN_TILL_ALL_RANK_ONE));
		PROPERTIES.setProperty("ga.initrun_till_pass_minutes_pop", Integer.toString(DEFAULT_RUN_TILL_PASS_MINUTES));
		PROPERTIES.setProperty("ga.operator_order_as_nsga2", Boolean.toString(DEFAULT_NSGA2_OP_ORDER));

	}

	@Override
	public String getUniqueName() {
		return "GANESH";
	}

	@Override
	public String getPrettyName() {
		return "Ganesh (GA)";
	}

	@Override
	public PopulationBaseAlgorithm createPopulationInstance(OptimPoint[] initialPopulation, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
		try {
			return new GaneshImpl(initialPopulation, config, receiver);
		} catch(Throwable e) {
			throw new NimrodOKException(e.getMessage());
		}
	}

	@Override
	public TrajectoryBaseAlgorithm createTrajectoryInstance(OptimPoint startingPoint, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getSupportedObjectiveCount() {
		return Integer.MAX_VALUE;
	}

	@Override
	public Properties getDefaultProperties() {
		return PROPERTIES;
	}

	@Override
	public boolean isTrajectoryBased() {
		return false;
	}

	@Override
	public boolean isPopulationBased() {
		return true;
	}
}

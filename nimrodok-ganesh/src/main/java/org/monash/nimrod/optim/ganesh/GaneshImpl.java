package org.monash.nimrod.optim.ganesh;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.Batch;
import au.edu.uq.rcc.nimrod.optim.Configuration;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimParameter;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.PopulationBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Properties;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import java.util.List;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import org.ganesh.core.ChromosomeFactory;
import org.ganesh.core.ExnGAChained;
import org.ganesh.core.ObjFunc;
import org.ganesh.core.Variable;
import org.ganesh.core.Range;
import org.ganesh.core.VariableDouble;
import org.ganesh.core.PhenotypeList;
import org.ganesh.core.Report;
import org.ganesh.plugin.Experiment;

public class GaneshImpl extends PopulationBaseAlgorithm {

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

	private final int maxGenerations;
	private final float mutationProb;
	private final float crossoverProb;
	private final int memUT;//for garbage collection
	private final int numDupsAllowed;
	private final float crossoverPolyIndex;
	private final float mutationPolyIndex;
	private final boolean runTillAllRank1;
	private final int runTillPassMinutes;
	private final boolean nsga2OpOrder;

	public final GaneshGA gaAlg;

	public GaneshImpl(OptimPoint[] start, Configuration config, INimrodReceiver logger)
			throws NimrodOKException, ExnGAChained, ClassNotFoundException, IOException {
		super(start, config, logger);
		Properties props = config.customProperties();
		maxGenerations = parseMaxGens(props);
		mutationProb = parseMutationProb(props);
		crossoverProb = parseCrossoverProb(props);
		memUT = parseMemUT(props);
		numDupsAllowed = parseNumDupsAllowed(props);
		crossoverPolyIndex = parseCrossoverPolyIndex(props);
		mutationPolyIndex = parseMutationPolyIndex(props);
		runTillAllRank1 = parseRunTillAllRank1(props);
		runTillPassMinutes = parseRunTillPassMinutes(props);
		nsga2OpOrder = parseNsga2OpOrder(props);

		logger.logf(this, "GaneshImpl: \n"
				+ "maxGen=%d, initPop=%d , mutationprob=%f, crossoverprob=%f\n"
				+ "mem_upper_threshold=%d, num_dups_allowed=%d, crossover_poly_index=%f\n"
				+ "mutation_poly_index=%f, run_till_all_rank_one=%b, run_till_pass_minute=%d",
				this.maxGenerations, start.length, this.mutationProb, this.crossoverProb,
				this.memUT, this.numDupsAllowed, this.crossoverPolyIndex,
				this.mutationPolyIndex, this.runTillAllRank1, this.runTillPassMinutes);
		//create ganesh report
		Report _report = new Report();
		//create ganesh exp
		Experiment _experiment = new GaneshExperiment(this);
		this.gaAlg = new GaneshGA(_experiment.getExperimentPvt(), _report, this.maxGenerations,
				start.length, this.mutationProb, this.crossoverProb,
				this.memUT, this.numDupsAllowed, this.crossoverPolyIndex,
				this.mutationPolyIndex, this.runTillAllRank1, this.runTillPassMinutes, false,
				this.nsga2OpOrder, logger, this);
		this.gaAlg.conTest = config.convergenceSettings();
		this.gaAlg.setOfParams = start[0].setOfParams;
	}

	private boolean parseNsga2OpOrder(Properties props) throws NimrodOKException {
		String _nsga2OpOrderStr = props.getProperty("ga.operator_order_as_nsga2");
		if(_nsga2OpOrderStr == null) {
			return DEFAULT_NSGA2_OP_ORDER;
		}

		boolean _nsga2OpOrder;
		try {
			_nsga2OpOrder = Boolean.parseBoolean(_nsga2OpOrderStr);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}
		return _nsga2OpOrder;
	}

	/**
	 * Maximum number of generations
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private static int parseMaxGens(Properties props) throws NimrodOKException {
		String _maxGens = props.getProperty("ga.max_gens");
		if(_maxGens == null) {
			return DEFAULT_GENERATIONS;
		}

		int iters;
		try {
			iters = Integer.parseInt(_maxGens);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(iters < 1) {
			throw new NimrodOKException("Max generations must be >= 1");
		}
		return iters;
	}

	/**
	 * RunTillPassMinutes
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private int parseRunTillPassMinutes(Properties props) throws NimrodOKException {
		String _minutesStr = props.getProperty("ga.run_till_pass_minutes");
		if(_minutesStr == null) {
			return DEFAULT_RUN_TILL_PASS_MINUTES;
		}

		int mins;
		try {
			mins = Integer.parseInt(_minutesStr);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(mins < 1) {
			throw new NimrodOKException("Minutes must be >= 1");
		}
		return mins;
	}

	/**
	 * RunTillAllRank1
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private boolean parseRunTillAllRank1(Properties props) throws NimrodOKException {
		String _rank1Str = props.getProperty("ga.run_till_all_rank1");
		if(_rank1Str == null) {
			return DEFAULT_RUN_TILL_ALL_RANK_ONE;
		}

		boolean runTillRank1;
		try {
			runTillRank1 = Boolean.parseBoolean(_rank1Str);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}
		return runTillRank1;
	}

	/**
	 * parse ga.mutation_poly_index
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private float parseMutationPolyIndex(Properties props) throws NimrodOKException {
		String _mutationPolyIndexStr = props.getProperty("ga.mutation_poly_index");
		if(_mutationPolyIndexStr == null) {
			return DEFAULT_MUTATION_POLY_INDEX;
		}

		float _mutPolyIndex;
		try {
			_mutPolyIndex = Float.parseFloat(_mutationPolyIndexStr);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(_mutPolyIndex < 0) {
			throw new NimrodOKException("Mutation Poly Index must be >= 0");
		}
		return _mutPolyIndex;
	}

	/**
	 * parse ga.crossover_poly_index
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private float parseCrossoverPolyIndex(Properties props) throws NimrodOKException {
		String _crossover_poly_index = props.getProperty("ga.crossover_poly_index");
		if(_crossover_poly_index == null) {
			return DEFAULT_CROSSOVER_POLY_INDEX;
		}

		float _crossoverPolyIndex;
		try {
			_crossoverPolyIndex = Float.parseFloat(_crossover_poly_index);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(_crossoverPolyIndex < 0) {
			throw new NimrodOKException("Crossover Ploy Index must be >= 0");
		}
		return _crossoverPolyIndex;
	}

	/**
	 * parse number of duplicates allowed
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private int parseNumDupsAllowed(Properties props) throws NimrodOKException {
		String _numDupsStr = props.getProperty("ga.num_duplicates");
		if(_numDupsStr == null) {
			return DEFAULT_NUM_DUPLICATES;
		}

		int dups;
		try {
			dups = Integer.parseInt(_numDupsStr);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(dups < 0) {
			throw new NimrodOKException("Number of duplicates must be >= 0");
		}
		return dups;
	}

	/**
	 * parse upper threshold for memory
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private int parseMemUT(Properties props) throws NimrodOKException {
		String _upperThresholdMem = props.getProperty("ga.mem_upper_threshold");
		if(_upperThresholdMem == null) {
			return DEFAULT_MEM_UPPER_THRESSHOLD;
		}

		int mem;
		try {
			mem = Integer.parseInt(_upperThresholdMem);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(mem < 0) {
			throw new NimrodOKException("Mem upper threshold must be >= 0");
		}
		return mem;
	}

	/**
	 * parse ga.crossover_prob
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private float parseCrossoverProb(Properties props) throws NimrodOKException {
		String _crossOverProbStr = props.getProperty("ga.crossover_prob");
		if(_crossOverProbStr == null) {
			return DEFAULT_CROSSOVER_PROB;
		}

		float _crossOverP;
		try {
			_crossOverP = Float.parseFloat(_crossOverProbStr);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(_crossOverP < 0) {
			throw new NimrodOKException("Crossover probability must be >= 0");
		}
		return _crossOverP;
	}

	/**
	 * parse ga.mutation_prob
	 *
	 * @param props
	 * @return
	 * @throws NimrodOKException
	 */
	private float parseMutationProb(Properties props) throws NimrodOKException {
		String _mutationProbStr = props.getProperty("ga.mutation_prob");
		if(_mutationProbStr == null) {
			return DEFAULT_MUTATION_PROB;
		}

		float _mutationP;
		try {
			_mutationP = Float.parseFloat(_mutationProbStr);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(_mutationP < 0) {
			throw new NimrodOKException("Mutation probability must be >= 0");
		}
		return _mutationP;
	}

	@Override
	public void fire() throws NimrodOKException {
		receiver.logf(this, "GaneshImpl: Called with state %s", m_State);
		if(m_State == State.STOPPED) {
			m_State = State.WAITING_FOR_BATCH;
			try {
				gaAlg.init();
			} catch(ExnGAChained | ClassNotFoundException | IOException e) {
				throw new NimrodOKException("[GaneshImpl::fire - ga init] Error:" + e.getMessage());
			}
			m_CurrentBatch = evalBatch(gaAlg.evalSet);
		} else if(m_State == State.WAITING_FOR_BATCH) {
			/* If the batch hasn't finished, why have we been called? */
			if(!m_CurrentBatch.isFinished()) {
				receiver.logf(this, "GaneshImpl: Batch not finished, why were we called?");
				return;
			}
			/*change to RUNNING*/
			m_State = State.RUNNING;

			//call GaneshGA for next iteration here
			try {
				gaAlg.nextIteration();
			} catch(Exception e) {
				throw new NimrodOKException("[GaneshImpl::fire- ga nextIteration] Error:" + e.getMessage());
			}
			//only clear current batch after calling GaneshGA execute function
			m_CurrentBatch = null;

			if(!gaAlg.stillContinue) {
				receiver.logf(this, "In GaneshImpl mode 2: GA has completed");
				m_State = State.FINISHED;

				for(PointContainer _p : gaAlg.getParetoPoints()) {
					m_ParetoFront.add(_p);
				}
				m_State = State.FINISHED;
			} else {
				receiver.logf(this, "In GaneshImpl mode 2: GA has NOT completed. Continue running!!!");
				m_CurrentBatch = evalBatch(gaAlg.evalSet);
				m_State = State.WAITING_FOR_BATCH;
			}
		} else if(m_State == State.RUNNING) {
			/* Should never happen */
		} else if(m_State == State.FINISHED) {
			/* We're done, why are you calling us again? */
		}
	}

	private Batch evalBatch(ArrayOfPoints exploratoryPoints) throws NimrodOKException {
		gaAlg.pointsEvaluated = 0;
		gaAlg.storeJobs(exploratoryPoints);

		PointContainer[] currentBatch = new PointContainer[exploratoryPoints.pointArray.length];
		for(int i = 0; i < currentBatch.length; ++i) {
			currentBatch[i] = new PointContainer(exploratoryPoints.pointArray[i], config.objectiveCount());
		}

		return createBatch(currentBatch);
	}

	/**
	 * This GaneshExperiment class is created to overcome the abstract of Experiment class
	 *
	 * @author hoangnguyen
	 *
	 */
	private class GaneshExperiment extends Experiment {

		public GaneshImpl ganesh;

		public GaneshExperiment(GaneshImpl _ganesh) {
			super();
			ganesh = _ganesh;
			//add list var
			List<Variable> _listVars = new LinkedList<Variable>();
			SetOfParams _setOfParams = ganesh.startingPoints.pointArray[0].setOfParams;
			for(int i = 0; i < _setOfParams.size(); i++) {
				Variable _variable = null;
				OptimParameter _param = _setOfParams.get(i);
				//if(_setOfParams.paramIsInt(i)) {
				//	_variable = new VariableInt(new Range(_param.fromInt, _param.toInt));
				//} else if(_setOfParams.paramIsFloat(i)) {
				_variable = new VariableDouble(new Range(_param.min, _param.max));
				//}
				_listVars.add(_variable);
			}
			setListVars(_listVars);
			//list objective function
			List<ObjFunc> _listObjectives = new LinkedList<ObjFunc>();
			for(int i = 0; i < config.objectiveCount(); i++) {
				_listObjectives.add(new GaneshObjFunc(ganesh, i));
			}
			setListObjFuncs(_listObjectives);
			setChromosomeFactory(ChromosomeFactory.MIXED);
			setPopulationInitialiser(new GaneshPopulationInitialiser(this.getExperimentPvt(), ganesh.startingPoints));
		}
	}//GaneshExperiment

	/**
	 * class GaneshObjFunc
	 *
	 * @author hoangnguyen
	 *
	 */
	private class GaneshObjFunc extends ObjFunc {

		private GaneshImpl ganesh;
		private int index;

		public GaneshObjFunc(GaneshImpl _ganesh, int ind) {
			ganesh = _ganesh;
			index = ind;
		}

		@Override
		public double getObjFunc(PhenotypeList pList) throws Exception {
			int n = pList.size(); // number of genes = problem variables
			double[] inParams = new double[n];
			for(int j = 0; j < n; j++) {
				inParams[j] = pList.get(j).doubleValue();
			}
			//find in the current batch
			if(ganesh.m_CurrentBatch.isFinished()) {
				double result = 0;
				boolean found = false;
				for(PointContainer _point : ganesh.m_CurrentBatch.points) {
					if(Arrays.equals(inParams, _point.point.coords)) {
						result = _point.objectives[index];
						found = true;
					}
				}
				if(found) {
					return result;
				} else {
					throw new Exception("[GaneshExperiment] Cannot find the Phenotype in current batch");
				}
			} else {
				throw new Exception("[GaneshExperiment] Batch needs to be finished before getting the objectives");
			}
		}
	}//GaneshObjFunc

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
}

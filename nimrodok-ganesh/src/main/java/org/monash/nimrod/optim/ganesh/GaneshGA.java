package org.monash.nimrod.optim.ganesh;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.ConvergenceTest;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.PopulationBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import java.io.IOException;
//java
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

//ganesh
import org.ganesh.core.Report;
import org.ganesh.core.Universe;
import org.ganesh.plugin.ExperimentPvt;
import utils.Utils.MemoryInfo;
import utils.Utils.TimeUtils;
import org.ganesh.core.EvalFile;
import org.ganesh.core.ExnGAChained;
import org.ganesh.core.Organism;
import org.ganesh.core.Population;
import org.ganesh.core.Gene;
import org.ganesh.core.GeneReal;
import org.ganesh.core.GeneLong;
import org.ganesh.core.ObjFunc;

/**
 *
 * Author: Hoang nguyen
 *
 *
 */
public class GaneshGA extends Universe {
	//--------------------------Variables from Nimrod----------------------------------------------

	public ArrayOfPoints evalSet;
	public SetOfParams setOfParams;

	public int pointsEvaluated; //used by GAOptimActor
	private int genCount;
	private EvalFile evalFile;
	private boolean exitByOFVal = false;
	//private int 				optimIndex;
	public boolean stillContinue = true;

	//GA properties
	private long startTime;
	private long nowTime;
	private MemoryInfo mem;
	public int memShowGen = 10; // show every n generations
	public int memShowCnt = 1;
	//GA keeps pareto front for each generation
	private final List<PointContainer> paretoPoints;

	private boolean isFirstIteration = true;

	//-------used for execution statistics purposes-----------
	private int jobCount;
	private int jobRequestsCount;
	private int batchRequestsCount;
	private int batchCount;
	private Map<String, Integer> jobMap;
	//--------------------------------------------------------

	public String errorMessage;
	public ConvergenceTest conTest;

	//------logging
	private INimrodReceiver logger;
	private PopulationBaseAlgorithm nimrodAlgorithm;

	//------------------------------------------------------------------------------------------------------------------
	/**
	 * Create new ganesh
	 *
	 * @param inExpp
	 * @param inRep
	 * @param maxGenerations
	 * @param initPop
	 * @param mutationProb
	 * @param crossoverProb
	 * @param memUT
	 * @param numDupsAllowed
	 * @param crossoverPolyIndex
	 * @param mutationPolyIndex
	 * @param runTillAllRank1
	 * @param runTillPassMinutes
	 * @param inNsga2OpOrder
	 * @param logger
	 * @param algo
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws ExnGAChained
	 */
	public GaneshGA(ExperimentPvt inExpp, Report inRep, int maxGenerations, int initPop,
			float mutationProb, float crossoverProb, long memUT,
			int numDupsAllowed, float crossoverPolyIndex, float mutationPolyIndex,
			boolean runTillAllRank1, long runTillPassMinutes, boolean inNsga2OpOrder, boolean exitByOFVal,
			INimrodReceiver logger, PopulationBaseAlgorithm algo) {
		super(inExpp, inRep, initPop, maxGenerations, mutationProb, crossoverProb, memUT,
				numDupsAllowed, runTillAllRank1, runTillPassMinutes, inNsga2OpOrder);
		this.exitByOFVal = exitByOFVal;
		parentPop = new Population(this, popSize, prbMutation, prbCrossover);
		childPop = new Population(this, 0, prbMutation, prbCrossover); // 0 first time
		evalFile = new EvalFile(this);
		this.logger = logger;
		this.nimrodAlgorithm = algo;
		//init job related 
		jobMap = new HashMap<String, Integer>();
		jobMap.clear();
		jobCount = 0;
		jobRequestsCount = 0;
		batchRequestsCount = 0;
		batchCount = 0;
		genCount = 0;
		mem = new MemoryInfo();
		paretoPoints = new LinkedList<PointContainer>();
		this.evalSet = algo.startingPoints;
	}

	/**
	 * init the GA
	 *
	 * @throws ClassNotFoundException
	 * @throws ExnGAChained
	 * @throws IOException
	 */
	public void init() throws ExnGAChained, ClassNotFoundException, IOException {
		startTime = TimeUtils.getTime();
		mem.showMemory(); // info only
		//init the population
		parentPop.populate(); // Create an initial population
		evalFile.writePop(parentPop); // save the pop in eval file
		//put jobs into evalSet
		this.fillUpEvalSet(parentPop);
		this.isFirstIteration = true;
		this.stillContinue = true;
	}

	/**
	 * store jobs
	 *
	 * @param exploratoryPoints
	 */
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

	/**
	 * fillup the evalSet
	 *
	 * @param pop
	 */
	private void fillUpEvalSet(Population population) {
		ArrayOfPoints ap = new ArrayOfPoints(evalSet.pointArray[0], population.getSize());
		//for each candidate solution
		Iterator<Organism> _popIterator = population.getPopIterator();
		int index = 0;
		while(_popIterator.hasNext()) {
			OptimPoint _op = new OptimPoint(evalSet.pointArray[0]);
			Organism _org = _popIterator.next();
			Gene[] _genotype = _org.getChromosome().getGenotype();
			for(int i = 0; i < _genotype.length; i++) {
				Gene _gene = _genotype[i];
				if(_gene instanceof GeneLong) {
					GeneLong _gL = (GeneLong)_gene;
					_op.coords[i] = _gL.getValue();
				} else if(_gene instanceof GeneReal) {
					GeneReal _gD = (GeneReal)_gene;
					_op.coords[i] = _gD.getValue();
				}
			}
			ap.pointArray[index] = _op;
			index++;
		}
		evalSet = ap;
	}

	/**
	 * move to next iteration
	 *
	 * @param ef
	 * @param efg
	 * @param noWriteText
	 * @param exitByOFVal
	 * @throws Exception
	 */
	public void nextIteration() throws Exception {
		logger.logf(this.nimrodAlgorithm, "Generation = %d    %s", genCount, TimeUtils.getTimeStringFull1());
		boolean allR1 = false;
		Iterator<Organism> _popIterator = null;
		if(this.isFirstIteration) {
			parentPop.evaluate(evalFile);
			this.isFirstIteration = false;
			_popIterator = parentPop.getPopIterator();
		} else {
			childPop.evaluate(evalFile); // Fitnesses & constraints evaluation
			_popIterator = childPop.getPopIterator();
			logger.logf(this.nimrodAlgorithm, "Done non-first generation evaluation");
		}

		//get pareto for this generation
		//clear from previous iteration
		this.paretoPoints.clear();

		int _numObjectives = this.getExperimentPvt().getListObjFuncs().size();
		while(_popIterator.hasNext()) {
			Organism _org = _popIterator.next();
			logger.logf(this.nimrodAlgorithm, "organism: " + _org.getRank() + ":::" + _org.getOFCount());
			logger.logf(this.nimrodAlgorithm, ":: " + _org.getAsText(0, 1));
			Iterator<Double> _fIterator = _org.getFitnessIterator();

			if(_org.getOFCount() != _numObjectives) {
				logger.logf(this.nimrodAlgorithm, "Looking into chromorome: " + _org.getChromosome().getOFCount());
				Iterator<ObjFunc> iOF;
				iOF = _org.getChromosome().getOFIterator();
				while(iOF.hasNext()) {
					ObjFunc of = iOF.next();
					double ofv = of.getObjFunc(_org.getChromosome().getPhenotype());
					logger.logf(this.nimrodAlgorithm, "___________ofv: " + ofv);
				} //while
				throw new Exception("[GaneshGA::nextIteration] org OF count (" + _org.getOFCount() + ") is not equal to experiment objective count");
			}

			if(_org.getRank() == 1) {
				logger.logf(this.nimrodAlgorithm, "num of objectives: " + _numObjectives);
				OptimPoint _optimPoint = new OptimPoint(this.setOfParams);
				//set optimPoint
				Gene[] _genotype = _org.getChromosome().getGenotype();
				if(_genotype.length != _optimPoint.dimensionality) {
					throw new Exception("[GaneshGA::nextIteration] genotype and number of params do not have the same length");
				}

				logger.logf(this.nimrodAlgorithm, "optim point d=: " + _optimPoint.dimensionality);
				for(int i = 0; i < _optimPoint.dimensionality; i++) {
					if(_genotype[i] instanceof GeneLong) {
						_optimPoint.coords[i] = ((GeneLong)_genotype[i]).getValue();
					} else if(_genotype[i] instanceof GeneReal) {
						_optimPoint.coords[i] = ((GeneReal)_genotype[i]).getValue();
					}
					logger.logf(this.nimrodAlgorithm, "adding value: " + _optimPoint.coords[i]);
				}
				logger.logf(this.nimrodAlgorithm, "Done creating optim point");
				//create _optimPoint
				PointContainer _pContainer = new PointContainer(_optimPoint, _numObjectives);
				logger.logf(this.nimrodAlgorithm, "Done creating point container");
				for(int i = 0; i < _numObjectives; i++) {
					_pContainer.objectives[i] = _org.getOFValue(i);
					logger.logf(this.nimrodAlgorithm, "objective: " + _org.getOFValue(i));

				}
				this.paretoPoints.add(_pContainer);
				logger.logf(this.nimrodAlgorithm, "Add one");
			}
		}
		logger.logf(this.nimrodAlgorithm, "Done processing pareto-front. Continue running or not");

		// Merge old Parent & new child pops - for elitism
		mixedPop = parentPop.mergePopulations(childPop);
		// Get new Parent pop by sorting mixedPop based on dom-rank, and
		// request new parentPop is original size, as given by popSize
		parentPop = mixedPop.fastNonDominatedSort(popSize); // Selection
		childPop = new Population(this, popSize, prbMutation, prbCrossover);
		parentPop.breed(childPop); // Parent pop breeds child pop
		evalFile.writePop(childPop); // save the pop in eval file

		//fill up evalSet
		this.fillUpEvalSet(childPop);

		//condition to continue
		if(isR1) {
			allR1 = parentPop.checkAllR1();
		} // All Rank 1?

		// memory reporting - just for info
		if(genCount * popSize > memUT) {
			if((memShowCnt++ % memShowGen == 1)) {
				mem.showMemory();
				System.gc(); // garbage collector
			}
		}

		genCount++;
		// check termination conditions

		// only one of -m and -g
		if(numMins != null) {
			// if -r1 chosen then so must -m be
			if(isR1) {
				stillContinue = !allR1;
				if(allR1) {
					logger.logf(this.nimrodAlgorithm, "All are Rank 1");
				}
			} // isR1
			nowTime = TimeUtils.getTime();
			long duration = TimeUtils.getMinsDiff(startTime, nowTime);
			if(duration >= numMins.longValue()) {
				logger.logf(this.nimrodAlgorithm, "Timeout after %d mins\n", duration);
				stillContinue = false;
			}
		} /* numMins */ else if(genCount >= maxGen) {
			stillContinue = false;
			logger.logf(this.nimrodAlgorithm, "Count of generations run: %d\n", genCount);
		} // not numMins

		if(exitByOFVal) {
			// Check whether OF values set terminaton condition
			if(parentPop.canStopByOF()) {
				stillContinue = false; // continue not true when terminate true
				logger.logf(this.nimrodAlgorithm, "Terminated as value of OF reached\n");
			}//if stop
		}//if check OF vals
	}

	/**
	 * get pareto poitns
	 *
	 * @return
	 */
	public List<PointContainer> getParetoPoints() {
		return this.paretoPoints;
	}

}

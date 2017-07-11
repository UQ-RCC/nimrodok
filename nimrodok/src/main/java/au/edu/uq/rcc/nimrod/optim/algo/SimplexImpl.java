package au.edu.uq.rcc.nimrod.optim.algo;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.Batch;
import au.edu.uq.rcc.nimrod.optim.Configuration;
import au.edu.uq.rcc.nimrod.optim.IAlgorithmDefinition;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.PopulationBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Properties;
import au.edu.uq.rcc.nimrod.optim.TrajectoryBaseAlgorithm;
import org.monash.nimrod.optim.SimplexAlgorithm;

public class SimplexImpl extends TrajectoryBaseAlgorithm {

	private static final int DEFAULT_ITERATIONS = 1000;
	private static final boolean DEFAULT_ORIGINAL_METHOD = false;
	private static final double DEFAULT_TOLERANCE = 0.0;

	private final int maxIters;
	private final boolean originalMethod;
	private final double tolerance;

	public final SimplexAlgorithm sxAlg;

	public SimplexImpl(OptimPoint start, Configuration config, INimrodReceiver logger) throws NimrodOKException {
		super(start, config, logger);
		Properties props = config.customProperties();
		maxIters = parseMaxIters(props);
		originalMethod = parseOriginalMethod(props);
		tolerance = parseTolerance(props);

		logger.logf(this, "SimplexImpl: %d iterations, %susing original method, tolerance %f\n",
				maxIters, originalMethod ? "" : "not ", tolerance);

		this.sxAlg = new SimplexAlgorithm(maxIters, originalMethod, tolerance, start);
		this.sxAlg.conTest = config.convergenceSettings();
		this.sxAlg.startingPoint = start;
		this.sxAlg.origStartingPoint = new OptimPoint(start);
		this.sxAlg.setOfParams = start.setOfParams;
	}

	// <editor-fold defaultstate="collapsed" desc="Configuration">
	private static int parseMaxIters(Properties props) throws NimrodOKException {
		String _maxIters = props.getProperty("simplex.max_iters");
		if(_maxIters == null) {
			return DEFAULT_ITERATIONS;
		}

		int iters;
		try {
			iters = Integer.parseInt(_maxIters);
		} catch(Exception e) {
			throw new NimrodOKException(e.getMessage());
		}

		if(iters < 1) {
			throw new NimrodOKException("Max iterations must be >= 1");
		}

		return iters;
	}

	private static boolean parseOriginalMethod(Properties props) throws NimrodOKException {
		String _original = props.getProperty("simplex.original_method");
		if(_original == null) {
			return DEFAULT_ORIGINAL_METHOD;
		}

		boolean original;
		try {
			original = Boolean.parseBoolean(_original);
		} catch(NumberFormatException e) {
			throw new NimrodOKException(e.getMessage());
		}

		return original;
	}

	private static double parseTolerance(Properties props) throws NimrodOKException {
		String _tolerance = props.getProperty("simplex.tolerance");
		if(_tolerance == null) {
			return DEFAULT_TOLERANCE;
		}

		double tolerance;
		try {
			tolerance = Double.parseDouble(_tolerance);
		} catch(NumberFormatException e) {
			throw new NimrodOKException(e.getMessage());
		}

		return tolerance;
	}
	// </editor-fold>

	@Override
	public void fire() throws NimrodOKException {
		receiver.logf(this, "SimplexImpl: Called with state %s", m_State);
		if(m_State == State.STOPPED) {
			m_State = State.WAITING_FOR_BATCH;
			m_CurrentBatch = evalBatch(sxAlg.evalSet);
		} else if(m_State == State.WAITING_FOR_BATCH) {
			/* If the batch hasn't finished, why have we been called? */
			if(!m_CurrentBatch.isFinished()) {
				receiver.logf(this, "SimplexImpl: Batch not finished, why were we called?");
				return;
			}

			m_State = State.RUNNING;

			for(int i = 0; i < m_CurrentBatch.points.length; ++i) {
				sxAlg.evalSet.setObjective(i, m_CurrentBatch.points[i].objectives[0]);
			}

			m_CurrentBatch = null;

			sxAlg.nextSimplex();
			if(sxAlg.convergent) {
				receiver.logf(this, "In SimplexOptimActor:fire mode 2: simplex has converged");

				PointContainer result = new PointContainer(sxAlg.resultPoint, config.objectiveCount());
				result.objectives[0] = result.point.objective;
				m_ParetoFront.add(result);
				m_State = State.FINISHED;
			} else {
				m_CurrentBatch = evalBatch(sxAlg.evalSet);
				m_State = State.WAITING_FOR_BATCH;
			}
		} else if(m_State == State.RUNNING) {
			/* Should never happen */
		} else if(m_State == State.FINISHED) {
			/* We're done, why are you calling us again? */
		}
	}

	private Batch evalBatch(ArrayOfPoints exploratoryPoints) throws NimrodOKException {
		sxAlg.pointsEvaluated = 0;
		sxAlg.storeJobs(exploratoryPoints);

		PointContainer[] currentBatch = new PointContainer[exploratoryPoints.pointArray.length];
		for(int i = 0; i < currentBatch.length; ++i) {
			currentBatch[i] = new PointContainer(exploratoryPoints.pointArray[i], config.objectiveCount());
		}

		return createBatch(currentBatch);
	}

	private static final Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		PROPERTIES.setProperty("simplex.max_iters", Integer.toString(DEFAULT_ITERATIONS));
		PROPERTIES.setProperty("simplex.tolerance", Double.toString(DEFAULT_TOLERANCE));
		PROPERTIES.setProperty("simplex.original_method", Boolean.toString(DEFAULT_ORIGINAL_METHOD));
	}

	public static IAlgorithmDefinition definition = new IAlgorithmDefinition() {
		@Override
		public String getUniqueName() {
			return "SIMPLEX";
		}

		@Override
		public String getPrettyName() {
			return "Simplex (Nelder-Mead)";
		}

		@Override
		public PopulationBaseAlgorithm createPopulationInstance(OptimPoint[] initialPopulation, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			throw new UnsupportedOperationException();
		}

		@Override
		public TrajectoryBaseAlgorithm createTrajectoryInstance(OptimPoint startingPoint, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			return new SimplexImpl(startingPoint, config, receiver);
		}


		@Override
		public int getSupportedObjectiveCount() {
			return 1;
		}

		@Override
		public Properties getDefaultProperties() {
			return PROPERTIES;
		}

		@Override
		public boolean isTrajectoryBased() {
			return true;
		}

		@Override
		public boolean isPopulationBased() {
			return false;
		}
	};
}

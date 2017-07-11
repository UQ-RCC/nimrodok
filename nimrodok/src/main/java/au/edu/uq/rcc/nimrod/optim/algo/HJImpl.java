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
import org.monash.nimrod.optim.HookeAndJeevesAlgorithm;

public class HJImpl extends TrajectoryBaseAlgorithm {

	private static final int DEFAULT_ITERATIONS = 1000;
	private static final double DEFAULT_TOLERANCE = 1;

	private final int maxIters;
	private final double tolerance;

	public final HookeAndJeevesAlgorithm hjAlg;

	public HJImpl(OptimPoint start, Configuration config, INimrodReceiver logger) throws NimrodOKException {
		super(start, config, logger);
		Properties props = config.customProperties();
		maxIters = parseMaxIters(props);
		tolerance = parseTolerance(props);

		this.hjAlg = new HookeAndJeevesAlgorithm(maxIters, tolerance, start);
		this.hjAlg.generateNextEvalPoints();
		this.hjAlg.setOfParams = start.setOfParams;
		this.hjAlg.pointsEvaluated = 0;
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
	
	private static double parseTolerance(Properties props) throws NimrodOKException {
		String _tolerance = props.getProperty("hj.tolerance");
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
		receiver.logf(this, "HJImpl: Called with state %s", m_State);
		if(m_State == State.STOPPED) {
			m_State = State.WAITING_FOR_BATCH;
			m_CurrentBatch = evalBatch(hjAlg.exploratoryPoints);
		} else if(m_State == State.WAITING_FOR_BATCH) {
			/* If the batch hasn't finished, why have we been called? */
			if(!m_CurrentBatch.isFinished()) {
				receiver.logf(this, "HJImpl: Batch not finished, why were we called?");
				return;
			}

			m_State = State.RUNNING;

			for(int i = 0; i < m_CurrentBatch.points.length; ++i) {
				OptimPoint pt = hjAlg.exploratoryPoints.pointArray[i];
				pt.objective = pt.cost = m_CurrentBatch.points[i].objectives[0];
				pt.defined = true;
			}

			hjAlg.pointsEvaluated = hjAlg.exploratoryPoints.numPoints;
			
			m_CurrentBatch = null;

			hjAlg.processresults();
			if(hjAlg.convergent) {
				receiver.logf(this, "In HJOptimActor:fire mode 2: algorithm has converged");
				m_State = State.FINISHED;

				PointContainer result = new PointContainer(hjAlg.resultPoint[0], config.objectiveCount());
				result.objectives[0] = result.point.objective;
				m_ParetoFront.add(result);
				m_State = State.FINISHED;
			} else {
				hjAlg.generateNextEvalPoints();
				m_CurrentBatch = evalBatch(hjAlg.exploratoryPoints);
				m_State = State.WAITING_FOR_BATCH;
			}
		} else if(m_State == State.RUNNING) {
			/* Should never happen */
		} else if(m_State == State.FINISHED) {
			/* We're done, why are you calling us again? */
		}
	}

	private Batch evalBatch(ArrayOfPoints exploratoryPoints) throws NimrodOKException {
		hjAlg.pointsEvaluated = 0;
		hjAlg.storeJobs(exploratoryPoints);

		PointContainer[] currentBatch = new PointContainer[exploratoryPoints.pointArray.length];
		for(int i = 0; i < currentBatch.length; ++i) {
			currentBatch[i] = new PointContainer(exploratoryPoints.pointArray[i], config.objectiveCount());
		}
		
		return createBatch(currentBatch);
	}

	private static final Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		PROPERTIES.setProperty("hj.max_iters", Integer.toString(DEFAULT_ITERATIONS));
		PROPERTIES.setProperty("hj.tolerance", Double.toString(DEFAULT_TOLERANCE));
	}

	public static IAlgorithmDefinition definition = new IAlgorithmDefinition() {
		@Override
		public String getUniqueName() {
			return "HOOKEJEEVES";
		}

		@Override
		public String getPrettyName() {
			return "Hooke-Jeeves";
		}

		@Override
		public PopulationBaseAlgorithm createPopulationInstance(OptimPoint[] initialPopulation, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			throw new UnsupportedOperationException();
		}

		@Override
		public TrajectoryBaseAlgorithm createTrajectoryInstance(OptimPoint startingPoint, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			return new HJImpl(startingPoint, config, receiver);
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
	
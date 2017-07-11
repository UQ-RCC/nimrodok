package au.edu.uq.rcc.nimrod.optim.algo;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.Batch;
import au.edu.uq.rcc.nimrod.optim.Configuration;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.PopulationBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Properties;
import au.edu.uq.rcc.nimrod.optim.TrajectoryBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.IAlgorithmDefinition;
import org.monash.nimrod.optim.SubdivisionAlgorithm;

public class SubdivImpl extends TrajectoryBaseAlgorithm {

	private static final int DEFAULT_ITERATIONS = 1000;
	private static final boolean DEFAULT_DRIFT_ALLOWED = true;
	private static final int DEFAULT_UNIFORM_STEP = 5;

	private final int[] defaultSteps;
	
	private final int maxIters;
	private final boolean driftAllowed;
	private final int[] steps;

	public final SubdivisionAlgorithm subdivAlg;

	public SubdivImpl(OptimPoint start, Configuration config, INimrodReceiver logger) throws NimrodOKException {
		super(start, config, logger);
		Properties props = config.customProperties();
		maxIters = parseMaxIters(props);
		driftAllowed = parseDrift(props);
		
		defaultSteps = new int[start.dimensionality];
		for(int i = 0; i < start.dimensionality; ++i) {
			defaultSteps[i] = DEFAULT_UNIFORM_STEP;
		}
		
		steps = parseSteps(defaultSteps, props);

		this.subdivAlg = new SubdivisionAlgorithm(maxIters, driftAllowed, steps, start);
		subdivAlg.Subdivide();
	}

	// <editor-fold defaultstate="collapsed" desc="Configuration">
	private static int parseMaxIters(Properties props) throws NimrodOKException {
		String _maxIters = props.getProperty("subdiv.max_iters");
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

	private static boolean parseDrift(Properties props) throws NimrodOKException {
		String _drift = props.getProperty("subdiv.drift_allowed");
		if(_drift == null) {
			return DEFAULT_DRIFT_ALLOWED;
		}

		boolean drift;
		try {
			drift = Boolean.parseBoolean(_drift);
		} catch(NumberFormatException e) {
			throw new NimrodOKException(e.getMessage());
		}

		return drift;
	}

	private static int[] parseSteps(int[] defaultSteps, Properties props) throws NimrodOKException {
		String _steps = props.getProperty("subdiv.steps");
		if(_steps == null) {
			return defaultSteps;
		}

		
		String[] toks = _steps.trim().split(",\\s+");
		if(toks.length != defaultSteps.length) {
			throw new NimrodOKException("Step count != dimensionality");
		}
		
		int[] steps = new int[toks.length];
		try {
			for(int i = 0; i < toks.length; ++i) {
				steps[i] = Integer.parseInt(toks[i]);
				
				if(steps[i] <= 0) {
					throw new NumberFormatException(String.format("Step %d <= 0", i));
				}
			}
			
		} catch(NumberFormatException e) {
			throw new NimrodOKException(e.getMessage());
		}

		
		return steps;
	}
	// </editor-fold>

	@Override
	public void fire() throws NimrodOKException {
		receiver.logf(this, "SubdivImpl: Called with state %s", m_State);
		if(m_State == State.STOPPED) {
			m_State = State.WAITING_FOR_BATCH;
			m_CurrentBatch = evalBatch(subdivAlg.evalSet);
		} else if(m_State == State.WAITING_FOR_BATCH) {
			/* If the batch hasn't finished, why have we been called? */
			if(!m_CurrentBatch.isFinished()) {
				receiver.logf(this, "SubdivImpl: Batch not finished, why were we called?");
				return;
			}

			m_State = State.RUNNING;

			for(int i = 0; i < m_CurrentBatch.points.length; ++i) {
				double objective = m_CurrentBatch.points[i].objectives[0];
				subdivAlg.evalSet.pointArray[i].objective = objective;
				subdivAlg.evalSet.pointArray[i].cost = objective;
				subdivAlg.evalSet.pointArray[i].evaluated = true;
			}
			
			subdivAlg.pointsEvaluated = m_CurrentBatch.points.length;
			m_CurrentBatch = null;

			subdivAlg.selectSubdomain();
			
			if(config.convergenceSettings().checkConvergence(subdivAlg.evalSet) || subdivAlg.iteration == subdivAlg.maxIters) {
				subdivAlg.ProduceResultPoints();
				
				PointContainer result = new PointContainer(subdivAlg.resultPoint, config.objectiveCount());
				result.objectives[0] = result.point.objective;
				m_ParetoFront.add(result);
				m_State = State.FINISHED;
			} else {
				subdivAlg.Subdivide();
				m_CurrentBatch = evalBatch(subdivAlg.evalSet);
				m_State = State.WAITING_FOR_BATCH;
			}
		} else if(m_State == State.RUNNING) {
			/* Should never happen */
		} else if(m_State == State.FINISHED) {
			/* We're done, why are you calling us again? */
		}
	}

	private Batch evalBatch(ArrayOfPoints exploratoryPoints) throws NimrodOKException {
		subdivAlg.pointsEvaluated = 0;

		PointContainer[] currentBatch = new PointContainer[exploratoryPoints.pointArray.length];
		for(int i = 0; i < currentBatch.length; ++i) {
			currentBatch[i] = new PointContainer(exploratoryPoints.pointArray[i], config.objectiveCount());
		}
		
		return createBatch(currentBatch);
	}

	private static final Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		PROPERTIES.setProperty("subdiv.max_iters", Integer.toString(DEFAULT_ITERATIONS));
		PROPERTIES.setProperty("subdiv.drift_allowed", Boolean.toString(DEFAULT_DRIFT_ALLOWED));
		PROPERTIES.setProperty("subdiv.steps", String.format("%d, %d, %d", DEFAULT_UNIFORM_STEP, DEFAULT_UNIFORM_STEP, DEFAULT_UNIFORM_STEP));
	}

	public static IAlgorithmDefinition definition = new IAlgorithmDefinition() {
		@Override
		public String getUniqueName() {
			return "SUVDIVISION";
		}

		@Override
		public String getPrettyName() {
			return "Subdivision";
		}

		@Override
		public PopulationBaseAlgorithm createPopulationInstance(OptimPoint[] initialPopulation, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			throw new UnsupportedOperationException();
		}

		@Override
		public TrajectoryBaseAlgorithm createTrajectoryInstance(OptimPoint startingPoint, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			return new SubdivImpl(startingPoint, config, receiver);
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
	
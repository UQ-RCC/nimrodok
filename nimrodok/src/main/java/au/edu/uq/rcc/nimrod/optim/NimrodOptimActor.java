/*
 * Copyright (C) 2017 Zane van Iperen
 * All rights reserved.
 * 
 * NOTICE: This code may not be used unless explicit permission
 * is obtained from Zane van Iperen.
 * 
 * CONTACT: zane@zanevaniperen.com
 */
package au.edu.uq.rcc.nimrod.optim;

import au.edu.uq.rcc.nimrod.optim.algo.SimplexImpl;
import au.edu.uq.rcc.nimrod.optim.modules.Author;
import au.edu.uq.rcc.nimrod.optim.modules.INimrodOKModule;
import au.edu.uq.rcc.nimrod.optim.modules.Module;
import au.edu.uq.rcc.nimrod.optim.modules.ModuleException;
import au.edu.uq.rcc.nimrod.optim.modules.ModuleLoader;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ptolemy.kernel.*;
import ptolemy.kernel.util.*;
import ptolemy.actor.*;
import ptolemy.data.*;

import ptolemy.data.type.*;

import org.monash.nimrod.data.Tag;
import ptolemy.actor.gui.PtolemyQuery;
import ptolemy.data.expr.FileParameter;
import ptolemy.data.expr.Parameter;

import org.apache.log4j.Logger;
import org.monash.nimrod.NimrodActor.BasicFunctions;
import org.monash.nimrod.NimrodActor.Functions;
import org.monash.nimrod.NimrodActor.NimrodAtomicActor;
import org.monash.nimrod.NimrodActor.TagActor;
import ptolemy.actor.gui.style.ChoiceStyle;
import ptolemy.data.expr.StringParameter;

public class NimrodOptimActor extends NimrodAtomicActor<BasicFunctions> implements TagActor {

	private static final String DEFAULT_CONVERGENCE_SETTINGS = "tolerance 1\nspatial range";
	private final Logger logger = Logger.getLogger(NimrodOptimActor.class);

	private static List<Module> m_sModules = null;

	private ChoiceStyle m_SelectionStyle;

	public final Functions nimrodK;

	private final Map<String, IAlgorithmDefinition> m_AlgorithmLookup;
	private final Map<String, IAlgorithmDefinition> m_AlgorithmPrettyLookup;
	private final Set<BaseAlgorithm> m_Optimisations;

	private final StringParameter optSettings;
	private final StringParameter convSettings;
	private final Parameter m_ParamChosenAlgorithm;
	private final Parameter m_ParamNumObjectives;
	private final Parameter m_ParamRNGSeed;
	private final Parameter m_ParamDefaultSettings;
	//private final FileParameter m_ParamSaveDir;
	private final Parameter m_ParamShowMonitor;

	private final INimrodReceiver m_ActorInterface;

	private IAlgorithmDefinition m_Algo;
	private Configuration m_Configuration;

	private String paramString;
	private String convString;
	private ConvergenceTest conTest;
	private int m_NumObjectives;
	private Properties m_CustomConfiguration;
	private int m_RNGSeed;

	private ObjectivePort[] m_ObjectivePorts;
	private final TypedIOPort startsInputPort;
	private final TypedIOPort outputPort;
	private final TypedIOPort resultPort;
	private final TypedIOPort statsPort;

	private NimrodMonitor m_Monitor;

	private enum OperatingMode {
		NO_ACTIVITY,
		INCOMING_POINT,
		INCOMING_RESULT
	}

	private OperatingMode m_OperatingMode;

	public NimrodOptimActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		if(m_sModules == null) {
			loadExternalModules(logger);
		}

		nimrodK = nimrodKFunctions();

		m_AlgorithmLookup = new HashMap<>();
		m_AlgorithmPrettyLookup = new HashMap<>();
		m_Optimisations = new HashSet<>();

		setupAlgorithms();

		m_ActorInterface = new _OptimisationActor();

		m_Monitor = new NimrodMonitor();

		optSettings = new StringParameter(this, "OptimSettings");
		optSettings.setDisplayName("Optimisation settings");

		convSettings = new StringParameter(this, "ConvergenceSettings");
		convSettings.setDisplayName("Convergence settings");

		convString = DEFAULT_CONVERGENCE_SETTINGS;
		convSettings.setExpression(DEFAULT_CONVERGENCE_SETTINGS);
		conTest = new ConvergenceTest(DEFAULT_CONVERGENCE_SETTINGS);

		//m_ParamSaveDir = setupSaveDir();
		m_ParamChosenAlgorithm = setupAlgorithmSelection();
		m_ParamDefaultSettings = setupDefaultSettings();

		m_ParamShowMonitor = new Parameter(this, "ShowMonitor", BooleanToken.FALSE);
		m_ParamShowMonitor.setDisplayName("Show monitor?");
		m_ParamShowMonitor.setTypeEquals(BaseType.BOOLEAN);

		startsInputPort = new TypedIOPort(this, "StartingPoints", true, false);
		startsInputPort.setTypeEquals(PortTypes.POINT_ARRAY_TYPE);
		startsInputPort.setDisplayName("Starting Points");

		resultPort = new TypedIOPort(this, "OptimalPoint", false, true);
		resultPort.setTypeEquals(PortTypes.RESULT_PORT_TYPE);
		resultPort.setDisplayName("Optimal Point");

		statsPort = new TypedIOPort(this, "Statistics", false, true);
		statsPort.setTypeEquals(PortTypes.STATS_PORT_TYPE);
		statsPort.setDisplayName("Statistics");

		outputPort = new TypedIOPort(this, "EvalOutput", false, true);
		outputPort.setTypeEquals(PortTypes.POINT_TYPE);
		outputPort.setDisplayName("Points for Evaluation");
		StringAttribute outputPortCardinal = new StringAttribute(outputPort, "_cardinal");
		outputPortCardinal.setExpression("SOUTH");

		m_ObjectivePorts = ActorUtils.regenerateObjectivePorts(this, new ObjectivePort[0], 1);

		m_NumObjectives = 1;
		m_ParamNumObjectives = setupNumObjectives();

		m_RNGSeed = 0;
		m_ParamRNGSeed = setupRNGSeed();

		m_CustomConfiguration = new Properties();

	}

	// <editor-fold defaultstate="collapsed" desc="Construction Helpers">
	private void registerAlgorithm(IAlgorithmDefinition algo) {
		if(algo == null) {
			return;
		}

		m_AlgorithmLookup.put(algo.getUniqueName(), algo);
		m_AlgorithmPrettyLookup.put(algo.getPrettyName(), algo);
	}

	private void setupAlgorithms() {

		registerAlgorithm(SimplexImpl.definition);
		//registerAlgorithm(HJImpl.definition);
		//registerAlgorithm(SubdivImpl.definition);

		/* Default to Simplex. */
		m_Algo = SimplexImpl.definition;

		/* Register any from external modules */
		for(Module module : m_sModules) {
			for(IAlgorithmDefinition algo : module.getModule().getAlgorithms()) {
				registerAlgorithm(algo);
			}
		}

	}

	private Parameter setupAlgorithmSelection() throws IllegalActionException, NameDuplicationException {
		Parameter param = new Parameter(this, "Algorithm");
		AlgorithmicChoiceStyle cs = new AlgorithmicChoiceStyle(param, "Algorithm", this);

		m_SelectionStyle = cs;
		PtolemyQuery query = new PtolemyQuery(this);
		for(IAlgorithmDefinition algo : m_AlgorithmLookup.values()) {
			StringAttribute s = new StringAttribute(cs, algo.getUniqueName());
			s.setExpression(algo.getPrettyName());
			query.addStyledEntry(s);

		}
		cs.addEntry(query);

		param.setStringMode(true);
		param.setExpression(m_Algo.getPrettyName());
		param.setDisplayName("Algorithm");

		return param;
	}

	private Parameter setupNumObjectives() throws IllegalActionException, NameDuplicationException {
		Parameter param = new Parameter(this, "ObjectiveCount");
		param.setDisplayName("Number of Objectives");
		param.setTypeEquals(BaseType.INT);
		param.setToken(new IntToken(m_NumObjectives));
		return param;
	}

	private Parameter setupRNGSeed() throws IllegalActionException, NameDuplicationException {
		Parameter param = new Parameter(this, "RNGSeed");
		param.setDisplayName("RNG Seed (32-bit)");
		param.setTypeEquals(BaseType.INT);
		param.setToken(new IntToken(m_RNGSeed));
		return param;
	}

	private Parameter setupDefaultSettings() throws IllegalActionException, NameDuplicationException {
		Parameter param = new Parameter(this, "DefaultSettings");
		param.setDisplayName("Default Settings");
		param.setTypeEquals(BaseType.STRING);
		param.setToken(new StringToken());

		param.setStringMode(true);
		param.setVisibility(Settable.NOT_EDITABLE);
		return param;
	}

	private FileParameter setupSaveDir() throws IllegalActionException, NameDuplicationException {
		FileParameter param = new FileParameter(this, "SaveDirectory");
		param.setDisplayName("Save Directory");
		param.setTypeEquals(BaseType.STRING);
		String dirName = String.format("$CWD/%d-saverestore", this.hashCode());
		param.setToken(new StringToken(dirName));
		new Parameter(param, "allowFiles", BooleanToken.FALSE).setVisibility(Settable.NOT_EDITABLE);
		new Parameter(param, "allowDirectories", BooleanToken.TRUE).setVisibility(Settable.NOT_EDITABLE);

		return param;
	}

	// </editor-fold>
	public IAlgorithmDefinition lookupAlgorithm(String uniqueName) {
		return m_AlgorithmLookup.get(uniqueName);
	}

	public IAlgorithmDefinition lookupPrettyAlgorithm(String prettyName) {
		return m_AlgorithmPrettyLookup.get(prettyName);
	}

	@Override
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();

		List l = inputPortList();
		for(Iterator ip = l.iterator(); ip.hasNext();) {
			IOPort io = (IOPort) ip.next();
			if(io.getAttribute("tokenConsumptionRate") != null) {
				((Parameter) io.getAttribute("tokenConsumptionRate")).setToken(IntToken.ZERO);
			} else {
				try {
					new Parameter(io, "tokenConsumptionRate", IntToken.ZERO);
				} catch(NameDuplicationException e) {
					logger.error("Error setting tokenConsumptionRate", e);
				}
			}
		}

		m_Monitor.reset();

		if(((BooleanToken) m_ParamShowMonitor.getToken()).booleanValue()) {
			m_Monitor.open();
		}
	}

	@Override
	public void initialize() throws IllegalActionException {
		debugf("initialize() called");
		super.initialize();

		/* Try doing this in attributeChanged(). I dare you. */
		if(m_NumObjectives > m_Algo.getSupportedObjectiveCount()) {
			throw new IllegalActionException("The current algorithm doesn't support multiple objectives.");
		}

		/* Merge the default properties with the user's custom ones. */
		Properties combinedProps = new Properties(m_Algo.getDefaultProperties());
		combinedProps.addAll(m_CustomConfiguration);

		m_Configuration = new Configuration(m_NumObjectives, m_RNGSeed, conTest, combinedProps);
	}

	@Override
	public boolean prefire() throws IllegalActionException {
		m_OperatingMode = OperatingMode.NO_ACTIVITY;

		if(startsInputPort.hasToken(0)) {
			m_OperatingMode = OperatingMode.INCOMING_POINT;
		} else {
			for(ObjectivePort o : m_ObjectivePorts) {
				if(o.hasToken()) {
					m_OperatingMode = OperatingMode.INCOMING_RESULT;
					break;
				}
			}
		}

		return super.prefire() ? (m_OperatingMode != OperatingMode.NO_ACTIVITY) : false;
	}

	private void fireAlgorithm(BaseAlgorithm algo) throws IllegalActionException, NimrodOKException {
		algo.fire();
		m_Monitor.update(algo);
		//sendStatsMessage(algo, algo.getState().toString());
		sendStatsPareto(algo);
		switch(algo.getState()) {
			/* New batch, dispatch it. */
			case WAITING_FOR_BATCH:
				Batch batch = algo.getCurrentBatch();

				//this.debugf("Batch has %d points", batch.points.length);
				for(PointContainer pc : batch.points) {
					sendPointForProcessing(pc, batch);
				}
				break;
			/* Finished, send the result. */
			case FINISHED:
				List<PointContainer> results = algo.getResults();

				sendWithCurrentColour(resultPort, ActorUtils.makeResultToken(algo.startingPoints, results));
				m_Monitor.update(algo);
				break;
		}
	}

	@Override
	public void fire() throws IllegalActionException {
		//debugf("fire() called with operating mode %s", m_OperatingMode);

		if(m_OperatingMode == OperatingMode.INCOMING_POINT) {
			/* New points, new optimisation! */
			ArrayOfPoints newPoints = PortTypes.makeArrayOfPoints(startsInputPort.get(0));

			if(m_Algo.isTrajectoryBased()) {
				/* Trajectory-based, create a new optimisation for each point. */
				for(OptimPoint pt : newPoints.pointArray) {
					try {
						BaseAlgorithm impl = m_Algo.createTrajectoryInstance(pt, m_Configuration, m_ActorInterface);
						m_Optimisations.add(impl);
						m_Monitor.addInstance(impl);
						fireAlgorithm(impl);
					} catch(NimrodOKException e) {
						throw new IllegalActionException(this, e, e.getMessage());
					}
				}
			} else if(m_Algo.isPopulationBased()) {
				try {
					/* Population based, we ARE the population. */
					BaseAlgorithm impl = m_Algo.createPopulationInstance(newPoints.pointArray, m_Configuration, m_ActorInterface);
					m_Optimisations.add(impl);
					m_Monitor.addInstance(impl);
					fireAlgorithm(impl);
				} catch(NimrodOKException e) {
					throw new IllegalActionException(this, e, e.getMessage());
				}
			} else {
				throw new IllegalActionException("Algorithm is neither trajectory or population based. This is a bug.");
			}
		} else if(m_OperatingMode == OperatingMode.INCOMING_RESULT) {
			/* New result, process it! */
			ObjectivePort.Result result = null;
			for(ObjectivePort o : m_ObjectivePorts) {
				if((result = o.fire()) != null) {
					break;
				}
			}

			/* result will never be null *
			 * If the batch is finished, fire() the state machine again. */
			if(result.batch.isFinished()) {
				for(int i = 0; i < result.batch.points.length; ++i) {
					sendStatsPoint(result.batch.instance, result.batch.points[i]);
				}
				try {
					fireAlgorithm(result.batch.instance);
				} catch(NimrodOKException e) {
					throw new IllegalActionException(this, e, e.getMessage());
				}
			}

		}
	}

	@Override
	public void wrapup() {
		for(BaseAlgorithm algo : m_Optimisations) {
			algo.cleanup();
		}
	}

	private void sendStatsPoint(BaseAlgorithm instance, PointContainer pc) throws IllegalActionException {
		sendWithCurrentColour(statsPort, ActorUtils.makeStatsPointToken(instance, pc));
	}

	private void sendStatsMessage(BaseAlgorithm instance, String message) throws IllegalActionException {
		sendWithCurrentColour(statsPort, ActorUtils.makeStatsMessageToken(instance, message));
	}

	private void sendStatsStats(BaseAlgorithm instance, Map<String, String> stats) throws IllegalActionException {
		sendWithCurrentColour(statsPort, ActorUtils.makeStatsStatsToken(instance, stats));
	}

	private void sendStatsPareto(BaseAlgorithm instance) throws IllegalActionException {
		RecordToken token = ActorUtils.makeStatsParetoToken(instance, instance.getResults());
		if(token != null) {
			sendWithCurrentColour(statsPort, token);
		}
	}

	private void sendWithCurrentColour(TypedIOPort port, Token token) throws IllegalActionException {
		Tag colour;
		if(m_OperatingMode == OperatingMode.INCOMING_POINT) {
			colour = nimrodK.getFiringColour();
		} else {
			colour = nimrodK.getFiringColour().popTag();
		}

		nimrodK.send(port, 0, token, colour);
	}

	@Override
	public boolean postfire() throws IllegalActionException {
		return super.postfire();
	}

	// <editor-fold defaultstate="collapsed" desc="Attribute Change Handlers">
	@Override
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		if(attribute == convSettings) {
			convString = (((StringToken) convSettings.getToken()).stringValue()).replaceAll("\\\\n", "\n");
			if(convString.length() != 0) {
				ConvergenceTest ctest = new ConvergenceTest(convString);
				if(!ctest.parsingValid) {

					if(conTest == null) {
						conTest = ctest;
					}
					throw new IllegalActionException(this, "Parsing error for convergence settings : " + conTest.errorMessage);
				}

				conTest = ctest;
			}
		} else if(attribute == m_ParamChosenAlgorithm) {
			debugf("Algorithm changed to %s", m_ParamChosenAlgorithm.getExpression());

			/* There has to be a better way to do this...
			 * EDIT: Nope, there is no actual way to have the text in the selection box to represent something else.
			 * See Query.java:1909 */
			IAlgorithmDefinition algo = m_AlgorithmPrettyLookup.get(m_ParamChosenAlgorithm.getExpression());

			if(algo == null) {
				throw new IllegalActionException("Invalid algorithm change");
			}

			m_Algo = algo;
			m_ParamDefaultSettings.setExpression(m_Algo.getDefaultProperties().toString());
		} else if(attribute == m_ParamNumObjectives) {
			int numObj = ((IntToken) m_ParamNumObjectives.getToken()).intValue();
			if(numObj < 1) {
				throw new IllegalActionException("Number of objectives must be >= 1");
			}

			debugf("Number of Objectives parameter changed to %s", numObj);

			if(numObj != m_NumObjectives) {
				/* RANT: So apparently setContainer()'ing or creating a new port somehow causes changeAttributes() to be called
				 * for the objective ports parameter, causing a recursive loop. I'm not sure if this behaviour is intentional,
				 * but it would be nice if it was documented. 
				 *
				 * To account for this, update m_NumObjectives here, so the check above will be hit.
				 */
				int oldNum = m_NumObjectives;
				m_NumObjectives = numObj;
				try {
					m_ObjectivePorts = ActorUtils.regenerateObjectivePorts(this, m_ObjectivePorts, numObj);
				} catch(NameDuplicationException e) {
					debugf(e);
					m_NumObjectives = oldNum;
					throw new IllegalActionException(e.getMessage());
				}
			}
		} else if(attribute == optSettings) {
			String optParamString = optSettings.getExpression().replaceAll("\\\\n", "\n");

			try {
				m_CustomConfiguration = Properties.parseString(optParamString);
			} catch(java.text.ParseException e) {
				debugf(e);
				throw new IllegalActionException(e.getMessage());
			}
		} else if(attribute == m_ParamRNGSeed) {
			m_RNGSeed = ((IntToken) m_ParamRNGSeed.getToken()).intValue();
		} else {
			super.attributeChanged(attribute);
		}
	}
	//</editor-fold>

	// <editor-fold defaultstate="collapsed" desc="I/O Port Management">
	@Override
	public TagAction tagAction(TypedIOPort input, TypedIOPort output) {
		debugf("tagAction(%s, %s)", input, output);

		boolean isResultInput = false;
		for(ObjectivePort p : m_ObjectivePorts) {
			if(input == p.port) {
				isResultInput = true;
				break;
			}
		}

		if(isResultInput && output == resultPort) {
			return TagAction.REMOVE;
		}
		if(isResultInput && output == statsPort) {
			return TagAction.REMOVE;
		}
		if(input == startsInputPort && output == outputPort) {
			return TagAction.ADD;
		}

		return super.tagAction(input, output);
	}

	public TypedIOPort createOrGetIOPort(String portName, boolean output, String cardinal) throws IllegalActionException, NameDuplicationException {
		debugf("  Creating port \"%s\"", portName);
		return ActorUtils.createOrGetIOPort(this, portName, output, cardinal);
	}

	// </editor-fold>
	private class _OptimisationActor implements INimrodReceiver {

		@Override
		public void logf(BaseAlgorithm instance, String fmt, Object... args) {
			NimrodOptimActor.this.debugf(fmt, args);
		}

		@Override
		public void logf(BaseAlgorithm instance, Throwable e) {
			NimrodOptimActor.this.debugf(e);
		}

		@Override
		public void incomingStats(BaseAlgorithm instance, Stat[] stats) {
			HashMap<String, String> _stats = new HashMap<>();
			for(int i = 0; i < stats.length; ++i) {
				_stats.put(stats[i].key, stats[i].value);
			}

			try {
				sendStatsStats(instance, _stats);
			} catch(IllegalActionException e) {
				logf(instance, e);
			}
		}
	}

	/**
	 * Send a point for evaluation.
	 *
	 * @param nextPoint The point to send.
	 * @return The tag the point was sent with.
	 * @throws IllegalActionException
	 */
	private Tag sendPointForProcessing(PointContainer nextPoint, Batch batch) throws IllegalActionException {
		Token token = PortTypes.toToken(nextPoint.point);

		HashMap<String, Token> meta = new HashMap<>();
		meta.put("Optimizer", new ObjectToken(this));
		meta.put("creator", new ObjectToken(this));
		meta.put("point", new ObjectToken(nextPoint));
		meta.put("batch", new ObjectToken(batch));

		Tag colour = null;

		if(m_OperatingMode == OperatingMode.INCOMING_POINT) {
			colour = nimrodK.getFiringColour().pushTag(meta, token);
		} else if(m_OperatingMode == OperatingMode.INCOMING_RESULT) {
			colour = nimrodK.getFiringColour().popTag().pushTag(meta, token);
		}

		nimrodK.send(outputPort, 0, token, colour);
		debugf("Sending point %s with colour %s", nextPoint.point, colour);
		return colour;
	}

	private static String[] getClassPathDirectories() {
		List<String> paths = new ArrayList<>();
		for(String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
			/* Ignore .jar files. */
			if(s.toLowerCase().endsWith(".jar")) {
				continue;
			}

			paths.add(s);
		}

		return paths.toArray(new String[paths.size()]);
	}

	private static void loadExternalModules(Logger logger) {
		logger.info("First run, loading modules...");
		m_sModules = new ArrayList<>();

		/* Get all directories in the classpath. */
		String[] dirs = getClassPathDirectories();

		List<ModuleException> failed = new ArrayList<>();
		try {
			/* Attempt to load modules from the nimrodok-modules subdirectory of each. */
			for(int i = 0; i < dirs.length; ++i) {
				Path moduleFolder = Paths.get(dirs[i]).resolve("nimrodok-modules");
				if(!Files.exists(moduleFolder) || !Files.isDirectory(moduleFolder)) {
					continue;
				}
				logger.info(String.format("  Searching %s...", moduleFolder));
				try {
					ModuleLoader.loadModulesFromPath(moduleFolder, m_sModules, failed);
				} catch(IOException e) {
					logger.info("  Failed! ", e);
				}
			}
		} catch(Exception e) {
			logger.error("Error loading modules", e);
		}

		if(!failed.isEmpty()) {
			logger.warn("Some modules failed to load! Enable INFO level for more information.");

			for(int i = 0; i < failed.size(); ++i) {
				logger.info(String.format("  %s", failed.get(i).getMessage()));
			}
		}

		logger.info("Loaded Modules:");
		for(int i = 0; i < m_sModules.size(); ++i) {
			Module module = m_sModules.get(i);

			INimrodOKModule moduleInfo = module.getModule();
			logger.info(String.format("  Module: %s", moduleInfo.getName()));
			logger.info(String.format("    Path: %s", module.getPath()));
			Author[] authors = moduleInfo.getAuthors();
			for(int j = 0; j < authors.length; ++j) {
				String authorString = String.format("%s <%s>", authors[j].name, authors[j].email);

				if(j == 0) {
					logger.info(String.format("       Authors: %s", authorString));
				} else {
					logger.info(String.format("                %s", authorString));
				}
			}

			try {
				IAlgorithmDefinition[] algos = moduleInfo.getAlgorithms();
				for(int j = 0; j < algos.length; ++j) {
					if(j == 0) {
						logger.info(String.format("    Algorithms: %s", algos[j].getPrettyName()));
					} else {
						logger.info(String.format("                %s", algos[j].getPrettyName()));
					}
				}
			} catch(Throwable e) {
				logger.error("Error enumerating algorithms", e);
			}
		}
	}

	// <editor-fold defaultstate="collapsed" desc="Debugging">
	public final void debugf(Throwable e) {
		if(!this._debugging) {
			return;
		}

		StringWriter w = new StringWriter();
		e.printStackTrace(new PrintWriter(w));
		debugf("%s", e.getMessage());
		debugf("%s", w.toString());
	}

	public synchronized void debugf(String fmt, Object... args) {
		if(!this._debugging) {
			return;
		}

		boolean forceStdout = false;
		String s = null;
		try {
			s = String.format(fmt, args);
		} catch(IllegalFormatException e) {
			s = String.format("Error logging message with bad format string \"%s\"", fmt);
		}

		try {
			_debug(s.replace("[\r\n][\r\n]*", ""));
		} catch(java.lang.Error e) {
			/* This happens sometimes due to Kepler's listen window not being thread-safe. */
			forceStdout = true;
			s = String.format("Unable to write message to debug log: %s. Message is as follows: %s", e.getMessage(), s);
		}

		if(forceStdout) {
			/* Bypass whatever Kepler's done to stdout. */
			PrintStream stdout = new PrintStream(new FileOutputStream(FileDescriptor.out));
			stdout.printf("%s\n", s);
			stdout.flush();
		}
	}
	// </editor-fold>
}

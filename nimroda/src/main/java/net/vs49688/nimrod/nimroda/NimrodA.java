/*
 * Copyright (C) 2016 Zane van Iperen
 * All rights reserved.
 * 
 * NOTICE: This code may not be used unless explicit permission
 * is obtained from Zane van Iperen.
 * 
 * CONTACT: zane@zanevaniperen.com
 */
package net.vs49688.nimrod.nimroda;

import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimParameter;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import java.io.*;
import java.util.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import net.vs49688.cio.CIOContext;

/**
 * A wrapper around the Nimrod/A library.
 *
 * @author zane
 */
public class NimrodA {

	public enum OptimisationState {
		Stopped,
		WaitingForBatch,
		Finished
	}

	private static final Map<String, Object> COMPAT_CLASS_OPTIONS;

	static {
		COMPAT_CLASS_OPTIONS = new HashMap<>();
		//m_sCompatClassOptions.put(Library.OPTION_STRUCTURE_ALIGNMENT, Structure.ALIGN_NONE);
	}

	private static final int SIZEOF_DOUBLE = Native.getNativeSize(Double.TYPE);

	private static boolean m_sLibraryInit = false;
	private static INimrodA m_sNimrodA;
	static final HashMap<NativeAlgorithm, NimAlgorithm> AVAILABLE_ALGORITHMS = new HashMap<>();

	private final SetOfParams m_SetOfParams;
	private final Pointer m_Instance;
	private final INimrodReceiver m_Receiver;

	private OptimisationState m_State;
	private NimBatch.ByReference m_CurrentRawBatch;
	private PointContainer[] m_CurrentBatch;
	private NimOptimStatus m_RawOptimStatus;
	private OptimResult m_OptimResult;

	/* For the love of all things holy, do NOT delete this until release() has been called. */
	@SuppressWarnings("unused")
	private NimConfig m_RawConfig;
	@SuppressWarnings("unused")
	private NimCallbacks.ByReference m_RawCallbacks;

	public static void initLibrary() throws UnsatisfiedLinkError {
		if(m_sLibraryInit) {
			return;
		}
		Map<String, Object> opts = new HashMap<>();
		String oldJNAPath = fixLibraryPath(opts);
		// Prefix is Platform.getNativeLibraryResourcePrefix()
		// ${CLASSPATH}/win32-x86/nimroda.dll
		// ${CLASSPATH}/win32-x86-64/nimroda.dll
		// ${CLASSPATH}/linux-x86/libnimroda.so
		// ${CLASSPATH}/linux-x86-64/libnimroda.so
		// When running/debugging using gradle, $CLASSPATH = build/classes/main
		// When running in Kepler, $CLASSPATH = $KEPLER/nimrodok-2.4.0/lib
		//
		// Visual Studio Example:
		// copy "$(TargetPath)" "$CLASSPATH\win32-x86-64\nimroda.dll"
		try {
			m_sNimrodA = (INimrodA)Native.loadLibrary("nimroda", INimrodA.class, opts);
		} finally {
			System.setProperty("jna.library.path", oldJNAPath);
		}

		/* Query the available algorithms */
		IntByReference numAlgos = new IntByReference();
		for(NimAlgorithm alg : (NimAlgorithm[])m_sNimrodA.jnaQueryAlgorithms(numAlgos).toArray(numAlgos.getValue())) {
			AVAILABLE_ALGORITHMS.put(new NativeAlgorithm(alg.uniqueName, alg.prettyName, alg.multiObjective != 0), alg);
		}
		m_sLibraryInit = true;
	}

	/**
	 * Get the list of available algorithms.
	 *
	 * @return The list of available algorithms.
	 */
	public static Set<NativeAlgorithm> availableAlgorithms() {
		return Collections.unmodifiableSet(AVAILABLE_ALGORITHMS.keySet());
	}

	public NimrodA(NativeAlgorithm algo, PointContainer startingPoint, int numObjectives, int rngSeed, Map<String, String> config, INimrodReceiver receiver) throws NimrodOKException {
		if(algo == null) {
			throw new IllegalArgumentException("algo cannot be null");
		}
		if(startingPoint == null) {
			throw new IllegalArgumentException("startingPoint cannot be null");
		}
		if(numObjectives <= 0) {
			throw new IllegalArgumentException("numObjectives cannot be <= 0");
		}
		if(receiver == null) {
			throw new IllegalArgumentException("logger cannot be null");
		}
		m_Receiver = receiver;
		m_SetOfParams = startingPoint.point.setOfParams;
		NimVarbleAttrib.ByReference vars = new NimVarbleAttrib.ByReference();
		vars.initFromSetOfParams(m_SetOfParams);
		NimConfig.ByReference cfg = new NimConfig.ByReference();
		cfg.algorithm = (NimAlgorithm.ByReference)AVAILABLE_ALGORITHMS.get(algo);
		cfg.vars = vars;
		cfg.numDimensions = m_SetOfParams.size();
		cfg.numObjectives = numObjectives;
		cfg.callbacks.puts = new _PutsCallback();
		cfg.callbacks.writeStats = new _WriteStatsCallback();
		cfg.callbacks.user = null;
		cfg.debug = 6;
		cfg.rngSeed = rngSeed;
		cfg.customConfig = createConfigFromMap(config);
		cfg.startingPoint = point2NimPoint(null, startingPoint);
		if((m_Instance = m_sNimrodA.jnaInit(cfg)) == null) {
			throw new NimrodOKException("Nimrod/A initialisation failed.");
		}
		m_RawConfig = cfg;
		m_State = intToState(m_sNimrodA.jnaGetState(m_Instance));
	}

	public NimrodA(CIOContext ctx, INimrodReceiver receiver) throws NimrodOKException, IOException {
		if(ctx == null) {
			throw new IllegalArgumentException("ctx cannot be null");
		}

		if(receiver == null) {
			throw new IllegalArgumentException("receiver cannot be null");
		}

		m_Receiver = receiver;

		m_RawCallbacks = new NimCallbacks.ByReference();
		m_RawCallbacks.puts = new _PutsCallback();
		m_RawCallbacks.writeStats = new _WriteStatsCallback();
		m_RawCallbacks.user = null;

		if((m_Instance = m_sNimrodA.jnaRestore(m_RawCallbacks, ctx.getNativeContext())) == null) {
			throw new NimrodOKException("Nimrod/A initialisation failed.");
		}
		m_State = intToState(m_sNimrodA.jnaGetState(m_Instance));

		m_SetOfParams = createSOP(m_sNimrodA.jnaGetDimensionality(m_Instance), m_sNimrodA.jnaGetVars(m_Instance));
		resyncState(m_State);
	}

	private static SetOfParams createSOP(int dims, NimVarbleAttrib.ByReference vars) {
		OptimParameter[] params = new OptimParameter[dims];
		for(int i = 0; i < params.length; ++i) {
			params[i] = new OptimParameter(
					new String(vars.names, i * NimDefinitions.MAX_NAME_LENGTH, NimDefinitions.MAX_NAME_LENGTH, Charset.forName("US-ASCII")),
					vars.lowerBound[i],
					vars.upperBound[i],
					0
			);
		}

		return new SetOfParams(params);
	}

	public static NimKVPair.ByReference createConfigFromMap(Map<String, String> map) {
		if(map == null) {
			return null;
		}
		NimKVPair.ByReference c = new NimKVPair.ByReference();
		NimKVPair[] cfg = (NimKVPair[])c.toArray(map.size() + 1);
		int i = 0;
		for(String key : map.keySet()) {
			cfg[i].set(key, map.get(key));
			++i;
		}
		cfg[i].set(null, null);
		return c;
	}

	public void release() {
		m_sNimrodA.jnaRelease(m_Instance);
		m_RawConfig = null;
	}

	/**
	 * Emulate the behaviour of the newer JNA versions in Kepler's old one.
	 *
	 * @param outOpts Any specific compatibility options that have to be applied are appended here.
	 * @return The old JNA library path.
	 */
	private static String fixLibraryPath(Map<String, Object> outOpts) {
		if(outOpts == null) {
			throw new IllegalArgumentException("outOpts cannot be null");
		}
		String nativePrefix = Platform.getNativeLibraryResourcePrefix();
		String oldPath = System.getProperty("jna.library.path");
		if(oldPath == null) {
			oldPath = "";
		}

		/* Append the platform prefix to all directories in the classpath
		 * and set it to the JNA load path. */
		StringBuilder jnaPathBuilder = new StringBuilder();
		URL[] cpUrls = ((URLClassLoader)NimrodA.class.getClassLoader()).getURLs();
		for(URL url : cpUrls) {
			//System.err.printf("CPURL: %s\n", url);
			try {
				jnaPathBuilder.append(Paths.get(url.toURI()).resolve(nativePrefix).toString());
				jnaPathBuilder.append(File.pathSeparatorChar);
			} catch(URISyntaxException e) {
			}
		}
//		for(String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
//			/* Ignore .jar files. */
//			if(s.toLowerCase().endsWith(".jar")) {
//				continue;
//			}
//
//			jnaPathBuilder.append(s);
//			jnaPathBuilder.append(File.separatorChar);
//			jnaPathBuilder.append(nativePrefix);
//			jnaPathBuilder.append(File.pathSeparatorChar);
//		}

		/* Remove the trailing path separator */
		jnaPathBuilder.deleteCharAt(jnaPathBuilder.length() - 1);
		System.setProperty("jna.library.path", jnaPathBuilder.toString());
		//System.err.printf("jna.library.path set to %s\n", jnaPathBuilder.toString());
		outOpts.putAll(COMPAT_CLASS_OPTIONS);
		return oldPath;
	}

	/**
	 * Resynchronise the list of points with the raw Nimrod/A Batch.
	 *
	 * @param points The list of points.
	 * @param rawBatch The raw Nimrod/A batch.
	 * @return If all the points have finished evaluating, true is returned. Otherwise, false.
	 */
	private static boolean resyncBatch(PointContainer[] points, NimBatch rawBatch) {
		for(int i = 0; i < points.length; ++i) {
			PointContainer point = points[i];
			if(!point.hasAllObjectives()) {
				return false;
			}
		}
		NimPoint.ByReference[] rawPoints = rawBatch.getPoints();
		for(int i = 0; i < points.length; ++i) {
			PointContainer point = points[i];
			for(int j = 0; j < rawPoints[i].numObjectives.intValue(); ++j) {
				rawPoints[i].objectives.setDouble(j * SIZEOF_DOUBLE, point.objectives[j]);
			}
			rawPoints[i].status = NimDefinitions.JOB_COMPLETE;
			rawPoints[i].write();
		}
		return true;
	}

	/**
	 * Fire the Nimrod/A state machine.
	 *
	 * @return The state after firing.
	 */
	public OptimisationState fire() {
		if(m_State == OptimisationState.Finished) {
			return OptimisationState.Finished;
		}

		/* Only continue if the batch has finished evaluating. */
		if(m_State == OptimisationState.WaitingForBatch) {
			if(!resyncBatch(m_CurrentBatch, m_CurrentRawBatch)) {
				return OptimisationState.WaitingForBatch;
			}
		} else {
			m_CurrentRawBatch = null;
			m_CurrentBatch = null;
		}
		m_RawOptimStatus = null;
		m_OptimResult = null;
		OptimisationState state = intToState(m_sNimrodA.jnaOptimise(m_Instance));

		resyncState(state);
		return state;
	}

	private void resyncState(OptimisationState state) {
		/* If we want evaluations, get the batch. */
		if(state == OptimisationState.WaitingForBatch) {
			m_CurrentRawBatch = m_sNimrodA.jnaGetBatch(m_Instance);
			NimPoint.ByReference[] rawPoints = m_CurrentRawBatch.getPoints();
			m_CurrentBatch = new PointContainer[rawPoints.length];
			for(int i = 0; i < m_CurrentBatch.length; ++i) {
				m_CurrentBatch[i] = nimPoint2Point(rawPoints[i], m_SetOfParams);

				/* nimPoint2Point() copies the objective values, which in this case
				 * MUST be null, as we use it to determine if we've been evaluated. */
				Double[] objectives = m_CurrentBatch[i].objectives;
				for(int j = 0; j < objectives.length; ++j) {
					objectives[j] = null;
				}
			}
		}
		/*else if(state == OptimisationState.Finished) {
			m_RawOptimStatus = m_sNimrodA.jnaGetOptimStatus(m_Instance);
			m_OptimResult = new OptimResult(this, m_RawOptimStatus);
		}*/
		m_RawOptimStatus = m_sNimrodA.jnaGetOptimStatus(m_Instance);
		m_OptimResult = new OptimResult(this, m_RawOptimStatus);
		m_State = state;
	}

	public OptimisationState getState() {
		return m_State;
	}

	public static OptimisationState intToState(int raw) {
		switch(raw) {
			case 0:
				return OptimisationState.Stopped;
			case 1:
				return OptimisationState.WaitingForBatch;
			case 3:
				return OptimisationState.Finished;
		}
		return null;
	}

	public SetOfParams getSetOfParams() {
		return m_SetOfParams;
	}

	public PointContainer[] getBatch() {
		if(m_State != OptimisationState.WaitingForBatch || m_CurrentBatch == null) {
			throw new IllegalStateException();
		}
		return m_CurrentBatch;
	}

	public OptimResult getResult() {
		if(m_OptimResult == null) {
			throw new IllegalStateException();
		}
		return m_OptimResult;
	}

	public void save(CIOContext ctx) throws IOException {
		if(m_sNimrodA.jnaSave(m_Instance, ctx.getNativeContext()) != 0) {
			throw new IOException();
		}
	}

	/**
	 * Get the Nimrod/A instance. Use this at your own peril.
	 *
	 * @return Get the Nimrod/A instance.
	 */
	public Pointer getInstance() {
		return m_Instance;
	}

	/**
	 * Get the raw Nimrod/A interface. Use this at your own peril. If initLibrary() has not been previously called, this
	 * will call it.
	 *
	 * @return The raw Nimrod/A interface.
	 */
	public static INimrodA getInterface() throws UnsatisfiedLinkError {
		initLibrary();
		return m_sNimrodA;
	}

	// <editor-fold defaultstate="collapsed" desc="Point Conversions">
	public static NimPoint.ByReference point2NimPoint(Pointer state, PointContainer pc) {
		NimPoint.ByReference pt = new NimPoint.ByReference();
		point2NimPoint(state, pc, pt);
		return pt;
	}

	private static void point2NimPoint(Pointer state, PointContainer pc, NimPoint pt) {
		pt.dimensionality = new SizeT(pc.point.dimensionality);
		pt.coords = new Memory(SIZEOF_DOUBLE * pc.point.dimensionality);
		pt.status = 0;
		pt.numObjectives = new SizeT(pc.objectives.length);
		pt.objectives = new Memory(SIZEOF_DOUBLE * pc.objectives.length);
		for(int i = 0; i < pc.point.dimensionality; ++i) {
			long offset = i * SIZEOF_DOUBLE;
			pt.coords.setDouble(offset, pc.point.coords[i]);
		}
		for(int i = 0; i < pc.objectives.length; ++i) {
			pt.objectives.setDouble(i * SIZEOF_DOUBLE, pc.objectives[i] == null ? NimDefinitions.HUGEISH : pc.objectives[i]);
		}
		pt.write();
	}

	public static PointContainer nimPoint2Point(NimPoint point, SetOfParams sop) {
		OptimPoint pt = new OptimPoint(sop);
		for(int i = 0; i < pt.dimensionality; ++i) {
			pt.coords[i] = point.coords.getDouble(i * SIZEOF_DOUBLE);
		}
		PointContainer pc = new PointContainer(pt, point.numObjectives.intValue());
		for(int i = 0; i < point.numObjectives.intValue(); ++i) {
			pc.objectives[i] = point.objectives.getDouble(i * SIZEOF_DOUBLE);
		}
		return pc;
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="Callback Stubs">
	private class _PutsCallback implements INimrodA.IPuts {

		@Override
		public void puts(Pointer state, String s) {
			NimrodA.this.m_Receiver.logf(null, "%s", s);
		}

	}

	private class _WriteStatsCallback implements INimrodA.IWriteStats {

		@Override
		public void writeStats(Pointer instance, NimKVPair.ByReference stats, SizeT count) {
			NimKVPair.ByReference[] rawStats = (NimKVPair.ByReference[])stats.toArray(count.intValue());
			INimrodReceiver.Stat[] _stats = new INimrodReceiver.Stat[rawStats.length];
			for(int i = 0; i < _stats.length; ++i) {
				_stats[i] = new INimrodReceiver.Stat(rawStats[i].key, rawStats[i].value);
			}
			m_Receiver.incomingStats(null, _stats);
		}

	}
	// </editor-fold>
}

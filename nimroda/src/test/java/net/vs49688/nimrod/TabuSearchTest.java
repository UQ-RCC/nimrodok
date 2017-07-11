package net.vs49688.nimrod;

import au.edu.uq.rcc.nimrod.optim.BaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Batch;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.OptimParameter;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.*;
import java.nio.*;
import java.util.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import net.vs49688.cio.CIOContext;
import net.vs49688.nimrod.nimroda.INimrodA;
import net.vs49688.nimrod.nimroda.NativeAlgorithm;
import net.vs49688.nimrod.nimroda.NimAlgorithm;
import net.vs49688.nimrod.nimroda.NimConfig;
import net.vs49688.nimrod.nimroda.NimDefinitions;
import net.vs49688.nimrod.nimroda.NimKVPair;
import net.vs49688.nimrod.nimroda.NimPoint;
import net.vs49688.nimrod.nimroda.NimVarbleAttrib;
import net.vs49688.nimrod.nimroda.NimrodA;
import net.vs49688.nimrod.nimroda.OptimResult;
import net.vs49688.nimrod.nimroda.SizeT;
import org.junit.*;

public class TabuSearchTest {

	private static final int ZDT4_DIMENSIONS = 10;
	private static final int ZDT4_OBJECTIVES = 2;

	public static void evalBatch(Batch batch) {
		for(PointContainer pc : batch.points) {
			pc.objectives[0] = pc.point.coords[0];
			pc.objectives[1] = ZDT4.zdt4_f(pc.point.coords);
		}
	}

	public static NativeAlgorithm getMOTS2() {
		NativeAlgorithm algo = ZDT4.getMOTS2();
		Assert.assertNotNull("Unable to query MOTS2 algorithm from Nimrod/A", algo);
		return algo;
	}

	private INimrodReceiver m_Logger = new INimrodReceiver() {
		@Override
		public void logf(BaseAlgorithm instance, String fmt, Object... args) {
			System.out.printf(fmt, args);
		}

		@Override
		public void logf(BaseAlgorithm instance, Throwable e) {
			e.printStackTrace(System.err);
		}

		@Override
		public void incomingStats(BaseAlgorithm instance, INimrodReceiver.Stat[] stats) {

		}

	};

	private double[][] zdt4Optimise(double[] start, int iterations) throws Exception {
		Assert.assertEquals(ZDT4_DIMENSIONS, start.length);

		NimrodA.initLibrary();

		NativeAlgorithm algo = getMOTS2();
		SetOfParams sop = ZDT4.getSOP();

		PointContainer pc = new PointContainer(new OptimPoint(sop), ZDT4_OBJECTIVES);

		for(int i = 0; i < pc.point.dimensionality; ++i) {
			pc.point.coords[i] = start[i];
		}

		NimrodA nimrod = new NimrodA(algo, pc, ZDT4_OBJECTIVES, 0, ZDT4.getConfig(iterations), m_Logger);

		for(;;) {
			NimrodA.OptimisationState state = nimrod.fire();
			if(state == NimrodA.OptimisationState.WaitingForBatch) {
				Batch b = new Batch(null, nimrod.getBatch());
				evalBatch(b);
			} else if(state == NimrodA.OptimisationState.Finished) {
				break;
			}
		}

		OptimResult result = nimrod.getResult();
		nimrod.release();

		List<PointContainer> pareto = result.getResults();
		double[][] k = new double[pareto.size()][10];
		for(int i = 0; i < k.length; ++i) {
			for(int j = 0; j < k[i].length; ++j) {
				k[i][j] = pareto.get(i).point.coords[j];
			}
		}

		return k;
	}

	public static int pid = NativeInterfaceTest.pid;

	private static NimAlgorithm.ByReference getRawAlgorithm(INimrodA nimroda) {
		IntByReference numAlgos = new IntByReference();
		NimAlgorithm.ByReference p = nimroda.jnaQueryAlgorithms(numAlgos);
		NimAlgorithm.ByReference[] algorithms = (NimAlgorithm.ByReference[])p.toArray(numAlgos.getValue());

		NimAlgorithm.ByReference mots2 = null;
		for(NimAlgorithm.ByReference algo : algorithms) {
			if(algo.uniqueName.equals("MOTS2")) {
				mots2 = algo;
				break;
			}
		}

		Assert.assertNotNull("Unable to find MOTS2 algorithm", mots2);

		return mots2;
	}

	private static final int SIZEOF_DOUBLE = Native.getNativeSize(Double.TYPE);

	private static NimVarbleAttrib.ByReference createVarsRaw() throws UnsupportedEncodingException {
		NimVarbleAttrib.ByReference vars = new NimVarbleAttrib.ByReference();

		for(int i = 0; i < 10; ++i) {
			if(i == 0) {
				vars.lowerBound[i] = 0.0;
				vars.upperBound[i] = 1.0;
			} else {
				vars.lowerBound[i] = -5.0;
				vars.upperBound[i] = 5.0;
			}

			byte[] nameBytes = String.format("x%d", i + 1).getBytes("US-ASCII");
			ByteBuffer bb = ByteBuffer.wrap(vars.names, i * NimDefinitions.MAX_NAME_LENGTH, NimDefinitions.MAX_NAME_LENGTH);
			bb.put(nameBytes);

			vars.range[i] = vars.upperBound[i] - vars.lowerBound[i];
		}
		return vars;
	}

	private static NimPoint.ByReference createRawPoint(Pointer state, double[] values) {
		NimPoint.ByReference point = new NimPoint.ByReference();

		point.dimensionality = new SizeT(ZDT4_DIMENSIONS);
		point.coords = new Memory(ZDT4_DIMENSIONS * SIZEOF_DOUBLE);
		point.status = 0;
		point.numObjectives = new SizeT(ZDT4_OBJECTIVES);
		point.objectives = new Memory(ZDT4_OBJECTIVES * SIZEOF_DOUBLE);

		for(int i = 0; i < ZDT4_DIMENSIONS; ++i) {
			long offset = i * SIZEOF_DOUBLE;
			point.coords.setDouble(offset, values[i]);
		}

		for(int i = 0; i < ZDT4_OBJECTIVES; ++i) {
			point.objectives.setDouble(i * SIZEOF_DOUBLE, NimDefinitions.HUGEISH);
		}

		return point;
	}

	public static double[] arrayFromPointer(Pointer ptr, int count) {
		double[] coords = new double[count];
		for(int i = 0; i < count; ++i) {
			coords[i] = ptr.getDouble(i * SIZEOF_DOUBLE);
		}
		return coords;
	}

	@Test
	public void zdt4VariableConversionEquivalenceTest() throws Exception {
		NimVarbleAttrib rawVars = createVarsRaw();
		SetOfParams sop = ZDT4.getSOP();

		Assert.assertEquals(ZDT4_DIMENSIONS, sop.size());

		for(int i = 0; i < sop.size(); ++i) {
			OptimParameter param = sop.get(i);

			Assert.assertEquals(rawVars.upperBound[i], param.max, 0.00001);
			Assert.assertEquals(rawVars.lowerBound[i], param.min, 0.00001);
			Assert.assertEquals(rawVars.range[i], param.max - param.min, 0.00001);
		}
	}

	@Test
	public void rawInitialisationTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		NimConfig.ByReference cfg = new NimConfig.ByReference();
		cfg.algorithm = getRawAlgorithm(nimroda);
		cfg.vars = createVarsRaw();
		cfg.numDimensions = ZDT4_DIMENSIONS;
		cfg.numObjectives = ZDT4_OBJECTIVES;
		cfg.callbacks.puts = new _PutsCallback();
		cfg.callbacks.writeStats = new _WriteStatsCallback();
		cfg.debug = 6;
		cfg.rngSeed = 0;
		cfg.customConfig = NimrodA.createConfigFromMap(ZDT4.getConfig(10));
		cfg.startingPoint = createRawPoint(null, ZDT4.INPUT_POINTS[0]);

		Pointer state = nimroda.jnaInit(cfg);
		if(state == null) {
			throw new Exception("Nimrod/A initialisation failed.");
		}

		nimroda.jnaRelease(state);
	}

	@Test
	public void resumeOptimisationTest() throws Exception {
		JsonElement jsob = new JsonParser().parse(ZDT4.MOTS2_STATE_DUMP_WAITING);
		CIOContext ctx = new CIOContext(NimrodA.getInterface(), "nimroda", false, new GSONReadOperations(jsob));

		NimrodA nimrod = new NimrodA(ctx, m_Logger);

		for(;;) {
			NimrodA.OptimisationState state = nimrod.fire();
			if(state == NimrodA.OptimisationState.WaitingForBatch) {
				Batch b = new Batch(null, nimrod.getBatch());
				evalBatch(b);
			} else if(state == NimrodA.OptimisationState.Finished) {
				break;
			}
		}

		OptimResult result = nimrod.getResult();
		nimrod.release();
	}

//	@Test
//	public void rawOptimisationTest() throws Exception {
//		INimrodA nimroda = NimrodA.getInterface();
//
//		MemoryManager mm = new MemoryManager();
//
//		NimConfig.ByReference cfg = new NimConfig.ByReference();
//		cfg.algorithm = getRawAlgorithm(nimroda);
//		cfg.vars = createVarsRaw();
//		cfg.numDimensions = ZDT4_DIMENSIONS;
//		cfg.numObjectives = ZDT4_OBJECTIVES;
//		cfg.puts = new _PutsCallback();
//		cfg.malloc = new _MallocCallback(mm);
//		cfg.free = new _FreeCallback(mm);
//		cfg.debug = 6;
//		cfg.rngSeed = 0;
//		cfg.customConfig = NimrodA.createConfigFromMap(getConfig(10));
//		cfg.startingPoint = createRawPoint(null, mm, ZDT4SamplePoints.INPUT_POINTS[0]);
//
//		Pointer instance = nimroda.jnaInit(cfg);
//		if(instance == null) {
//			mm.release(null, cfg.startingPoint.coords);
//			mm.release(null, cfg.startingPoint.multipleObjectives);
//			throw new Exception("Nimrod/A initialisation failed.");
//		}
//		mm.release(null, cfg.startingPoint.coords);
//		mm.release(null, cfg.startingPoint.multipleObjectives);
//
//		for(;;) {
//			NimrodA.OptimisationState state = NimrodA.intToState(nimroda.jnaOptimise(instance));
//			if(state == NimrodA.OptimisationState.WaitingForBatch) {
//				 NimBatch.ByReference batch = nimroda.jnaGetBatch(instance);
//				 batch.
//			}
//			
//		}
//		
//		//NimOptimStatus status = nimroda.jnaOptimise(state, point, 1.0f);
//
//		double[] results = null;
//		if(status.howOptEnded == 0) {
//			results = arrayFromPointer(status.result.coords, status.result.dimensionality);
//			mm.release(instance, status.result.coords);
//			mm.release(instance, status.result.multipleObjectives);
//		}
//
//		nimroda.jnaRelease(instance);
//
//		mm.dump(System.out);
//
//		Assert.assertArrayEquals(ZDT4SamplePoints.OPTIMAL_POINTS[0], results, 0.00001);
//	}
//	@Test
//	public void intenseRawOptimisationTest() throws Exception {
//		INimrodA nimroda = NimrodA.getInterface();
//
//		MemoryManager mm = new MemoryManager();
//
//		NimConfig.ByReference cfg = new NimConfig.ByReference();
//		cfg.algorithm = getRawAlgorithm(nimroda);
//		cfg.vars = createVarsRaw();
//		cfg.numDimensions = ZDT4_DIMENSIONS;
//		cfg.numObjectives = ZDT4_OBJECTIVES;
//		cfg.puts = new _PutsCallback();
//		cfg.malloc = new _MallocCallback(mm);
//		cfg.free = new _FreeCallback(mm);
//		cfg.debug = 6;
//		cfg.rngSeed = 0;
//		cfg.customConfig = NimrodA.createConfigFromMap(getConfig(10));
//		cfg.startingPoint = createRawPoint(null, mm, ZDT4SamplePoints.INPUT_POINTS[0]);
//
//		Pointer state = nimroda.jnaInit(cfg);
//		if(state == null) {
//			throw new Exception("Nimrod/A initialisation failed.");
//		}
//		NimOptimStatus status = nimroda.jnaOptimise(state, point, 1.0f);
//
//		double[] results = null;
//		if(status.howOptEnded == 0) {
//			results = arrayFromPointer(status.result.coords, status.result.dimensionality);
//			mm.release(state, status.result.coords);
//			mm.release(state, status.result.multipleObjectives);
//		}
//
//		mm.release(null, cfg.startingPoint.coords);
//		mm.release(null, cfg.startingPoint.multipleObjectives);
//
//		nimroda.jnaRelease(state);
//
//		mm.dump(System.out);
//
//		Assert.assertArrayEquals(ZDT4SamplePoints.OPTIMAL_POINTS[0], results, 0.00001);
//	}
	@Test
	public void rawLoop10() throws Exception {
		/* If this works and wrapperLoop10() fails, then
		 * you have heap corruption. Make sure you're keeping
		 * references to any objects you're passing to C. */
		for(int i = 0; i < 10; ++i) {
			rawInitialisationTest();
		}
	}

	@Test
	public void wrapperLoop10() throws Exception {
		for(int i = 0; i < 10; ++i) {
			zdt4Test1();
		}
	}

//	@Test
//	public void zdt4Test0() throws Exception {
//		testZDT4(0);
//	}
	@Test
	public void zdt4Test1() throws Exception {
		testZDT4(1);
	}
//
//	@Test
//	public void zdt4Test2() throws Exception {
//		testZDT4(2);
//	}

	private void testZDT4(int i) throws Exception {
		double[][] results = zdt4Optimise(ZDT4.INPUT_POINTS[i], ZDT4.ITERATIONS[i]);

		Assert.assertEquals(ZDT4.OPTIMAL_POINTS[i].length, results.length);

		for(int j = 0; j < results.length; ++j) {
			Assert.assertArrayEquals(ZDT4.OPTIMAL_POINTS[i][j], results[j], 0.00001);
		}
	}

	private class _PutsCallback implements INimrodA.IPuts {

		@Override
		public void puts(Pointer state, String s) {
			System.out.print(s);
		}

	}

	private class _WriteStatsCallback implements INimrodA.IWriteStats {

		@Override
		public void writeStats(Pointer instance, NimKVPair.ByReference stats, SizeT count) {

		}

	}
}

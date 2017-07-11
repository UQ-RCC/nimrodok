package net.vs49688.nimrod;

import com.sun.jna.*;
import junit.framework.Assert;
import net.vs49688.nimrod.nimroda.INimrodA;
import net.vs49688.nimrod.nimroda.NimAlgorithm;
import net.vs49688.nimrod.nimroda.NimBatch;
import net.vs49688.nimrod.nimroda.NimConfig;
import net.vs49688.nimrod.nimroda.NimKVPair;
import net.vs49688.nimrod.nimroda.NimDefinitions;
import net.vs49688.nimrod.nimroda.NimOptimStatus;
import net.vs49688.nimrod.nimroda.NimPoint;
import net.vs49688.nimrod.nimroda.NimVarbleAttrib;
import net.vs49688.nimrod.nimroda.NimrodA;
import net.vs49688.nimrod.nimroda.SizeT;
import org.junit.Test;

public class NativeInterfaceTest {

	private static int getPID() {
		try {
			java.lang.management.RuntimeMXBean runtime
					= java.lang.management.ManagementFactory.getRuntimeMXBean();
			java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
			jvm.setAccessible(true);
			sun.management.VMManagement mgmt
					= (sun.management.VMManagement) jvm.get(runtime);
			java.lang.reflect.Method pid_method
					= mgmt.getClass().getDeclaredMethod("getProcessId");
			pid_method.setAccessible(true);

			int pid = (Integer) pid_method.invoke(mgmt);
			System.err.printf("PID is %d\n", pid);
			return pid;
		} catch(Exception e) {
			return -1;
		}
	}

	public static int pid = getPID();

	private int getStructureSize(Class<? extends Structure> s) {
		try {
			return s.newInstance().size();
		} catch(InstantiationException | IllegalAccessException e) {
			return -1;
		}
	}

	@Test
	public void structureSizesTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		/* Check the sizes of all the data structures. */
		Assert.assertEquals("Invalid sizeof(NimAlgorithm)", getStructureSize(NimAlgorithm.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_ALGORITHM));
		Assert.assertEquals("Invalid sizeof(NimConfig)", getStructureSize(NimConfig.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_CONFIG));
		Assert.assertEquals("Invalid sizeof(NimCustomConfig)", getStructureSize(NimKVPair.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_CUSTOM_CONFIG));
		Assert.assertEquals("Invalid sizeof(NimOptimStatus)", getStructureSize(NimOptimStatus.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_OPTIM_STATUS));
		Assert.assertEquals("Invalid sizeof(NimPoint)", getStructureSize(NimPoint.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_POINT));
		Assert.assertEquals("Invalid sizeof(NimVarbleAttrib)", getStructureSize(NimVarbleAttrib.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_VARBLE_ATTRIB));
		Assert.assertEquals("Invalid sizeof(NimBatch)", getStructureSize(NimBatch.class), nimroda.jnaTestSizeOf(NimDefinitions.TESTPARAM_BATCH));
	}

	@Test
	public void optimStatusMarshallingTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		NimOptimStatus status = new NimOptimStatus();
		nimroda.jnaTestGetSampleStructure(NimDefinitions.TESTPARAM_OPTIM_STATUS, status.getPointer());
		status.read();

		Assert.assertEquals(200, status.code);
		Assert.assertEquals(1, status.state);
		Assert.assertEquals("Sample Optim_Status message", status.getMessage());
		Assert.assertEquals(0xDEADBABEL, Pointer.nativeValue(status.results));
		Assert.assertEquals(101010101, status.resultCount.intValue());
	}

	@Test
	public void batchMarshallingTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		NimBatch batch = new NimBatch();
		nimroda.jnaTestGetSampleStructure(NimDefinitions.TESTPARAM_BATCH, batch.getPointer());
		batch.read();

		Assert.assertEquals(69, batch.numPoints.intValue());
		Assert.assertEquals(0xDEADBABEL, Pointer.nativeValue(batch.points));
	}

	@Test
	public void pointMarshallingTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		NimPoint pt = new NimPoint();
		nimroda.jnaTestGetSampleStructure(NimDefinitions.TESTPARAM_POINT, pt.getPointer());
		pt.read();

		Assert.assertEquals(10, pt.dimensionality.intValue());
		Assert.assertEquals(0xDEADBABEL, Pointer.nativeValue(pt.coords));
		Assert.assertEquals(1, pt.status);
		Assert.assertEquals(2, pt.numObjectives.intValue());
		Assert.assertEquals(0xDEADBEEFL, Pointer.nativeValue(pt.objectives));
	}

	@Test
	public void customConfigMarshallingTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		NimKVPair cfg = new NimKVPair();
		nimroda.jnaTestGetSampleStructure(NimDefinitions.TESTPARAM_CUSTOM_CONFIG, cfg.getPointer());
		cfg.read();

		Assert.assertEquals("key", cfg.key);
		Assert.assertEquals("value", cfg.value);
	}

	@Test
	public void algorithmMarshallingTest() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		NimAlgorithm algo = new NimAlgorithm();
		nimroda.jnaTestGetSampleStructure(NimDefinitions.TESTPARAM_ALGORITHM, algo.getPointer());
		algo.read();

		Assert.assertEquals("SAMPLEALGORITHM", algo.uniqueName);
		Assert.assertEquals("Sample Algorithm", algo.prettyName);
		Assert.assertEquals(100, algo.multiObjective);
		Assert.assertEquals(0xDEADBABEL, Pointer.nativeValue(algo.init));
		Assert.assertEquals(0xCAFEBABEL, Pointer.nativeValue(algo.optimise));
		Assert.assertEquals(0xDEADBEEFL, Pointer.nativeValue(algo.deinit));
	}

	@Test
	public void mallocRape() throws Exception {
		INimrodA nimroda = NimrodA.getInterface();

		Pointer[] ptrs = new Pointer[1000];

		for(int i = 0; i < ptrs.length; ++i) {
			SizeT size = new SizeT((long) (Math.random() * 1048576) + 1);
			ptrs[i] = nimroda.jnaTestMalloc(size);
			nimroda.jnaTestOneFill(ptrs[i], size);
		}

		for(int i = 0; i < ptrs.length; ++i) {
			nimroda.jnaTestFree(ptrs[i]);
		}
	}
}

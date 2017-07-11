package net.vs49688.nimrod;

import com.sun.jna.*;
import junit.framework.Assert;
import net.vs49688.nimrod.nimroda.INimrodA;
import net.vs49688.nimrod.nimroda.NimAlgorithm;
import net.vs49688.nimrod.nimroda.NimBatch;
import net.vs49688.nimrod.nimroda.NimCallbacks;
import net.vs49688.nimrod.nimroda.NimConfig;
import net.vs49688.nimrod.nimroda.NimKVPair;
import net.vs49688.nimrod.nimroda.NimDefinitions;
import net.vs49688.nimrod.nimroda.NimOptimStatus;
import net.vs49688.nimrod.nimroda.NimPoint;
import net.vs49688.nimrod.nimroda.NimVarbleAttrib;
import net.vs49688.nimrod.nimroda.NimrodA;
import net.vs49688.nimrod.nimroda.SizeT;
import org.junit.Test;

public class CrashTest {

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

	@Test
	public void structureSizesTest() throws Exception {
		//INimrodA nimroda = NimrodA.getInterface();
		//NimCallbacks cbk = NimCallbacks.class.newInstance();
		NimConfig cfg = NimConfig.class.newInstance();
		
		
		int x = 0;
	}
}

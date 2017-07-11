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

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import net.vs49688.cio.ICIO;
import net.vs49688.cio.NativeCIOContext;

public interface INimrodA extends ICIO {

	NimAlgorithm.ByReference jnaQueryAlgorithms(IntByReference size);

	Pointer jnaInit(NimConfig.ByReference config);
	void jnaRelease(Pointer instance);
	int jnaOptimise(Pointer instance);
	NimBatch.ByReference jnaGetBatch(Pointer instance);
	int jnaGetState(Pointer instance);
	NimOptimStatus.ByReference jnaGetOptimStatus(Pointer instance);
	NimVarbleAttrib.ByReference jnaGetVars(Pointer instance);
	int jnaGetDimensionality(Pointer instance);
	int jnaGetObjectiveCount(Pointer instance);

	long jnaHash(Pointer data, SizeT size);
	long jnaHashDouble(double val);
	long jnaHashDoubleArray(DoubleByReference p, SizeT count);

	int jnaSave(Pointer instance, NativeCIOContext.ByReference ctx);
	Pointer jnaRestore(NimCallbacks.ByReference callbacks, NativeCIOContext.ByReference ctx);

	/* Debug functions */
	void jnaDumpVarbleAttribs(NimVarbleAttrib.ByReference vars);
	void jnaDumpStateVarbleAttribs(Pointer instance);
	int jnaTestSizeOf(int structure);
	void jnaTestGetSampleStructure(int structure, Pointer ptr);
	Pointer jnaTestMalloc(SizeT bytes);
	void jnaTestFree(Pointer ptr);
	void jnaTestOneFill(Pointer ptr, SizeT size);

	public interface IPuts extends Callback {
		void puts(Pointer instance, String s);
	}

	public interface IWriteStats extends Callback {
		void writeStats(Pointer instance, NimKVPair.ByReference stats, SizeT count);
	}
}

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

import java.util.*;
import com.sun.jna.*;

public class NimConfig extends Structure {

	public static class ByReference extends NimConfig implements Structure.ByReference {
		public ByReference() {
			super();
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public NimConfig() {
		super();
		callbacks = new NimCallbacks.ByValue();
	}

	public NimConfig(Pointer p) {
		super(p);
	}

	public NimAlgorithm.ByReference algorithm;
	public NimVarbleAttrib.ByReference vars;
	public int numDimensions;
	public int numObjectives;
	public NimCallbacks.ByValue callbacks;
	public int debug;
	public int rngSeed;
	public NimKVPair.ByReference customConfig;
	public NimPoint.ByReference startingPoint;

	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS = Arrays.asList(new String[]{
		"algorithm", "vars", "numDimensions", "numObjectives",
		"callbacks", "debug", "rngSeed",
		"customConfig", "startingPoint"
	});
}

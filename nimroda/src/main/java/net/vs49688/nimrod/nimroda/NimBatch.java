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

public class NimBatch extends Structure {

	public static class ByReference extends NimBatch implements Structure.ByReference {

		public ByReference() {
			super();
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public static class ByValue extends NimBatch implements Structure.ByValue {
	}

	public NimBatch() {
		super();
	}

	public NimBatch(Pointer p) {
		super(p);
	}

	public SizeT numPoints;
	public Pointer points;
	
	public NimPoint.ByReference[] getPoints() {
		Pointer[] raw = points.getPointerArray(0, numPoints.intValue());
		NimPoint.ByReference[] points = new NimPoint.ByReference[numPoints.intValue()];
		for(int i = 0; i < points.length; ++i) {
			points[i] = new NimPoint.ByReference(raw[i]);
			points[i].read();
		}
		
		return points;
	}

	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS;

	static {
		FIELDS = Arrays.asList(new String[]{"numPoints", "points"});
	}
}

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

import au.edu.uq.rcc.nimrod.optim.OptimParameter;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import java.util.*;
import com.sun.jna.*;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * The JNA interface to struct Varble_Attrib
 *
 * @author Zane van Iperen
 */
public class NimVarbleAttrib extends Structure {

	public static class ByReference extends NimVarbleAttrib implements Structure.ByReference {

		public ByReference() {
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public static class ByValue extends NimVarbleAttrib implements Structure.ByValue {
	}

	public NimVarbleAttrib() {
		super();
	}

	public NimVarbleAttrib(Pointer p) {
		super(p);
	}

	/**
	 * Coordinates of the lower bounds of the search space.
	 */
	public double[] lowerBound = new double[NimDefinitions.MAX_DIMS];

	/**
	 * Coordinates of the upper bounds of the search space.
	 */
	public double[] upperBound = new double[NimDefinitions.MAX_DIMS];

	/**
	 * This gives 'range' (span of domain).
	 */
	public double[] range = new double[NimDefinitions.MAX_DIMS];

	/**
	 * Variable names.
	 */
	public byte[] names = new byte[NimDefinitions.MAX_DIMS * NimDefinitions.MAX_NAME_LENGTH];

	public void initFromSetOfParams(SetOfParams sop) {
		if(sop.size() >= NimDefinitions.MAX_DIMS) {
			throw new IllegalArgumentException("Too many parameters");
		}

		for(int i = 0; i < sop.size(); ++i) {
			OptimParameter param = sop.get(i);
			importParameter(param, i);
		}
	}

	private void importParameter(OptimParameter param, int index) {
		/* Copy the name. */
		byte[] nameBytes = null;
		try {
			nameBytes = param.name.getBytes("US-ASCII");
		} catch(UnsupportedEncodingException e) {
			// Should never happen
		}

		ByteBuffer bb = ByteBuffer.wrap(names, index * NimDefinitions.MAX_NAME_LENGTH, NimDefinitions.MAX_NAME_LENGTH);
		bb.put(nameBytes);

		lowerBound[index] = param.min;
		upperBound[index] = param.max;

		range[index] = Math.abs(upperBound[index] - lowerBound[index]);
	}

	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS;

	static {
		FIELDS = Arrays.asList(new String[]{
			"lowerBound", "upperBound", "range", "names"
		});
	}
}

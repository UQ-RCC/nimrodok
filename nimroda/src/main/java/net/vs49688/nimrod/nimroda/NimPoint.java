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

/**
 * The JNA interface to jna_point_t
 * @author Zane van Iperen
 */
public class NimPoint extends Structure {

	public static class ByReference extends NimPoint implements Structure.ByReference {

		public ByReference() {
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public static class ByValue extends NimPoint implements Structure.ByValue {
	}
	
	public NimPoint() {
		super();
	}

	public NimPoint(Pointer p) {
		super(p);
	}

	/**
	 * The number of numeric values.
	 */
	public SizeT dimensionality;

	/**
	 * The elements of our coordinate.
	 */
	public Pointer coords;	// double*

	public int status; // enum mapping, will fix later

	public SizeT numObjectives;
	public Pointer objectives; // double*

	public Pointer owner;

	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS;

	static {
		FIELDS = Arrays.asList(new String[]{
			"dimensionality", "coords",
			"status", "numObjectives",
			"objectives",
			"owner"
		});
	}
}

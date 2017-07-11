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

public class NimCallbacks extends Structure {

	public static class ByReference extends NimCallbacks implements Structure.ByReference {

		public ByReference() {
			super();
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public static class ByValue extends NimCallbacks implements Structure.ByValue {

		public ByValue() {
			super();
		}

		/* Having this in causes a segfault in Kepler trunk :/ */
		public ByValue(Pointer p) {
			super(p);
		}
	}

	public NimCallbacks() {
		super();
	}

	public NimCallbacks(Pointer p) {
		super(p);
	}

	public INimrodA.IPuts puts;
	public INimrodA.IWriteStats writeStats;
	public Pointer user;

	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS = Arrays.asList(new String[]{
		"puts", "writeStats", "user"
	});
}

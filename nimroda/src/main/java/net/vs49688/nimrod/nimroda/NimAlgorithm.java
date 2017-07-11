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

public class NimAlgorithm extends Structure {
	public static class ByReference extends NimAlgorithm implements Structure.ByReference {
		public ByReference() { }
		public ByReference(Pointer p) { super(p); }
	}
	
	public NimAlgorithm() {	}
	public NimAlgorithm(Pointer p) { super(p); }
	
	public String uniqueName;
	public String prettyName;
	public int multiObjective;
	public Pointer init;
	public Pointer optimise;
	public Pointer deinit;
	
	public Pointer save;
	public Pointer restore;
	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}
	
	private static final List<String> FIELDS;

	static {
		FIELDS = Arrays.asList(new String[]{
			"uniqueName", "prettyName", "multiObjective",
			"init", "optimise", "deinit", "save", "restore"
		});
	}
}

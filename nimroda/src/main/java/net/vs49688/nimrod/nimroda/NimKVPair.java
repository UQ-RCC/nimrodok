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

public class NimKVPair extends Structure {
	public static class ByReference extends NimKVPair implements Structure.ByReference {
		public ByReference() { }
		public ByReference(Pointer p) { super(p); }
	}

	public NimKVPair() {	}
	public NimKVPair(Pointer p) { super(p); }
	
	public String key;
	public String value;

	public void set(String key, String value)
	{
		this.key = key;
		this.value = value;
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}
	
	private static final List<String> FIELDS;

	static {
		FIELDS = Arrays.asList(new String[]{
			"key", "value"
		});
	}
}

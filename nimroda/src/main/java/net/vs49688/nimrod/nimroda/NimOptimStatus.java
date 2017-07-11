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
import java.nio.charset.*;
import com.sun.jna.*;

public class NimOptimStatus extends Structure {

	public static class ByReference extends NimOptimStatus implements Structure.ByReference {

		public ByReference() {
			super();
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public static class ByValue extends NimOptimStatus implements Structure.ByValue {
	}

	public NimOptimStatus() {
		super();
	}

	public NimOptimStatus(Pointer p) {
		super(p);
	}

	public int code;
	public int state;
	public byte[] message = new byte[NimDefinitions.LONGEST_ERROR_STRING];
	public Pointer results;
	public SizeT resultCount;

	public String getMessage() {
		int end = 0;
		for(int i = 0; i < message.length; ++i) {
			if(message[i] == '\0') {
				end = i;
				break;
			}
		}
		
		return new String(message, 0, end, Charset.forName("US-ASCII"));
	}
	
	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS;

	static {
		FIELDS = Arrays.asList(new String[]{"code", "state", "message", "results", "resultCount"});
	}
}

/*
 * Copyright (C) 2017 Zane van Iperen
 * All rights reserved.
 * 
 * NOTICE: This code may not be used unless explicit permission
 * is obtained from Zane van Iperen.
 * 
 * CONTACT: zane@zanevaniperen.com
 */
package net.vs49688.cio;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NativeCIOContext extends Structure {
	public NativeCIOContext() {
		super();
	}

	public NativeCIOContext(Pointer p) {
		super(p);
	}

	public static class ByReference extends NativeCIOContext implements Structure.ByReference {
		public ByReference() {
			super();
		}

		public ByReference(Pointer p) {
			super(p);
		}
	}

	public IPush push;
	public IPop pop;
	public IRead read;
	public IWrite write;
	public Pointer user;

	public ByReference parent;
	public Pointer name;
	public int listFlag;

	@Override
	protected List<String> getFieldOrder() {
		return Collections.unmodifiableList(FIELDS);
	}

	private static final List<String> FIELDS = Arrays.asList(new String[]{
		"push", "pop", "read", "write", "user", "parent", "name", "listFlag"
	});

	public interface IRead extends Callback {
		int read(ByReference ctx, Pointer buffer, SizeT size, int type, String name);
	}

	public interface IWrite extends Callback {
		int write(ByReference ctx, Pointer buffer, SizeT size, int type, String name);
	}

	public interface IPush extends Callback {
		int push(ByReference oldCtx, ByReference newCtx);
	}

	public interface IPop extends Callback {
		int pop(ByReference ctx);
	}
}

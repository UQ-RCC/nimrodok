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

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public final class CIOContext {
	public interface Operations {
		/**
		 * Called when a new context is pushed.
		 * @param ctx The old context.
		 * @param name The name of the new context.
		 * @param isList Is the new context a list?
		 * @return The operations implementation to use for the new context.
		 * @throws IOException - if an I/O error occurs.
		 */
		Operations push(CIOContext ctx, String name, boolean isList) throws IOException;

		void pop(CIOContext ctx) throws IOException;

		void read(CIOContext ctx, Pointer buffer, SizeT size, Type type, String name) throws IOException;

		void write(CIOContext ctx, Pointer buffer, SizeT size, Type type, String name) throws IOException;

	}

	private final NativeCIOContext.ByReference m_Context;
	private final ICIO m_Interface;
	private final CIOContext m_Parent;
	private final Operations m_Operations;
	private final Memory m_NameBuffer;
	private final String m_Name;
	private final Map<NativeCIOContext, CIOContext> m_Children;

	public enum Type {
		None,
		Int8, UInt8,
		Int16, UInt16,
		Int32, UInt32,
		Int64, UInt64,
		Float32, Float64,
		String
	}

	/**
	 * Create a new C/IO context.
	 *
	 * @param cio The native C/IO interface.
	 * @param name The context name.
	 * @param list Is this context a list or a dictionary?
	 * @param ops The operations implementation.
	 */
	public CIOContext(ICIO cio, String name, boolean list, Operations ops) {
		this(new NativeCIOContext.ByReference(), name, null, cio, ops);
		m_Context.listFlag = list ? 1 : 0;
		m_Context.user = null;
	}

	/**
	 * Initialise a C/IO context.
	 *
	 * @param ctx The C/IO context.
	 * @param name The context name.
	 * @param parent Our parent context. null if none.
	 * @param ops The operations implementation.
	 * @param cio The native C/IO interface.
	 */
	private CIOContext(NativeCIOContext.ByReference ctx, String name, CIOContext parent, ICIO cio, Operations ops) {
		m_Context = ctx;
		m_Context.push = new _Push();
		m_Context.pop = new _Pop();
		m_Context.read = new _Read();
		m_Context.write = new _Write();

		/* Always replace the native name with our copy, lest the GC might collect it. */
		if((m_Name = name) == null) {
			m_NameBuffer = null;
		} else {
			byte[] stringBytes = m_Name.getBytes(Charset.forName("UTF-8"));
			m_Context.name = m_NameBuffer = new Memory(stringBytes.length);
			m_NameBuffer.write(0, stringBytes, 0, stringBytes.length);
		}

		m_Context.name = m_NameBuffer;

		m_Parent = parent;
		m_Interface = cio;
		m_Operations = ops;
		m_Children = new HashMap<>();
	}

	public CIOContext pushDict(String name) throws IOException {
		NativeCIOContext.ByReference ctx = new NativeCIOContext.ByReference();
		if(m_Interface.jnaCIOPushDict(ctx, m_Context, name) != ctx) {
			throw new IOException();
		}

		return m_Children.get(ctx);
	}

	public CIOContext pushList(String name) throws IOException {
		NativeCIOContext.ByReference ctx = new NativeCIOContext.ByReference();
		if(m_Interface.jnaCIOPushList(ctx, m_Context, name) != ctx) {
			throw new IOException();
		}

		return m_Children.get(ctx);
	}

	public CIOContext pop() throws IOException {
		if(m_Interface.jnaCIOPop(m_Context) != 0) {
			throw new IOException();
		}
		return m_Parent;
	}

	public String getName() {
		return m_Name;
	}

	public boolean isList() {
		return m_Context.listFlag != 0;
	}

	public CIOContext getParent() {
		return m_Parent;
	}

	public NativeCIOContext.ByReference getNativeContext() {
		return m_Context;
	}

	// <editor-fold defaultstate="collapsed" desc="RW Helpers">
	public void writeByte(byte val) throws IOException {
		writeByte(val, null);
	}

	public void writeByte(byte val, String name) throws IOException {
		write(new ByteByReference(val).getPointer(), 1, Type.Int8, name);
	}

	public void writeShort(short val) throws IOException {
		writeShort(val, null);
	}

	public void writeShort(short val, String name) throws IOException {
		write(new ShortByReference(val).getPointer(), 2, Type.Int16, name);
	}

	public void writeInt(int val) throws IOException {
		writeInt(val, null);
	}

	public void writeInt(int val, String name) throws IOException {
		write(new IntByReference(val).getPointer(), 4, Type.Int32, name);
	}

	public void writeLong(long val) throws IOException {
		writeLong(val, null);
	}

	public void writeLong(long val, String name) throws IOException {
		write(new LongByReference(val).getPointer(), 8, Type.Int64, name);
	}

	public void writeFloat(float val) throws IOException {
		writeFloat(val, null);
	}

	public void writeFloat(float val, String name) throws IOException {
		write(new FloatByReference(val).getPointer(), 4, Type.Float32, name);
	}

	public void writeDouble(double val) throws IOException {
		writeDouble(val, null);
	}

	public void writeDouble(double val, String name) throws IOException {
		write(new DoubleByReference(val).getPointer(), 8, Type.Float64, name);
	}

	public void writeString(String val, String name) throws IOException {
		byte[] bytes = val.getBytes(Charset.forName("UTF-8"));
		Memory mem = new Memory(bytes.length);
		mem.write(0, bytes, 0, bytes.length);
		write(mem, mem.size(), Type.String, name);
	}

	public byte readByte() throws IOException {
		return readByte(null);
	}

	public byte readByte(String name) throws IOException {
		ByteByReference ref = new ByteByReference();
		read(ref.getPointer(), 1, Type.Int8, name);
		return ref.getValue();
	}

	public short readShort() throws IOException {
		return readShort(null);
	}

	public short readShort(String name) throws IOException {
		ShortByReference ref = new ShortByReference();
		read(ref.getPointer(), 2, Type.Int16, name);
		return ref.getValue();
	}

	public int readInt() throws IOException {
		return readInt(null);
	}

	public int readInt(String name) throws IOException {
		IntByReference ref = new IntByReference();
		read(ref.getPointer(), 4, Type.Int32, name);
		return ref.getValue();
	}

	public long readLong() throws IOException {
		return readLong(null);
	}

	public long readLong(String name) throws IOException {
		LongByReference ref = new LongByReference();
		read(ref.getPointer(), 8, Type.Int64, name);
		return ref.getValue();
	}

	public float readFloat() throws IOException {
		return readFloat(null);
	}

	public float readFloat(String name) throws IOException {
		FloatByReference ref = new FloatByReference();
		read(ref.getPointer(), 4, Type.Float32, name);
		return ref.getValue();
	}

	public double readDouble() throws IOException {
		return readDouble(null);
	}

	public double readDouble(String name) throws IOException {
		DoubleByReference ref = new DoubleByReference();
		read(ref.getPointer(), 8, Type.Float64, name);
		return ref.getValue();
	}

	public String readString(String name) throws IOException {
		/* Get the length */
		SizeTByReference size = new SizeTByReference();
		read(size.getPointer(), 0, Type.String, name);
		long byteLength = size.getValue().longValue();
		/* Get the string itself. */
		ByteBuffer bb = ByteBuffer.allocateDirect((int)byteLength);
		read(Native.getDirectBufferPointer(bb), byteLength, Type.String, name);
		return new String(bb.array(), Charset.forName("UTF-8"));
	}
	// </editor-fold>

	private void write(Pointer ref, long size, Type type, String name) throws IOException {
		if(m_Interface.jnaCIOWrite(m_Context, ref, new SizeT(size), typeToInt(type), name) != 0) {
			throw new IOException();
		}
	}

	private void read(Pointer ref, long size, Type type, String name) throws IOException {
		if(m_Interface.jnaCIORead(m_Context, ref, new SizeT(size), typeToInt(type), name) != 0) {
			throw new IOException();
		}
	}

	private class _Push implements NativeCIOContext.IPush {
		@Override
		public int push(NativeCIOContext.ByReference oldCtx, NativeCIOContext.ByReference newCtx) {
			assert oldCtx.equals(m_Context);

			try {
				String name = newCtx.name != null ? newCtx.name.getString(0) : null;
				Operations ops = m_Operations.push(CIOContext.this, name, newCtx.listFlag != 0);
				m_Children.put(newCtx, new CIOContext(newCtx, name, CIOContext.this, m_Interface, ops == null ? m_Operations : ops));
			} catch(Throwable e) {
				e.printStackTrace(System.err);
				return 1;
			}
			return 0;
		}

	}

	private class _Pop implements NativeCIOContext.IPop {
		@Override
		public int pop(NativeCIOContext.ByReference ctx) {
			assert ctx.equals(m_Context);
			
			try {
				m_Operations.pop(CIOContext.this);
				m_Children.remove(ctx);
			} catch(Throwable e) {
				e.printStackTrace(System.err);
				return 1;
			}
			return 0;
		}

	}

	private class _Read implements NativeCIOContext.IRead {
		@Override
		public int read(NativeCIOContext.ByReference ctx, Pointer buffer, SizeT size, int type, String name) {
			assert ctx.equals(m_Context);

			try {
				CIOContext.this.m_Operations.read(CIOContext.this, buffer, size, intToType(type), name);
			} catch(Throwable e) {
				e.printStackTrace(System.err);
				return 1;
			}
			return 0;
		}

	}

	private class _Write implements NativeCIOContext.IWrite {
		@Override
		public int write(NativeCIOContext.ByReference ctx, Pointer buffer, SizeT size, int type, String name) {
			assert ctx.equals(m_Context);

			try {
				CIOContext.this.m_Operations.write(CIOContext.this, buffer, size, intToType(type), name);
			} catch(Throwable e) {
				e.printStackTrace(System.err);
				return 1;
			}
			return 0;
		}

	}

	public static Type intToType(int type) {
		switch(type) {
			case 0:
				return Type.None;
			case 1:
				return Type.Int8;
			case 2:
				return Type.UInt8;
			case 3:
				return Type.Int16;
			case 4:
				return Type.UInt16;
			case 5:
				return Type.Int32;
			case 6:
				return Type.UInt32;
			case 7:
				return Type.Int64;
			case 8:
				return Type.UInt64;
			case 9:
				return Type.Float32;
			case 10:
				return Type.Float64;
			case 11:
				return Type.String;
		}
		throw new IllegalArgumentException("Unknown C/IO type id");
	}

	public static int typeToInt(Type type) {
		switch(type) {
			case None:
				return 0;
			case Int8:
				return 1;
			case UInt8:
				return 2;
			case Int16:
				return 3;
			case UInt16:
				return 4;
			case Int32:
				return 5;
			case UInt32:
				return 6;
			case Int64:
				return 7;
			case UInt64:
				return 8;
			case Float32:
				return 9;
			case Float64:
				return 10;
			case String:
				return 11;
		}
		throw new RuntimeException();
	}

}

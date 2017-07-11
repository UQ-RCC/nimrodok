package net.vs49688.nimrod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jna.Pointer;
import java.io.IOException;
import net.vs49688.cio.CIOContext;
import net.vs49688.cio.SizeT;

public class GSONReadOperations implements CIOContext.Operations {

	private final JsonElement m_Current;
	private int m_ArrayIndex;

	public GSONReadOperations(JsonElement root) {
		m_Current = root;
		m_ArrayIndex = 0;
	}

	@Override
	public CIOContext.Operations push(CIOContext ctx, String name, boolean listFlag) throws IOException {
		JsonElement newRoot;

		if(ctx.isList()) {
			newRoot = ((JsonArray)m_Current).get(m_ArrayIndex);

			if(listFlag) {
				if(!(newRoot instanceof JsonArray)) {
					throw new IOException();
				}
			} else if(!(newRoot instanceof JsonObject)) {
				throw new IOException();
			}

			++m_ArrayIndex;
		} else {
			JsonObject obj = (JsonObject)m_Current;

			if(listFlag) {
				newRoot = obj.getAsJsonArray(name);
			} else {
				newRoot = obj.getAsJsonObject(name);
			}
		}

		return new GSONReadOperations(newRoot);
	}

	@Override
	public void pop(CIOContext ctx) throws IOException {
	}

	@Override
	public void read(CIOContext ctx, Pointer buffer, SizeT size, CIOContext.Type type, String name) throws IOException {
		switch(type) {
			case Int8:
			case UInt8:
				if(ctx.isList()) {
					buffer.setByte(0, ((JsonArray)m_Current).get(m_ArrayIndex).getAsByte());
					++m_ArrayIndex;
				} else {
					buffer.setByte(0, ((JsonObject)m_Current).get(name).getAsByte());
				}
				break;
			case Int16:
			case UInt16:
				if(ctx.isList()) {
					buffer.setShort(0, ((JsonArray)m_Current).get(m_ArrayIndex).getAsShort());
					++m_ArrayIndex;
				} else {
					buffer.setShort(0, ((JsonObject)m_Current).get(name).getAsShort());
				}
				break;
			case Int32:
			case UInt32:
				if(ctx.isList()) {
					buffer.setInt(0, ((JsonArray)m_Current).get(m_ArrayIndex).getAsInt());
					++m_ArrayIndex;
				} else {
					buffer.setInt(0, ((JsonObject)m_Current).get(name).getAsInt());
				}
				break;
			case Int64:
			case UInt64:
				if(ctx.isList()) {
					buffer.setLong(0, ((JsonArray)m_Current).get(m_ArrayIndex).getAsLong());
					++m_ArrayIndex;
				} else {
					buffer.setLong(0, ((JsonObject)m_Current).get(name).getAsLong());
				}
				break;
			case Float32:
				if(ctx.isList()) {
					buffer.setFloat(0, ((JsonArray)m_Current).get(m_ArrayIndex).getAsFloat());
					++m_ArrayIndex;
				} else {
					buffer.setFloat(0, ((JsonObject)m_Current).get(name).getAsFloat());
				}
				break;
			case Float64:
				if(ctx.isList()) {
					buffer.setDouble(0, ((JsonArray)m_Current).get(m_ArrayIndex).getAsDouble());
					++m_ArrayIndex;
				} else {
					buffer.setDouble(0, ((JsonObject)m_Current).get(name).getAsDouble());
				}
				break;
			case String:
				/* This is a special case. */
				handleString(ctx, buffer, size, name);
				break;
		}
	}

	private void handleString(CIOContext ctx, Pointer buffer, SizeT size, String name) throws IOException {
		String s;
		if(ctx.isList()) {
			s = ((JsonArray)m_Current).get(m_ArrayIndex).getAsString();
		} else {
			s = ((JsonObject)m_Current).get(name).getAsString();
		}
		
		long _size = size.longValue();
		if(_size == 0) {
			/* Show me a case where sizeof(size_t) != sizeof(void*) */
			buffer.setPointer(0, Pointer.createConstant(s.length()));
		} else {
			if(s.length() > _size)
				throw new IOException("Buffer too small for string");
			buffer.setString(0, s);
			++m_ArrayIndex;
		}
	}
	
	@Override
	public void write(CIOContext ctx, Pointer buffer, SizeT size, CIOContext.Type type, String name) throws IOException {
		throw new IOException("Writing not supported");
	}

}

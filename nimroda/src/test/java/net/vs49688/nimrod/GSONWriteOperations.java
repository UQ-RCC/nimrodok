package net.vs49688.nimrod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jna.Pointer;
import java.io.IOException;
import net.vs49688.cio.CIOContext;
import net.vs49688.cio.SizeT;

public class GSONWriteOperations implements CIOContext.Operations {

	private final JsonElement m_Current;

	public GSONWriteOperations(JsonElement root) {
		m_Current = root;
	}

	@Override
	public CIOContext.Operations push(CIOContext ctx, String name, boolean listFlag) throws IOException {
		JsonElement newRoot = listFlag ? new JsonArray() : new JsonObject();

		if(ctx.isList()) {
			((JsonArray) m_Current).add(newRoot);
		} else {
			((JsonObject) m_Current).add(name, newRoot);
		}

		return new GSONWriteOperations(newRoot);
	}

	@Override
	public void pop(CIOContext ctx) throws IOException {
	}

	@Override
	public void read(CIOContext ctx, Pointer buffer, SizeT size, CIOContext.Type type, String name) throws IOException {
		throw new IOException("Reading not supported");
	}

	@Override
	public void write(CIOContext ctx, Pointer buffer, SizeT size, CIOContext.Type type, String name) throws IOException {
		switch(type) {
			case Int8:
			case UInt8:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getByte(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getByte(0));
				}
				break;
			case Int16:
			case UInt16:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getShort(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getShort(0));
				}
				break;
			case Int32:
			case UInt32:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getInt(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getInt(0));
				}
				break;
			case Int64:
			case UInt64:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getLong(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getLong(0));
				}
				break;
			case Float32:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getFloat(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getFloat(0));
				}
				break;
			case Float64:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getDouble(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getDouble(0));
				}
				break;
			case String:
				if(ctx.isList()) {
					((JsonArray) m_Current).add(buffer.getString(0));
				} else {
					((JsonObject) m_Current).addProperty(name, buffer.getString(0));
				}
				break;
		}
	}

}

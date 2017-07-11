package net.vs49688.nimrod;

import au.edu.uq.rcc.nimrod.optim.BaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Batch;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jna.Pointer;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import net.vs49688.cio.CIOContext;
import net.vs49688.cio.NativeCIOContext;
import net.vs49688.cio.SizeT;
import net.vs49688.nimrod.nimroda.INimrodA;
import net.vs49688.nimrod.nimroda.NativeAlgorithm;
import net.vs49688.nimrod.nimroda.NimCallbacks;
import net.vs49688.nimrod.nimroda.NimKVPair;
import net.vs49688.nimrod.nimroda.NimrodA;
import org.junit.Assert;
import org.junit.Test;

public class IOTest {

	public static int pid = NativeInterfaceTest.pid;

	public static ArrayList<Object> m_Test = new ArrayList<>();

	private class TestPop implements NativeCIOContext.IPop {

		public final JsonElement root;

		public TestPop(JsonElement root) {
			this.root = root;
			m_Test.add(this);
		}

		@Override
		public int pop(NativeCIOContext.ByReference ctx) {
			return 0;
		}

	}

	private class TestPush implements NativeCIOContext.IPush {

		public final JsonElement root;

		public TestPush(JsonElement root) {
			this.root = root;
			m_Test.add(this);
		}

		@Override
		public int push(NativeCIOContext.ByReference oldCtx, NativeCIOContext.ByReference newCtx) {
			boolean isNewList = newCtx.listFlag != 0;
			boolean isOldList = oldCtx.listFlag != 0;
			JsonElement newRoot = isNewList ? new JsonArray() : new JsonObject();
			newCtx.push = new TestPush(newRoot);
			newCtx.pop = new TestPop(newRoot);
			newCtx.read = new TestRead(newRoot);
			newCtx.write = new TestWrite(newRoot);
			if(isOldList) {
				((JsonArray)root).add(newRoot);
			} else {
				((JsonObject)root).add(newCtx.name.getString(0), newRoot);
			}
			return 0;
		}

	}

	private class TestRead implements NativeCIOContext.IRead {

		public final JsonElement root;

		public TestRead(JsonElement root) {
			this.root = root;
			m_Test.add(this);
		}

		@Override
		public int read(NativeCIOContext.ByReference ctx, Pointer buffer, SizeT size, int type, String name) {
			return -1;
		}

	}

	private class TestWrite implements NativeCIOContext.IWrite {

		public final JsonElement root;

		public TestWrite(JsonElement root) {
			this.root = root;
			m_Test.add(this);
		}

		@Override
		public int write(NativeCIOContext.ByReference ctx, Pointer buffer, SizeT size, int type, String name) {
			boolean isList = ctx.listFlag != 0;
			CIOContext.Type actualType = CIOContext.intToType(type);
			switch(actualType) {
				case Int8:
				case UInt8:
					if(isList) {
						((JsonArray)root).add(buffer.getByte(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getByte(0));
					}
					break;
				case Int16:
				case UInt16:
					if(isList) {
						((JsonArray)root).add(buffer.getShort(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getShort(0));
					}
					break;
				case Int32:
				case UInt32:
					if(isList) {
						((JsonArray)root).add(buffer.getInt(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getInt(0));
					}
					break;
				case Int64:
				case UInt64:
					if(isList) {
						((JsonArray)root).add(buffer.getLong(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getLong(0));
					}
					break;
				case Float32:
					if(isList) {
						((JsonArray)root).add(buffer.getFloat(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getFloat(0));
					}
					break;
				case Float64:
					if(isList) {
						((JsonArray)root).add(buffer.getDouble(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getDouble(0));
					}
					break;
				case String:
					if(isList) {
						((JsonArray)root).add(buffer.getString(0));
					} else {
						((JsonObject)root).addProperty(name, buffer.getString(0));
					}
			}
			return 0;
		}

	}

	private static NimrodA initMOTS() throws NimrodOKException, ParseException {
		NimrodA.initLibrary();
		NativeAlgorithm algo = ZDT4.getMOTS2();
		Assert.assertNotNull("Unable to query MOTS2 algorithm from Nimrod/A", algo);
		SetOfParams sop = ZDT4.getSOP();
		PointContainer pc = new PointContainer(new OptimPoint(sop), 2);
		for(int i = 0; i < pc.point.dimensionality; ++i) {
			pc.point.coords[i] = 0;
		}
		NimrodA nimrod = new NimrodA(algo, pc, 2, 0, ZDT4.getConfig(10), new INimrodReceiver() {
			@Override
			public void logf(BaseAlgorithm instance, String fmt, Object... args) {
			}

			@Override
			public void logf(BaseAlgorithm instance, Throwable e) {
			}

			@Override
			public void incomingStats(BaseAlgorithm instance, INimrodReceiver.Stat[] stats) {
			}

		});

		for(int i = 0; i < 10; ++i) {
			NimrodA.OptimisationState state = nimrod.fire();
			if(state == NimrodA.OptimisationState.WaitingForBatch) {
				Batch b = new Batch(null, nimrod.getBatch());
				evalBatch(b);
			} else if(state == NimrodA.OptimisationState.Finished) {
				break;
			}
		}

		return nimrod;
	}

	public static void evalBatch(Batch batch) {
		for(PointContainer pc : batch.points) {
			pc.objectives[0] = pc.point.coords[0];
			pc.objectives[1] = ZDT4.zdt4_f(pc.point.coords);
		}
	}

	@Test
	public void rawRestoreTest() throws Exception {
		INimrodA lib = NimrodA.getInterface();

		JsonElement jsob = new JsonParser().parse(ZDT4.MOTS2_STATE_DUMP_FINISHED);
		CIOContext ctx = new CIOContext(NimrodA.getInterface(), "nimroda", false, new GSONReadOperations(jsob));

		NimCallbacks.ByReference callbacks = new NimCallbacks.ByReference();
		callbacks.puts = new INimrodA.IPuts() {
			@Override
			public void puts(Pointer instance, String s) {
				System.out.print(s);
			}

		};
		callbacks.writeStats = new INimrodA.IWriteStats() {
			@Override
			public void writeStats(Pointer instance, NimKVPair.ByReference stats, net.vs49688.nimrod.nimroda.SizeT count) {

			}

		};
		callbacks.user = null;

		Pointer instance = lib.jnaRestore(callbacks, ctx.getNativeContext());
		if(instance == null) {
			throw new Exception();
		}

		int x = 0;
		lib.jnaRelease(instance);
	}

	@Test
	public void rawSaveTest() throws IOException, NimrodOKException, ParseException {
		INimrodA lib = NimrodA.getInterface();
		NimrodA nimrod = initMOTS();

		JsonObject saveRoot = new JsonObject();
		NativeCIOContext.ByReference nativeCtx = new NativeCIOContext.ByReference();
		nativeCtx.pop = new TestPop(saveRoot);
		nativeCtx.push = new TestPush(saveRoot);
		nativeCtx.read = new TestRead(saveRoot);
		nativeCtx.write = new TestWrite(saveRoot);
		nativeCtx.user = null;

		lib.jnaSave(nimrod.getInstance(), nativeCtx);
		nimrod.release();

		System.out.printf("%s\n", saveRoot.toString());
	}

	@Test
	public void wrapperSaveTest() throws IOException, NimrodOKException, ParseException {
		JsonObject saveRoot = new JsonObject();

		NimrodA nimrod = initMOTS();

		CIOContext ctx = new CIOContext(NimrodA.getInterface(), "nimroda", false, new GSONWriteOperations(saveRoot));

		nimrod.save(ctx);
		nimrod.release();
		System.out.printf("%s\n", saveRoot.toString());
	}

}

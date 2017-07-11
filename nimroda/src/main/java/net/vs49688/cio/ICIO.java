package net.vs49688.cio;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface ICIO extends Library {

	NativeCIOContext.ByReference jnaCIOPushDict(NativeCIOContext.ByReference ctx, NativeCIOContext.ByReference parent, String name);
	NativeCIOContext.ByReference jnaCIOPushList(NativeCIOContext.ByReference ctx, NativeCIOContext.ByReference parent, String name);
	
	int jnaCIOPop(NativeCIOContext.ByReference ctx);
	int jnaCIORead(NativeCIOContext.ByReference ctx, Pointer buffer, SizeT size, int type, String name);
	int jnaCIOWrite(NativeCIOContext.ByReference ctx, Pointer buffer, SizeT size, int type, String name);

	/* Don't need to include the type-specific calls here, we handle everything in Java. */
}

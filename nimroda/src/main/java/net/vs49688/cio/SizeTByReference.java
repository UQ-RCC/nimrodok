package net.vs49688.cio;

import com.sun.jna.Native;
import com.sun.jna.ptr.ByReference;

public final class SizeTByReference extends ByReference {
	public SizeTByReference() {
        this(new SizeT(0));
    }
    
    public SizeTByReference(SizeT value) {
        super(Native.SIZE_T_SIZE);
        setValue(value);
    }
    
    public void setValue(SizeT value) {
        getPointer().setLong(0, value.longValue());
    }
    
    public SizeT getValue() {
        return new SizeT(getPointer().getLong(0));
    }
}

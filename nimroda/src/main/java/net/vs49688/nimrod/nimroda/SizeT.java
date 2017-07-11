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

import com.sun.jna.*;

public class SizeT extends IntegerType {

	public SizeT() {
		this(0);
	}

	public SizeT(long value) {
		super(Native.SIZE_T_SIZE, value);
	}
}

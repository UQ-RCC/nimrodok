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

public class NimDefinitions {
	public static final int LONGEST_ERROR_STRING = 200;
	public static final int MAX_DIMS = 128;
	
	/** Length of parameter names. */
	public static final int MAX_NAME_LENGTH = 30;
	
	public static final int JOB_COMPLETE = 0;
	public static final int JOB_FAILED = 1;
	
	/* jnaSizeOf() parameters */
	public static final int TESTPARAM_ALGORITHM = 0;
	public static final int TESTPARAM_CONFIG = 1;
	public static final int TESTPARAM_CUSTOM_CONFIG = 2;
	public static final int TESTPARAM_OPTIM_STATUS = 3;
	public static final int TESTPARAM_POINT = 4;
	public static final int TESTPARAM_VARBLE_ATTRIB = 5;
	public static final int TESTPARAM_BATCH = 6;
	
	public static final double HUGEISH = 10e200;
}

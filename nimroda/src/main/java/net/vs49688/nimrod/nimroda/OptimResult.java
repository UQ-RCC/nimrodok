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

import au.edu.uq.rcc.nimrod.optim.PointContainer;
import com.sun.jna.Pointer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OptimResult {

	OptimResult(NimrodA nimrod, NimOptimStatus status) {
		this.errorCode = status.code;
		this.state = NimrodA.intToState(status.state);
		this.message = status.getMessage();

		int numPareto = status.resultCount.intValue();
		this.m_Results = new ArrayList<>(numPareto);
		if(numPareto != 0) {
			Pointer[] rawPointers = status.results.getPointerArray(0, numPareto);
			for(int i = 0; i < numPareto; ++i) {
				NimPoint.ByReference pt = new NimPoint.ByReference(rawPointers[i]);
				pt.read();
				this.m_Results.add(NimrodA.nimPoint2Point(pt, nimrod.getSetOfParams()));
			}
		}
	}

	public final int errorCode;
	public final NimrodA.OptimisationState state;
	public final String message;

	private final List<PointContainer> m_Results;

	public int getErrorCode() {
		return errorCode;
	}

	public String getMessage() {
		return message;
	}

	public List<PointContainer> getResults() {
		return Collections.unmodifiableList(m_Results);
	}
}

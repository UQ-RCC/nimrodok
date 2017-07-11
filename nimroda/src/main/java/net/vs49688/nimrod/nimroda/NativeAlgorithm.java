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

import java.util.Objects;

/**
 * Represents a native Nimrod/A algorithm.
 */
public final class NativeAlgorithm {

	/**
	 * The string unique to this algorithm.
	 */
	public final String identifier;
	/**
	 * The pretty-printable name of the algorithm.
	 */
	public final String prettyName;
	/**
	 * Does the algorithm support multiple objectives?
	 */
	public final boolean multiObjective;

	NativeAlgorithm(String identifier, String prettyName, boolean multiObjective) {
		this.identifier = identifier;
		this.prettyName = prettyName;
		this.multiObjective = multiObjective;
	}

	@Override
	public String toString() {
		return identifier;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 83 * hash + Objects.hashCode(this.identifier);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj == null) {
			return false;
		}
		if(getClass() != obj.getClass()) {
			return false;
		}
		final NativeAlgorithm other = (NativeAlgorithm)obj;
		return Objects.equals(this.identifier, other.identifier);
	}

}

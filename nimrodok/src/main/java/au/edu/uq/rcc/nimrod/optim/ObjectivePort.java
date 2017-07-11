/*
 * Copyright (C) 2017 Zane van Iperen
 * All rights reserved.
 * 
 * NOTICE: This code may not be used unless explicit permission
 * is obtained from Zane van Iperen.
 * 
 * CONTACT: zane@zanevaniperen.com
 */
package au.edu.uq.rcc.nimrod.optim;

import org.monash.nimrod.NimrodActor.NimrodAtomicActor;
import org.monash.nimrod.data.Tag;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.DoubleToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.Token;
import ptolemy.kernel.util.IllegalActionException;

public class ObjectivePort {

	public final NimrodAtomicActor<?> actor;
	public final TypedIOPort port;
	public final int index;

	public ObjectivePort(NimrodAtomicActor<?> actor, TypedIOPort port, int index) {
		this.actor = actor;
		this.port = port;
		this.index = index;
	}

	/**
	 * "fire" this objective port. This will consume one token.
	 *
	 * @return If an objective result was received, the point it belongs to is
	 * returned.
	 * @throws IllegalActionException
	 */
	public Result fire() throws IllegalActionException {
		return hasToken() ? processResult() : null;
	}

	public boolean hasToken() throws IllegalActionException {
		return port.hasToken(0);
	}

	private Result processResult() throws IllegalActionException {
		//actor.debugf("Objective %d: Accepting result...", index);
		DoubleToken dt = (DoubleToken)port.get(0);
		Tag colour = actor.nimrodKFunctions().getTokenColour(dt);

		Token tok = colour.getMetadata("creator");
		if(tok == null || !(tok instanceof ObjectToken)) {
			throw new IllegalActionException(port, "Received token without creator tag");
		}

		ObjectToken creatorToken = (ObjectToken)tok;
		if(creatorToken == null) {
			throw new IllegalActionException(port, "Received token with invalid creator tag");
		}

		/* Should never happen */
		if(creatorToken.getValue() != actor) {
			throw new IllegalActionException(port, "ObjectivePort: Received token that's not ours.");
		}

		tok = colour.getMetadata("point");
		if(tok == null || !(tok instanceof ObjectToken)) {
			throw new IllegalActionException(port, String.format("Objective %d received result without point token.", index));
		}

		Object obj = ((ObjectToken)tok).getValue();

		if(obj == null || !(obj instanceof PointContainer)) {
			throw new IllegalActionException(port, String.format("Objective %d received invalid point token.", index));
		}

		PointContainer pc = (PointContainer)obj;
		//actor.debugf("Objective %d:   point = %s", index, pc.point);
		pc.objectives[index] = dt.doubleValue();

		/* Minor hack */
		if(index == 0) {
			pc.point.objective = dt.doubleValue();
		}

		tok = colour.getMetadata("batch");
		if(tok == null || !(tok instanceof ObjectToken)) {
			throw new IllegalActionException(port, String.format("Objective %d received result without batch.", index));
		}

		obj = ((ObjectToken)tok).getValue();
		if(obj == null || !(obj instanceof Batch)) {
			throw new IllegalActionException(port, String.format("Objective %d received result with invalid batch.", index));
		}

		Batch batch = (Batch)obj;

		return new Result(pc, index, batch, colour);
	}

	public final class Result {

		public Result(PointContainer point, int index, Batch batch, Tag tokenColour) {
			this.point = point;
			this.objectiveIndex = index;
			this.batch = batch;
			this.tokenColour = tokenColour;
		}

		public final PointContainer point;
		public final int objectiveIndex;
		public final Batch batch;
		public final Tag tokenColour;
	}
}

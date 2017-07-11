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

import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class Point2ArrayActor extends TypedAtomicActor {
	private final TypedIOPort m_Input;
	private final TypedIOPort m_Output;
	
	public Point2ArrayActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
		
		m_Input = new TypedIOPort(this, "input", true, false);
		m_Input.setTypeEquals(PortTypes.POINT_TYPE);
		m_Input.setMultiport(false);
		
		m_Output = new TypedIOPort(this, "output", false, true);
		m_Output.setTypeEquals(new ArrayType(BaseType.DOUBLE));
		m_Output.setMultiport(false);
	}
	
	@Override
	public boolean prefire() throws IllegalActionException {
		return m_Input.hasToken(0);
	}
	
	@Override
	public void fire() throws IllegalActionException {
		OptimPoint pt = PortTypes.makeOptimPoint(m_Input.get(0));
		m_Output.send(0, ActorUtils.makeDoubleArrayTokens(pt.coords));
		
	}
}

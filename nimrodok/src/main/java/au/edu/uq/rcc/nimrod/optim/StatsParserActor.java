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

import java.util.LinkedList;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class StatsParserActor extends TypedAtomicActor {

	private final TypedIOPort m_InputPort;

	private final TypedIOPort m_StatsPort;
	
	private final TypedIOPort m_ParetoPort;

	private final TypedIOPort m_VectorPort;
	private final TypedIOPort m_ObjectivePort;
	
	private final LinkedList<Token[]> m_ParetoBacklog;

	private static final int PARETO_BACKLOG_SIZE = 32;

	public StatsParserActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		m_InputPort = new TypedIOPort(this, "Input", true, false);
		m_InputPort.setTypeEquals(PortTypes.STATS_PORT_TYPE);

		m_StatsPort = new TypedIOPort(this, "Stats", false, true);
		m_StatsPort.setTypeEquals(BaseType.GENERAL);

		m_ParetoPort = new TypedIOPort(this, "Pareto", false, true);
		m_ParetoPort.setTypeEquals(BaseType.DOUBLE);
		m_ParetoPort.setMultiport(true);

		m_VectorPort = new TypedIOPort(this, "Design Vector", false, true);
		m_VectorPort.setTypeEquals(BaseType.DOUBLE);
		m_VectorPort.setMultiport(true);

		m_ObjectivePort = new TypedIOPort(this, "Objective Vector", false, true);
		m_ObjectivePort.setTypeEquals(BaseType.DOUBLE);
		m_ObjectivePort.setMultiport(true);

		m_ParetoBacklog = new LinkedList<>();
	}

	@Override
	public void initialize() {
		m_ParetoBacklog.clear();
	}

	@Override
	public boolean prefire() throws IllegalActionException {
		return m_InputPort.hasToken(0);
	}

	@Override
	public void fire() throws IllegalActionException {
		RecordToken tok = (RecordToken)m_InputPort.get(0);

		String type = ((StringToken)tok.get("Type")).stringValue();

		Token payload = tok.get("Payload");
		switch(type) {
			case "msg":
				processMessage(payload);
				break;
			case "pareto":
				processPareto(payload);
				break;
			case "point":
				processPoint(payload);
				break;
			case "stats":
				processStats(payload);
				break;
		}

		/* Send off any backlog of pareto points. You'd be surprised how often
		 * The queue overflows. */
		for(int i = 0; i < PARETO_BACKLOG_SIZE; ++i) {
			if(m_ParetoBacklog.isEmpty()) {
				break;
			}

			Token[] toks = m_ParetoBacklog.pollFirst();
			for(int j = 0; j < toks.length; ++j) {
				m_ParetoPort.send(j, toks[j]);
			}
		}
	}

	private void processMessage(Token payload) throws IllegalActionException {
		m_StatsPort.send(0, payload);
	}

	private void processPareto(Token payload) throws IllegalActionException {
		Token[] toks = ((ArrayToken)payload).arrayValue();

		for(int i = 0; i < toks.length; ++i) {
			Token[] coords = ((ArrayToken)toks[i]).arrayValue();
			m_ParetoBacklog.addLast(coords);
		}
	}

	private void processPoint(Token payload) throws IllegalActionException {
		RecordToken rPayload = (RecordToken)payload;

		OptimPoint pt = PortTypes.makeOptimPoint(rPayload.get("Point"));
		for(int i = 0; i < pt.coords.length; ++i) {
			m_VectorPort.send(i, new DoubleToken(pt.coords[i]));
		}

		/* Objective values. */
		Token[] oToks = ((ArrayToken)rPayload.get("Objectives")).arrayValue();
		for(int i = 0; i < oToks.length; ++i) {
			m_ObjectivePort.send(i, oToks[i]);
		}
	}
	
	private void processStats(Token payload) throws IllegalActionException {
		m_StatsPort.send(0, payload);
	}
}

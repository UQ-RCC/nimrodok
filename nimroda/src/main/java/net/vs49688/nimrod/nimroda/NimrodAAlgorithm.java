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

import au.edu.uq.rcc.nimrod.optim.BaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Configuration;
import au.edu.uq.rcc.nimrod.optim.IAlgorithmDefinition;
import au.edu.uq.rcc.nimrod.optim.INimrodReceiver;
import au.edu.uq.rcc.nimrod.optim.NimrodOKException;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.PointContainer;
import au.edu.uq.rcc.nimrod.optim.PopulationBaseAlgorithm;
import au.edu.uq.rcc.nimrod.optim.Properties;
import au.edu.uq.rcc.nimrod.optim.TrajectoryBaseAlgorithm;
import java.util.Set;

public class NimrodAAlgorithm extends TrajectoryBaseAlgorithm {

	private NimrodA m_NimrodA;

	private final NativeAlgorithm m_Algo;

	private NimrodAAlgorithm(OptimPoint start, Configuration config, NativeAlgorithm algo, INimrodReceiver receiver) throws NimrodOKException {
		super(start, config, receiver);
		m_NimrodA = new NimrodA(algo, new PointContainer(start, config.objectiveCount()), config.objectiveCount(), config.rngSeed(), config.customProperties().getRawData(), new _Receiver(receiver));
		m_Algo = algo;
	}

	private class _Receiver implements INimrodReceiver {

		public final INimrodReceiver recv;

		public _Receiver(INimrodReceiver r) {
			recv = r;
		}

		@Override
		public void logf(BaseAlgorithm instance, String fmt, Object... args) {
			recv.logf(NimrodAAlgorithm.this, fmt, args);
		}

		@Override
		public void logf(BaseAlgorithm instance, Throwable e) {
			recv.logf(NimrodAAlgorithm.this, e);
		}

		@Override
		public void incomingStats(BaseAlgorithm instance, Stat[] stats) {
			recv.incomingStats(NimrodAAlgorithm.this, stats);
		}
	}

	private static State optimisationStateToState(NimrodA.OptimisationState state) {
		switch(state) {
			case Stopped:
				return State.STOPPED;
			case WaitingForBatch:
				return State.WAITING_FOR_BATCH;
			case Finished:
				return State.FINISHED;
		}

		throw new IllegalStateException();
	}

	private void doFire() throws NimrodOKException {
		m_ParetoFront.clear();
		m_State = State.RUNNING;
		m_State = optimisationStateToState(m_NimrodA.fire());
		if(m_State == State.WAITING_FOR_BATCH) {
			m_CurrentBatch = createBatch(m_NimrodA.getBatch());
		} else if(m_State == State.FINISHED) {
			OptimResult result = m_NimrodA.getResult();

			receiver.logf(this, "Optimisation finished with code %d: %s\n", result.errorCode, result.message);
			if(result.errorCode != 0) {
				throw new NimrodOKException(result.message);
			}
			m_ParetoFront.addAll(result.getResults());

			cleanup();
		}

		if(m_State != State.FINISHED) {
			m_ParetoFront.addAll(m_NimrodA.getResult().getResults());
		}
	}

	@Override
	public void fire() throws NimrodOKException {
		if(m_State == State.STOPPED) {
			doFire();
		} else if(m_State == State.WAITING_FOR_BATCH) {
			if(!m_CurrentBatch.isFinished()) {
				return;
			}

			doFire();
		} else if(m_State == State.RUNNING) {
			/* Should never happen */
		} else if(m_State == State.FINISHED) {
			/* We're done, why are you calling us again? */
		}
	}

	public static IAlgorithmDefinition[] getDefinitions() throws UnsatisfiedLinkError {
		NimrodA.initLibrary();

		Set<NativeAlgorithm> avail = NimrodA.availableAlgorithms();
		IAlgorithmDefinition[] algos = new IAlgorithmDefinition[avail.size()];

		int i = 0;
		for(NativeAlgorithm algo : NimrodA.availableAlgorithms()) {
			algos[i++] = new NimrodADefinition(algo);
		}

		return algos;
	}

	private static Properties getDefaultAlgorithmProperties(NativeAlgorithm algo) {
		Properties props = new Properties();
		if(algo.identifier.equals("MOTS2")) {
			props.setProperty("mots2.diversify", "25");
			props.setProperty("mots2.intensify", "15");
			props.setProperty("mots2.reduce", "50");
			props.setProperty("mots2.start_step", "10 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1 0.1");
			props.setProperty("mots2.ssrf", "0.5");
			props.setProperty("mots2.n_sample", "6");
			props.setProperty("mots2.loop_limit", "1000");
			props.setProperty("mots2.eval_limit", "0");
			props.setProperty("mots2.n_regions", "4");
			props.setProperty("mots2.stm_size", "20");
		}

		return props;
	}

	@Override
	public void cleanup() {
		if(m_NimrodA != null) {
			m_NimrodA.release();
			m_NimrodA = null;
		}
	}

	public NimrodA getNimrod() {
		return m_NimrodA;
	}

	// <editor-fold defaultstate="collapsed" desc="Unused, Keeping around for reference">
//	@Deprecated
//	private void createDummyPorts() throws IllegalActionException, NameDuplicationException {
//		NimrodOptimActor oa = (NimrodOptimActor) actor.getActor();
//		{
//			m_DummyRecv = oa.createOrGetIOPort("DummyRecv", false);
//			m_DummyRecv.setTypeEquals(BaseType.NIL);
//
//			StringAttribute hide = (StringAttribute) m_DummyRecv.getAttribute("_hide");
//			if(hide == null) {
//				hide = new StringAttribute(m_DummyRecv, "_hide");
//			}
//			hide.setExpression("true");
//		}
//
//		{
//			m_DummySend = oa.createOrGetIOPort("DummySend", true);
//			m_DummySend.setTypeEquals(BaseType.NIL);
//
//			StringAttribute hide = (StringAttribute) m_DummySend.getAttribute("_hide");
//			if(hide == null) {
//				hide = new StringAttribute(m_DummySend, "_hide");
//			}
//			hide.setExpression("true");
//		}
//
//		/* Clean-up any old relations that may be hanging around. */
//		{
//			List<Relation> rels = m_DummyRecv.linkedRelationList();
//			rels.addAll(m_DummySend.linkedRelationList());
//			for(Relation rel : rels) {
//				if(rel == null) {
//					continue;
//				}
//
//				rel.unlinkAll();
//				rel.workspace().remove(rel);
//			}
//
//			m_DummyRecv.unlinkAll();
//			m_DummySend.unlinkAll();
//		}
//
//		{
//			String relName = String.format("DummyPortRelation%d", this.hashCode());
//
//			TypedIORelation rel = new TypedIORelation(oa.workspace());
//			rel.setName(relName);
//
//			Attribute old = oa.getAttribute("DummyPortWidth");
//			if(old != null) {
//				old.setContainer(null);
//			}
//
//			rel.width = new Parameter(oa, "DummyPortWidth");
//			rel.width.setTypeEquals(BaseType.INT);
//			rel.width.setToken(new IntToken(1));
//			rel.width.setVisibility(Settable.NONE);
//
//			m_DummyRecv.link(rel);
//			m_DummySend.link(rel);
//		}
//
//	}
	// </editor-fold>
	// <editor-fold defaultstate="collapsed" desc="Definitions">
	private static class NimrodADefinition implements IAlgorithmDefinition {

		public NimrodADefinition(NativeAlgorithm algo) {
			this.algorithmInfo = algo;
			this.prettyName = String.format("[Nimrod/A] %s", algo.prettyName);
			this.properties = getDefaultAlgorithmProperties(algo);
		}

		public final NativeAlgorithm algorithmInfo;
		public final String prettyName;
		public final Properties properties;

		@Override
		public String getUniqueName() {
			return algorithmInfo.identifier;
		}

		@Override
		public String getPrettyName() {
			return prettyName;
		}

		@Override
		public PopulationBaseAlgorithm createPopulationInstance(OptimPoint[] initialPopulation, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			throw new UnsupportedOperationException();
		}

		@Override
		public TrajectoryBaseAlgorithm createTrajectoryInstance(OptimPoint startingPoint, Configuration config, INimrodReceiver receiver) throws NimrodOKException {
			return new NimrodAAlgorithm(startingPoint, config, algorithmInfo, receiver);
		}

		@Override
		public int getSupportedObjectiveCount() {
			return algorithmInfo.multiObjective ? Integer.MAX_VALUE : 1;
		}

		@Override
		public Properties getDefaultProperties() {
			return properties;
		}

		@Override
		public boolean isTrajectoryBased() {
			return true;
		}

		public boolean isPopulationBased() {
			return false;
		}
	}
	// </editor-fold>
}

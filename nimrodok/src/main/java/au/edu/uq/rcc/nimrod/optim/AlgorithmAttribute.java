package au.edu.uq.rcc.nimrod.optim;

import ptolemy.data.expr.Parameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;

public class AlgorithmAttribute extends StringAttribute {

	private IAlgorithmDefinition m_Algorithm;
	private final NimrodOptimActor m_OptimActor;

	public AlgorithmAttribute(NamedObj container, IAlgorithmDefinition algo) throws IllegalActionException, NameDuplicationException {
		super(container, algo.getUniqueName());
		
		if(algo == null) {
			throw new IllegalActionException(this, "Algorithm cannot be null");
		}
		m_Algorithm = algo;

		/* Ensure that we're only used by the correct class. */
		if(container == null
				|| !(container instanceof AlgorithmicChoiceStyle)
				|| (container.getContainer() == null)
				|| !(container.getContainer() instanceof Parameter)
				|| (container.getContainer().getContainer() == null)
				|| !(container.getContainer().getContainer() instanceof NimrodOptimActor)) {
			throw new IllegalActionException(this, "AlgorithmAttribute may only be added to a NimrodOptimActor ");
		}

		m_OptimActor = (NimrodOptimActor)container.getContainer().getContainer();
	}

	@Override
	public String getExpression() {
		return m_Algorithm.getUniqueName();
	}

	@Override
	public void setExpression(String expression) throws IllegalActionException {
		IAlgorithmDefinition def = m_OptimActor.lookupAlgorithm(expression);
		if(def == null) {
			throw new IllegalActionException(this, "Invalid algorithm ID");
		}
		
		m_Algorithm = def;
	}

	@Override
	public String getDisplayName() {
		return m_Algorithm.getPrettyName();
	}
}

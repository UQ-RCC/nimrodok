package au.edu.uq.rcc.nimrod.optim;

import ptolemy.actor.gui.style.ChoiceStyle;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.StringAttribute;

public class AlgorithmicChoiceStyle extends ChoiceStyle {

	private final NimrodOptimActor m_Actor;

	public AlgorithmicChoiceStyle(NamedObj container, String name, NimrodOptimActor actor) throws IllegalActionException, NameDuplicationException {
		super(container, name);
		m_Actor = actor;
	}

	@Override
	protected void _addAttribute(Attribute attribute) throws NameDuplicationException, IllegalActionException {
		/* Here, we stop bad or missing algorithms from being added to the selection menu.
		 * This can happen if something's changed and we're loading from a file. */

		if(!(attribute instanceof StringAttribute)) {
			throw new IllegalActionException("Not a StringAttribute - this is a bug.");
		}

		StringAttribute sa = (StringAttribute)attribute;
		IAlgorithmDefinition algo = m_Actor.lookupAlgorithm(sa.getName());

		if(algo == null) {
			sa.setContainer(null);
			throw new IllegalActionException(this, "No such algorithm");
		}
		super._addAttribute(attribute);
	}
}

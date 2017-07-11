package au.edu.uq.rcc.nimrod.optim;

import java.text.ParseException;
import ptolemy.actor.lib.LimitedFiringSource;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.Settable;

/**
 * <p>
 * <I>DefineSearchSpaceActor</I>Reads a set of parameter lines, which use the
 * Nimrod/O syntax to define the parameter names, data types and domains. Uses
 * the Nimrod Java API class <I>SetOfParams</I> to handle the parsing. This
 * information is then output in the form of a reference to the java class.
 * </p>
 *
 * @author Tom Peachey
 * @author Zane van Iperen
 */
public class DefineSearchSpaceActor extends LimitedFiringSource {

	private final StringParameter m_ParameterDef;
	private SetOfParams m_SetOfParams;
	private boolean m_HasTrigger;

	public DefineSearchSpaceActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		firingCountLimit.setExpression("NONE");

		m_ParameterDef = new StringParameter(this, "Parameters");
		m_ParameterDef.setTypeEquals(BaseType.STRING);
		m_ParameterDef.setDisplayName("Parameter Definition");
		m_ParameterDef.setExpression("parameter x float range from 0 to 1");

		output.setTypeEquals(PortTypes.SETOFPARAMS_TYPE);

		m_SetOfParams = null;
		m_HasTrigger = false;
	}

	@Override
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		super.attributeChanged(attribute);

		if(attribute == m_ParameterDef) {
			try {
				m_SetOfParams = SetOfParams.fromString(m_ParameterDef.stringValue());
			} catch(ParseException e) {
				throw new IllegalActionException(this, e, e.getMessage());
			}
		}
	}

	@Override
	public void initialize() throws IllegalActionException {
		super.initialize();
		m_ParameterDef.setVisibility(Settable.NOT_EDITABLE);
	}

	@Override
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		m_ParameterDef.setVisibility(Settable.FULL);
	}

	@Override
	public boolean prefire() throws IllegalActionException {
		boolean s = super.prefire();
		m_HasTrigger = s && trigger.numberOfSources() > 0;
		return s;
	}

	@Override
	public void fire() throws IllegalActionException {
		super.fire();
		output.send(0, PortTypes.toToken(m_SetOfParams));
	}

	@Override
	public boolean postfire() throws IllegalActionException {
		return m_HasTrigger ? true : super.postfire();
	}
}

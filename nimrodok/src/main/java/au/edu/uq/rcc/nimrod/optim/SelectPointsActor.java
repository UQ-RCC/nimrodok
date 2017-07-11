package au.edu.uq.rcc.nimrod.optim;

import org.monash.nimrod.optim.SelectPoints;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

/**
 * <p>
 * <I>SelectPointsActor</I>Reads a set of parameter lines, which use the Nimrod/O syntax to define the parameter names,
 * data types and domains. Uses the Nimrod Java API class <I>SetOfParams</I> to handle the parsing. This information is
 * then output in the form of a reference to the java class.
 *
 * </p>
 *
 * @author Tom Peachey
 * @author Zane van Iperen
 */
public class SelectPointsActor extends TypedAtomicActor {

	private final StringParameter inPars;
	//private final Parameter populationBased;
	private final TypedIOPort paramInfoPort;
	private final TypedIOPort outputStartingPoints;

	public SelectPointsActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		// Construct input ports.
		paramInfoPort = new TypedIOPort(this, "Search Space", true, false);
		//paramInfoPort.setTypeEquals(SetOfParams.TYPE);
		paramInfoPort.setTypeEquals(PortTypes.SETOFPARAMS_TYPE);
		paramInfoPort.setDisplayName("Search Space");

		// Construct output ports.
		outputStartingPoints = new TypedIOPort(this, "Starting Points", false, true);
		outputStartingPoints.setTypeEquals(PortTypes.POINT_ARRAY_TYPE);

		inPars = new StringParameter(this, "Criterion");
		inPars.setStringMode(true);
		inPars.setDisplayName("Criterion");
	}

	@Override
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		super.attributeChanged(attribute);

		if(attribute == inPars) {
			/* Hijack the behaviour to validate the input. */
			new SelectPoints(inPars.stringValue());
		}
	}

	@Override
	public boolean prefire() throws IllegalActionException {
		if(!paramInfoPort.hasToken(0)) {
			return false;
		}

		return super.prefire();
	}

	@Override
	public void fire() throws IllegalActionException {
		SetOfParams setOfParams = PortTypes.makeSetOfParams(paramInfoPort.get(0));
		SelectPoints setPts = new SelectPoints(inPars.stringValue());

		try {
			setPts.generatePoints(setOfParams);
		} catch(Exception e) {
			throw new IllegalActionException(this, e, "Error in generating points in search space");

		}

		outputStartingPoints.send(0, PortTypes.toToken(setPts.arrayOfPoints));

	}

	private static final long serialVersionUID = 1L;
}

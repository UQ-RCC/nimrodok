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

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.monash.nimrod.NimrodActor.BasicFunctions;
import org.monash.nimrod.NimrodActor.Functions;
import org.monash.nimrod.NimrodActor.NimrodAtomicActor;
import org.monash.nimrod.NimrodActor.TagActor;
import org.monash.nimrod.data.Tag;
import ptolemy.actor.IOPort;
import ptolemy.actor.TypeAttribute;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.BooleanToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.IntToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;

public class CacheActor extends NimrodAtomicActor<BasicFunctions> implements TagActor {

	private final StringParameter m_ParamDriver;
	private final StringParameter m_ParamDBUrl;
	private final StringParameter m_ParamTableName;
	private final Parameter m_ParamNumObjectives;
	private final Parameter m_ParamObjectiveNames;
	private final StringParameter m_ParamProperties;
	private final Parameter m_ParamReadOnly;

	private final TypedIOPort m_InputPort;
	private final TypedIOPort m_EvalPort;

	private int m_NumObjectives;
	private ObjectivePort[] m_ObjectivePorts;
	private TypedIOPort[] m_OutputPorts;

	public final Functions nimrodK;

	private enum OperatingMode {
		NO_ACTIVITY,
		INCOMING_POINT,
		INCOMING_RESULT
	}

	private OperatingMode m_OperatingMode;

	/* Temps, are cleared at the end of each run. */
	private String m_DBUrl;
	private String m_TableName;
	private String[] m_ObjectiveNames;
	private NimCache m_Cache;
	private Properties m_DBProperties;
	private boolean m_ReadOnly;

	public CacheActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		nimrodK = nimrodKFunctions();

		m_InputPort = ActorUtils.createOrGetIOPort(this, "input", false, "WEST");
		m_InputPort.setTypeEquals(PortTypes.POINT_TYPE);

		m_EvalPort = ActorUtils.createOrGetIOPort(this, "eval", true, "WEST");
		m_EvalPort.setTypeEquals(PortTypes.PARAMETER_TYPE);

		m_ParamDriver = new StringParameter(this, "driver");
		m_ParamDriver.setDisplayName("JDBC Driver");
		m_ParamDriver.setExpression("com.mysql.jdbc.Driver");

		m_ParamDBUrl = new StringParameter(this, "dbUrl");
		m_ParamDBUrl.setDisplayName("JDBC URL");
		m_ParamDBUrl.setExpression("jdbc:mysql://localhost:3306/nimcache");

		m_ParamTableName = new StringParameter(this, "tableName");
		m_ParamTableName.setDisplayName("Cache Table");
		m_ParamTableName.setExpression("cache");

		m_ObjectivePorts = new ObjectivePort[1];
		m_ObjectivePorts[0] = ActorUtils.createObjective(this, 0);

		m_OutputPorts = regenerateOutputPorts(new TypedIOPort[0], 1);

		m_NumObjectives = 1;
		m_ParamNumObjectives = setupNumObjectives();

		m_ParamObjectiveNames = new Parameter(this, "objectiveNames");
		m_ParamObjectiveNames.setDisplayName("Objective Names");
		m_ParamObjectiveNames.setTypeEquals(new ArrayType(BaseType.STRING));

		m_ParamProperties = new StringParameter(this, "dbProperties");
		m_ParamProperties.setDisplayName("Connection Properties");
		m_ParamProperties.setExpression("user username\npassword notaverygoodpassword\n");

		m_ParamReadOnly = new Parameter(this, "readOnly");
		m_ParamReadOnly.setDisplayName("Read Only?");
		m_ParamReadOnly.setTypeEquals(BaseType.BOOLEAN);
		m_ParamReadOnly.setToken(BooleanToken.FALSE);

		m_ObjectiveNames = null;
		m_Cache = null;
	}

	private Parameter setupNumObjectives() throws IllegalActionException, NameDuplicationException {
		Parameter param = new Parameter(this, "objectiveCount");
		param.setDisplayName("Number of Objectives");
		param.setTypeEquals(BaseType.INT);
		param.setToken(new IntToken(m_NumObjectives));
		return param;
	}

	@Override
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
		List l = inputPortList();
		for(Iterator ip = l.iterator(); ip.hasNext();) {
			IOPort io = (IOPort)ip.next();
			if(io.getAttribute("tokenConsumptionRate") != null) {
				((Parameter)io.getAttribute("tokenConsumptionRate")).setToken(IntToken.ZERO);
			} else {
				try {
					new Parameter(io, "tokenConsumptionRate", IntToken.ZERO);
				} catch(NameDuplicationException e) {
					//LOGGER.error("Error setting tokenConsumptionRate", e);
				}
			}
		}
	}

	@Override
	public void initialize() throws IllegalActionException {
		super.initialize();

		try {
			Class.forName(m_ParamDriver.stringValue());
		} catch(ClassNotFoundException e) {
			throw new IllegalActionException(this, e, "Invalid DB Driver");
		}

		m_DBUrl = m_ParamDBUrl.stringValue();
		m_TableName = m_ParamTableName.stringValue();

		Token[] objNames = ((ArrayToken)m_ParamObjectiveNames.getToken()).arrayValue();

		if(objNames.length != m_NumObjectives) {
			throw new IllegalActionException(this, "Objective Name count mismatch");
		}

		m_ObjectiveNames = new String[objNames.length];
		for(int i = 0; i < objNames.length; ++i) {
			m_ObjectiveNames[i] = ((StringToken)objNames[i]).stringValue();
		}

		try {
			m_DBProperties = Properties.parseString(m_ParamProperties.stringValue());
		} catch(ParseException e) {
			throw new IllegalActionException(this, e, "Invalid DB properties");
		}

		m_ReadOnly = ((BooleanToken)m_ParamReadOnly.getToken()).booleanValue();
	}

	@Override
	public void wrapup() throws IllegalActionException {
		super.wrapup();
		m_DBUrl = null;
		m_TableName = null;
		m_ObjectiveNames = null;
		m_DBProperties = null;
		if(m_Cache != null) {
			try {
				m_Cache.close();
			} catch(SQLException e) {
				/* We tried... */
			}
		}
		m_Cache = null;
	}

	@Override
	public boolean prefire() throws IllegalActionException {
		m_OperatingMode = OperatingMode.NO_ACTIVITY;

		if(m_InputPort.hasToken(0)) {
			m_OperatingMode = OperatingMode.INCOMING_POINT;
		} else {
			for(ObjectivePort o : m_ObjectivePorts) {
				if(o.hasToken()) {
					m_OperatingMode = OperatingMode.INCOMING_RESULT;
					break;
				}
			}
		}

		return super.prefire() ? (m_OperatingMode != OperatingMode.NO_ACTIVITY) : false;
	}

	@Override
	public void fire() throws IllegalActionException {
		if(m_OperatingMode == OperatingMode.INCOMING_POINT) {

			RecordToken rt = (RecordToken)PortTypes.POINT_TYPE.convert(m_InputPort.get(0));

			SetOfParams sop = PortTypes.makeSetOfParams(rt.get("SetOfParams"));
			if(m_Cache == null) {
				try {
					String[] coordNames = new String[sop.size()];
					for(int i = 0; i < coordNames.length; ++i) {
						coordNames[i] = sop.get(i).name;
					}

					m_Cache = new NimCache(m_DBUrl,
							m_DBProperties.toJavaProperties(), m_TableName,
							coordNames, m_ObjectiveNames);
				} catch(SQLException e) {
					throw new IllegalActionException(this, e, "Error connecting to database");
				}
			}
			PointContainer npc = new PointContainer(PortTypes.makeOptimPoint(rt), m_NumObjectives);
			if(_debugging) {
				this._debug(String.format("Got point: %s", npc.point));
			}
			Batch batch = new Batch(null, new PointContainer[]{npc});

			double[] objectives = null;
			try {
				objectives = m_Cache.lookupPoint(npc.point.coords);
			} catch(SQLException e) {
				throw new IllegalActionException(this, e, "Error looking up point");
			}

			if(objectives == null) {
				this._debug("  Not found in database, evaluating...");
				/* No DB Entry? Evaluate. */
				sendPointForProcessing(npc, batch);
			} else {
				this._debug("  Found, forwarding...");
				/* DB Entry? Forward. */
				forwardResults(npc.point.coords, objectives);
			}

		} else if(m_OperatingMode == OperatingMode.INCOMING_RESULT) {
			/* New result, process it! */
			ObjectivePort.Result result = null;
			for(ObjectivePort o : m_ObjectivePorts) {
				if((result = o.fire()) != null) {
					break;
				}
			}

			if(result.batch.isFinished()) {
				PointContainer pc = result.batch.points[0];

				if(this._debugging) {
					this._debug(String.format("Evaluation finished for %s", pc.point));
				}

				/* Double[] to double[]. */
				double[] obj = new double[pc.objectives.length];
				for(int i = 0; i < obj.length; ++i) {
					obj[i] = pc.objectives[i];
				}

				/* Update the DB. */
				if(!m_ReadOnly) {
					this._debug("Updating database...");
					try {
						m_Cache.addOrUpdate(pc.point.coords, obj);
					} catch(SQLException e) {
						throw new IllegalActionException(this, e, "Error updating database.");
					}
				}

				forwardResults(pc.point.coords, obj);
			}
		}
	}

	/**
	 * "Forward" the results to the output.
	 *
	 * @param coords The coordinates array.
	 * @param objectives The objectives array.
	 * @throws IllegalActionException
	 */
	private void forwardResults(double coords[], double[] objectives) throws IllegalActionException {

		/* Forward the results. */
		for(int i = 0; i < m_NumObjectives; ++i) {
			sendWithCurrentColour(m_OutputPorts[i], new DoubleToken(objectives[i]));
		}
	}

	private void sendWithCurrentColour(TypedIOPort port, Token token) throws IllegalActionException {
		Tag colour;
		if(m_OperatingMode == OperatingMode.INCOMING_POINT) {
			colour = nimrodK.getFiringColour();
		} else {
			colour = nimrodK.getFiringColour().popTag();
		}

		nimrodK.send(port, 0, token, colour);
	}

	private Tag sendPointForProcessing(PointContainer nextPoint, Batch batch) throws IllegalActionException {
		Token token = PortTypes.toToken(nextPoint.point);

		HashMap<String, Token> meta = new HashMap<>();
		meta.put("Optimizer", new ObjectToken(this));
		meta.put("creator", new ObjectToken(this));
		meta.put("point", new ObjectToken(nextPoint));
		meta.put("batch", new ObjectToken(batch));

		Tag colour = null;

		if(m_OperatingMode == OperatingMode.INCOMING_POINT) {
			colour = nimrodK.getFiringColour().pushTag(meta, token);
		} else if(m_OperatingMode == OperatingMode.INCOMING_RESULT) {
			colour = nimrodK.getFiringColour().popTag().pushTag(meta, token);
		}

		nimrodK.send(m_EvalPort, 0, token, colour);
		return colour;
	}

	@Override
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		if(attribute == m_ParamNumObjectives) {
			int numObj = ((IntToken)m_ParamNumObjectives.getToken()).intValue();
			if(numObj < 1) {
				throw new IllegalActionException("Number of objectives must be >= 1");
			}

			if(numObj != m_NumObjectives) {
				int oldNum = m_NumObjectives;
				m_NumObjectives = numObj;

				ObjectivePort[] objTmp;
				TypedIOPort[] outTmp;
				try {
					objTmp = ActorUtils.regenerateObjectivePorts(this, m_ObjectivePorts, numObj);
					outTmp = regenerateOutputPorts(m_OutputPorts, numObj);
				} catch(NameDuplicationException e) {
					m_NumObjectives = oldNum;
					throw new IllegalActionException(e.getMessage());
				}

				m_ObjectivePorts = objTmp;
				m_OutputPorts = outTmp;
			}
		}
	}

	private TypedIOPort[] regenerateOutputPorts(TypedIOPort[] ports, int num) throws IllegalActionException, NameDuplicationException {
		TypedIOPort[] newArray = Arrays.copyOf(ports, num);

		int oldLen = ports.length;

		/* If we're removing objective ports, destroy them. */
		if(num < oldLen) {
			//debugf("Removing...");
			for(int i = num; i < oldLen; ++i) {
				//debugf("  Removing port \"%s\"", m_ObjectivePorts[i].port.getName());
				ports[i].setContainer(null);
			}
			/* If we're adding objective ports, expand them. */
		} else if(num > oldLen) {
			List<TypedIOPort> newPorts = new ArrayList<>();
			try {
				for(int i = oldLen; i < num; ++i) {
					//debugf("  Adding...");
					newArray[i] = createOutputPort(i);
					newPorts.add(newArray[i]);
					//debugf("    Done");
				}
			} catch(IllegalActionException | NameDuplicationException e) {
				/* If adding the ports failed, then delete any new ones. */
				for(TypedIOPort p : newPorts) {
					p.setContainer(null);
				}
				throw e;
			}
		}

		return newArray;
	}

	private TypedIOPort createOutputPort(int i) throws IllegalActionException, NameDuplicationException {
		TypedIOPort port = ActorUtils.createOrGetIOPort(this, String.format("Output%d", i), true, "NORTH");

		/* Set the type. */
		TypeAttribute type = (TypeAttribute)port.getAttribute("type");
		if(type == null) {
			type = new TypeAttribute(port, "type");
		}
		type.setExpression("double");
		port.setTypeEquals(BaseType.DOUBLE);
		return port;
	}

	@Override
	public TagAction tagAction(TypedIOPort input, TypedIOPort output) {
		if(input == m_InputPort && output == m_EvalPort) {
			return TagAction.ADD;
		}

		boolean isResultInput = false;
		for(ObjectivePort p : m_ObjectivePorts) {
			if(input == p.port) {
				isResultInput = true;
				break;
			}
		}

		boolean isResultOutput = false;
		for(TypedIOPort p : m_OutputPorts) {
			if(output == p) {
				isResultOutput = true;
				break;
			}
		}

		if(isResultInput && isResultOutput) {
			return TagAction.REMOVE;
		}

		// Everything else returns the same tag level
		return super.tagAction(input, output);
	}

	private TypedIOPort createPort(String name, boolean isInput, boolean isOutput, String cardinal) throws IllegalActionException, NameDuplicationException {
		TypedIOPort port = new TypedIOPort(this, name, isInput, isOutput);

		StringAttribute cardinalAttribute = (StringAttribute)port.getAttribute("_cardinal");
		if(cardinalAttribute == null) {
			cardinalAttribute = new StringAttribute(port, "_cardinal");
		}
		cardinalAttribute.setExpression(cardinal);

		return port;
	}

	private class Kekek {

	}
}

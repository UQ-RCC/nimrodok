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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.monash.nimrod.NimrodActor.NimrodAtomicActor;
import ptolemy.actor.TypeAttribute;
import ptolemy.actor.TypedAtomicActor;
import ptolemy.actor.TypedIOPort;
import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.ObjectToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;

/**
 * This whole class is a clusterfuck of shit. It'll be cleaned up eventually.
 *
 * @author zane
 */
public class ActorUtils {

	public static TypedIOPort createOrGetIOPort(TypedAtomicActor actor, String portName, boolean output, String cardinal) throws IllegalActionException, NameDuplicationException {
		/* See if the port exists. This will only happen if we've been
		 * loaded from a file or if someone's been naughty. */
		TypedIOPort port = (TypedIOPort)actor.getPort(portName);
		if(port == null) {
			port = new TypedIOPort(actor, portName, !output, output);
		}

		port.setOutput(output);
		port.setInput(!output);

		StringAttribute cardinalAttribute = (StringAttribute)port.getAttribute("_cardinal");
		if(cardinalAttribute == null) {
			cardinalAttribute = new StringAttribute(port, "_cardinal");
		}
		cardinalAttribute.setExpression(cardinal);

		return port;
	}

	/**
	 * Create a new objective input port.
	 *
	 * @param actor The hosting actor..
	 * @param number The objective number.
	 * @return The newly-created objective input port.
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public static ObjectivePort createObjective(NimrodAtomicActor<?> actor, int number) throws IllegalActionException, NameDuplicationException {
		String portName = String.format("Objective%d", number);

		TypedIOPort port = createOrGetIOPort(actor, portName, false, "SOUTH");
		/* Set the type. */
		TypeAttribute type = (TypeAttribute)port.getAttribute("type");
		if(type == null) {
			type = new TypeAttribute(port, "type");
		}
		type.setExpression("double");

		port.setDisplayName(String.format("Evaluation Results %d", number));
		port.setTypeEquals(BaseType.DOUBLE);
		return new ObjectivePort(actor, port, number);
	}

	/**
	 * (Re)generate an optimisation actor's objective ports.
	 *
	 * @param actor The actor.
	 * @param ports The list of objective ports.
	 * @param num The new number of objective ports.
	 * @return The new list of objective ports.
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public static ObjectivePort[] regenerateObjectivePorts(NimrodAtomicActor<?> actor, ObjectivePort[] ports, int num) throws IllegalActionException, NameDuplicationException {
		ObjectivePort[] newArray = Arrays.copyOf(ports, num);

		int oldLen = ports.length;

		/* If we're removing objective ports, destroy them. */
		if(num < oldLen) {
			//debugf("Removing...");
			for(int i = num; i < oldLen; ++i) {
				//debugf("  Removing port \"%s\"", m_ObjectivePorts[i].port.getName());
				ports[i].port.setContainer(null);
			}
			/* If we're adding objective ports, expand them. */
		} else if(num > oldLen) {
			List<ObjectivePort> newPorts = new ArrayList<>();
			try {
				for(int i = oldLen; i < num; ++i) {
					//debugf("  Adding...");
					newArray[i] = createObjective(actor, i);
					newPorts.add(newArray[i]);
					//debugf("    Done");
				}
			} catch(IllegalActionException | NameDuplicationException e) {
				/* If adding the ports failed, then delete any new ones. */
				for(ObjectivePort o : newPorts) {
					o.port.setContainer(null);
				}
				throw e;
			}
		}

		return newArray;
	}

	/**
	 * Make a token suitable for broadcasting on an optimisation actor's result port. This is purely for compatibility
	 * with the old single-objective algorithms.
	 *
	 * @param start The starting point.
	 * @param result The result.
	 * @return The result token.
	 * @throws IllegalActionException
	 */
	@Deprecated
	public static RecordToken makeResultTokenSingle(OptimPoint start, OptimPoint result) throws IllegalActionException {
		return makeResultToken(new ArrayOfPoints(start, 1), new PointContainer[]{new PointContainer(result, 1)});
	}

	public static RecordToken makeResultToken(ArrayOfPoints initial, List<PointContainer> results) throws IllegalActionException {
		return makeResultToken(initial, results.toArray(new PointContainer[results.size()]));
	}

	/**
	 * Make a token suitable for broadcasting on an optimisation actor's result port.
	 *
	 * @param initial The starting points.
	 * @param results The list of results.
	 * @return The result token.
	 * @throws IllegalActionException
	 */
	public static RecordToken makeResultToken(ArrayOfPoints initial, PointContainer[] results) throws IllegalActionException {
		Token[] r = new Token[results.length];

		for(int i = 0; i < r.length; ++i) {
			r[i] = makeResultElementToken(results[i]);
		}

		return new RecordToken(
				new String[]{
					"Results",
					"Initial"
				},
				new Token[]{
					new ArrayToken(r),
					PortTypes.toToken(initial)
				}
		);
	}

	private static RecordToken makeResultElementToken(PointContainer pc) throws IllegalActionException {
		return new RecordToken(new String[]{
			"Point",
			"Objectives"
		}, new Token[]{
			PortTypes.toToken(pc.point),
			makeDoubleArrayTokens(pc.objectives)
		});
	}

	public static ArrayToken makeDoubleArrayTokens(Double[] vals) throws IllegalActionException {
		DoubleToken[] toks = new DoubleToken[vals.length];
		for(int i = 0; i < toks.length; ++i) {
			toks[i] = new DoubleToken(vals[i]);
		}

		return new ArrayToken(toks);
	}

	public static ArrayToken makeDoubleArrayTokens(double[] vals) throws IllegalActionException {
		DoubleToken[] toks = new DoubleToken[vals.length];
		for(int i = 0; i < toks.length; ++i) {
			toks[i] = new DoubleToken(vals[i]);
		}

		return new ArrayToken(toks);
	}

	public static RecordToken makeStatsMessageToken(BaseAlgorithm instance, String message) throws IllegalActionException {
		return makeStatsToken(instance, "msg", new StringToken(message));
	}

	public static RecordToken makeStatsStatsToken(BaseAlgorithm instance, Map<String, String> stats) throws IllegalActionException {
		Map<String, Token> _stats = new HashMap<>();
		for(String key : stats.keySet()) {
			_stats.put(key, new StringToken(stats.get(key)));
		}
		return makeStatsToken(instance, "stats", new RecordToken(_stats));
	}

	public static RecordToken makeStatsPointToken(BaseAlgorithm instance, PointContainer pc) throws IllegalActionException {
		return makeStatsToken(instance, "point", makeResultElementToken(pc));
	}

	public static RecordToken makeStatsParetoToken(BaseAlgorithm instance, List<PointContainer> pcs) throws IllegalActionException {
		if(pcs.isEmpty()) {
			return null;
		}

		ArrayToken[] objectiveTokens = new ArrayToken[pcs.size()];
		for(int i = 0; i < objectiveTokens.length; ++i) {
			objectiveTokens[i] = makeDoubleArrayTokens(pcs.get(i).objectives);
		}

		return makeStatsToken(instance, "pareto", new ArrayToken(objectiveTokens));
	}

	private static RecordToken makeStatsToken(BaseAlgorithm instance, String type, Token payload) throws IllegalActionException {
		return new RecordToken(new String[]{
			"Initial",
			"Instance",
			"Type",
			"Payload"
		}, new Token[]{
			PortTypes.toToken(instance.startingPoints),
			new ObjectToken(instance),
			new StringToken(type),
			payload
		});
	}
}

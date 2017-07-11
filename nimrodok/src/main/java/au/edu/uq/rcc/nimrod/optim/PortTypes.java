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

import ptolemy.data.ArrayToken;
import ptolemy.data.DoubleToken;
import ptolemy.data.RecordToken;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.type.ArrayType;
import ptolemy.data.type.BaseType;
import ptolemy.data.type.RecordType;
import ptolemy.data.type.Type;
import ptolemy.kernel.util.IllegalActionException;

public class PortTypes {

	public static final Type OPTIM_PARAMTER_TYPE;
	public static final Type SETOFPARAMS_TYPE;
	public static final Type PARAMETER_TYPE;
	public static final Type POINT_TYPE;
	public static final Type POINT_ARRAY_TYPE;
	public static final Type STATS_PORT_TYPE;
	public static final Type RESULT_ELEMENT_TYPE;
	public static final Type RESULT_PORT_TYPE;

	static {

		OPTIM_PARAMTER_TYPE = new RecordType(
				new String[]{"Name", "Min", "Max", "Step"},
				new Type[]{BaseType.STRING, BaseType.DOUBLE, BaseType.DOUBLE, BaseType.DOUBLE}
		);

		SETOFPARAMS_TYPE = new ArrayType(OPTIM_PARAMTER_TYPE);

		PARAMETER_TYPE = BaseType.RECORD;

		POINT_TYPE = new RecordType(new String[]{
			"Parameters",
			"SetOfParams"
		}, new Type[]{
			PARAMETER_TYPE,
			SETOFPARAMS_TYPE
		});

		POINT_ARRAY_TYPE = new RecordType(new String[]{
			"SetOfParams",
			"Parameters"
		}, new Type[]{
			SETOFPARAMS_TYPE,
			new ArrayType(PARAMETER_TYPE)
		});

		STATS_PORT_TYPE = new RecordType(new String[]{
			"Initial",
			"Instance",
			"Type",
			"Payload"
		}, new Type[]{
			POINT_ARRAY_TYPE,
			BaseType.OBJECT,
			BaseType.STRING,
			BaseType.GENERAL
		});

		RESULT_ELEMENT_TYPE = new RecordType(
				new String[]{"Point", "Objectives"},
				new Type[]{POINT_TYPE, new ArrayType(BaseType.DOUBLE)}
		);

		RESULT_PORT_TYPE = new RecordType(new String[]{
			"Results",
			"Point"
		}, new Type[]{
			new ArrayType(RESULT_ELEMENT_TYPE),
			POINT_TYPE
		});
	}

	public static RecordToken makeInitialPointsRecordToken(SetOfParams setOfParams, OptimPoint[] points) throws IllegalActionException {
		Token[] ptoks = new Token[points.length];
		for(int i = 0; i < ptoks.length; ++i) {
			ptoks[i] = makeParameterRecordToken(points[i]);
		}

		return new RecordToken(new String[]{
			"SetOfParams",
			"Points"
		}, new Token[]{
			toToken(setOfParams),
			new ArrayToken(ptoks)
		});
	}

	public static Token toToken(OptimParameter param) throws IllegalActionException {
		return new RecordToken(new String[]{
			"Name",
			"Min",
			"Max",
			"Step"
		}, new Token[]{
			new StringToken(param.name),
			new DoubleToken(param.min),
			new DoubleToken(param.max),
			new DoubleToken(param.step)
		});
	}

	public static Token toToken(SetOfParams sop) throws IllegalActionException {
		Token[] params = new Token[sop.size()];
		for(int i = 0; i < params.length; ++i) {
			params[i] = toToken(sop.get(i));
		}

		return new ArrayToken(params);
	}

	public static Token toToken(OptimPoint point) throws IllegalActionException {
		return new RecordToken(
				new String[]{"Parameters", "SetOfParams"},
				new Token[]{makeParameterRecordToken(point), toToken(point.setOfParams)}
		);
	}

	public static Token toToken(ArrayOfPoints aop) throws IllegalActionException {
		Token[] ptoks = new Token[aop.pointArray.length];
		for(int i = 0; i < ptoks.length; ++i) {
			ptoks[i] = makeParameterRecordToken(aop.pointArray[i]);
		}

		return new RecordToken(new String[]{
			"SetOfParams",
			"Parameters"
		}, new Token[]{
			toToken(aop.setOfParams),
			new ArrayToken(ptoks)
		});
	}

	public static OptimParameter makeOptimParameter(Token token) throws IllegalActionException {
		RecordToken tok = (RecordToken)OPTIM_PARAMTER_TYPE.convert(token);

		return new OptimParameter(
				((StringToken)tok.get("Name")).stringValue(),
				((DoubleToken)tok.get("Min")).doubleValue(),
				((DoubleToken)tok.get("Max")).doubleValue(),
				((DoubleToken)tok.get("Step")).doubleValue()
		);
	}

	public static SetOfParams makeSetOfParams(Token token) throws IllegalActionException {
		ArrayToken at = (ArrayToken)SETOFPARAMS_TYPE.convert(token);

		Token[] toks = at.arrayValue();
		OptimParameter[] params = new OptimParameter[toks.length];
		for(int i = 0; i < toks.length; ++i) {
			params[i] = makeOptimParameter(toks[i]);
		}

		return new SetOfParams(params);
	}

	public static OptimPoint makeOptimPointFromParameterToken(Token params, SetOfParams sop) throws IllegalActionException {
		RecordToken rt = (RecordToken)BaseType.RECORD.convert(params);

		String name;

		OptimPoint optPoint = new OptimPoint(sop);

		for(int i = 0; i < sop.size(); i++) {
			name = sop.get(i).name;
			DoubleToken t = (DoubleToken)rt.get(name);
			optPoint.coords[i] = (double)t.doubleValue();
		}

		optPoint.defined = true;

		return optPoint;
	}

	public static OptimPoint makeOptimPoint(Token token) throws IllegalActionException {
		RecordToken rt = (RecordToken)POINT_TYPE.convert(token);
		SetOfParams sop = makeSetOfParams(rt.get("SetOfParams"));

		return makeOptimPointFromParameterToken((RecordToken)rt.get("Parameters"), sop);
	}

	public static ArrayOfPoints makeArrayOfPoints(Token token) throws IllegalActionException {
		RecordToken rt = (RecordToken)PortTypes.POINT_ARRAY_TYPE.convert(token);

		SetOfParams sop = makeSetOfParams(rt.get("SetOfParams"));

		ArrayToken at = (ArrayToken)rt.get("Parameters");

		Token[] toks = at.arrayValue();

		ArrayOfPoints aop = new ArrayOfPoints(sop, toks.length);

		for(int i = 0; i < toks.length; ++i) {
			aop.pointArray[i] = makeOptimPointFromParameterToken((RecordToken)toks[i], sop);
		}

		return aop;
	}

	/**
	 * Convert the parameters into a set of key/value pairs (A RecordToken) e.g. {x1 = 0.13, x2 = -0.15, x3 = 0.08, x4 =
	 * 0.03, ...}
	 *
	 * @return A RecordToken containing the input parameters.
	 * @throws IllegalActionException
	 */
	public static RecordToken makeParameterRecordToken(OptimPoint point) throws IllegalActionException {

		String[] _paramNames = new String[point.numParams];
		for(int i = 0; i < point.numParams; i++) {
			_paramNames[i] = point.setOfParams.get(i).name;
		}

		Token[] _jobSpecs = new Token[point.numParams];

		for(int i = 0; i < point.numParams; ++i) {
			_jobSpecs[i] = new DoubleToken(point.coords[i]);
		}

		return new RecordToken(_paramNames, _jobSpecs);
	}

}

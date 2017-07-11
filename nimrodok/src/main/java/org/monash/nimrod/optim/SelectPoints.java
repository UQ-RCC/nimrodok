package org.monash.nimrod.optim;

import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

//////////////////////////////////////////////////////////////////////////
//// DefineSearchSpace
/**
 * <p>
 * <I>SelectSearchSpacePoints</I>Reads a set of parameter lines, which use the
 * Nimrod/O syntax to define the parameter names, data types and domains. Uses
 * the Nimrod Java API class <I>SetOfParams</I> to handle the parsing. This
 * information is then output in the form of a reference to the java class.
 *
 * </p>
 *
 * @author Tom Peachey
 * @version $Id: NimrodSingleJob.java,v 1.0 2007/03/20 12:00:00 berkley Exp $
 */
public class SelectPoints {

	public SelectPoints(String inString) {
		Debug.write("SelectPoints.constructor, using input: '" + inString + "'", debug, 1);
		userString = inString;
		parseSelectPoints(inString, 0);
		if(pointCount == 0) {
			System.out.println("Warning, no points selected");
		}
		Debug.write("In SelectPoints.constructor, preliminary parsing complete, " + pointCount + " points planned", debug, 1);

	} // constructor

	public SelectPoints() {
		Debug.write("SelectPoints.constructor, using no input", debug, 1);
		selectionMethod = SelectMethod.RANDOM;
		tries = NUM_TRIES_FOR_RANDOM_POINTS;

	} // constructor

	////////////////// Public ports and parameters ///////////////////////
	public enum SelectMethod {
		RANDOM,
		WIDESPACED,
		CENTRESPACED,
		SPECIFIED,
	}

	// class that stores information
	String userString;
	public SetOfParams setOfParams;

	public SelectMethod selectionMethod;

	public int numParams;

	private int startPointSteps[];
	public int pointCount;
	public int dimensionality;
	//public int numStarts;

	public ArrayOfPoints arrayOfPoints;
	public OptimPoint[] pointArray;
	public OptimPoint modelPoint;

	public int pointIndex;

	public int debug = 0;

	public Random rnd = new Random(0);

	public static final int NUM_TRIES_FOR_RANDOM_POINTS = 20000;
	public int tries;

	//int numPoints;
	//OptimPoint[] pointArray;
	//boolean pointsEvaluated;
	///////////////////////////////////////////////////////////////////
	////                        public methods                     ////
	public void generatePoints(SetOfParams inSop) throws NimrodJavaAPIException {
		setOfParams = inSop;
		Debug.write("In SelectPoints.generatePoints: creating " + pointCount + " points", debug, 2);
		arrayOfPoints = new ArrayOfPoints(setOfParams, pointCount);
		pointArray = arrayOfPoints.pointArray;

		modelPoint = pointArray[0];
		dimensionality = arrayOfPoints.dimensionality;
		numParams = modelPoint.numParams;

		parseSelectPoints(userString, 1);
		if(pointCount == 0) {
			System.out.println("Warning, no points selected");
		}
		Debug.write("In SelectPoints.generatePoints, full parsing complete", debug, 2);

		switch(selectionMethod) {
			case RANDOM:
				ObtainRandomStartingPoints(modelPoint, pointCount, pointArray);
				break;
			case WIDESPACED:
				ObtainWideSpacedPoints();
				break;
			case CENTRESPACED:
				ObtainCentreSpacedPoints();
				break;
			case SPECIFIED:

			default:

		}

	}

	public void ObtainRandomStartingPoints(OptimPoint model, int numInitPoints, OptimPoint[] op) {
		boolean ptFound;
		int count;

		for(int pt = 0; pt < numInitPoints; ++pt) {
			count = 0;
			while(true) {
				++count;
				if(count > tries)
					;	// add later
				ptFound = AddRandomPointCoords(model, op[pt]);
				if(ptFound) {
					break;
				}
			}
		}
		/*
			//point = pointArray[pt];

			for(int i=0; i < model.dimensionality; ++i)
			{
				if(model.coordIsInt(i) )
				{
		
					range = model.maxInt[i] - model.minInt[i];
					val = rnd.nextInt(range+1);
					op[pt].coords[i] = (double)(model.minInt[i] + val);
				}	 
				else if(model.coordIsFloat(i) )
				{
					rng = model.max[i] - model.min[i];
					dbl = rnd.nextDouble();
					op[pt].coords[i] =  model.min[i] + dbl*rng;				 
				}			 
				 
			}
			op[pt].defined = true;
		 */

	}

	private boolean AddRandomPointCoords(OptimPoint model, OptimPoint newPoint) {
		double rng, dbl;

		for(int i = 0; i < model.dimensionality; ++i) {
			rng = model.max[i] - model.min[i];
			dbl = rnd.nextDouble();
			newPoint.coords[i] = model.min[i] + dbl * rng;
		}
		newPoint.defined = true;

		return (true);
	}

	public void AugmentRandomStartingPoints(OptimPoint model, int numInitPoints, int numFinalPoints, OptimPoint[] oldPoints, OptimPoint[] newPoints) {
		boolean ptFound;
		//OptimPoint point;
		int count;

		for(int pt = 0; pt < numInitPoints; ++pt) {
			newPoints[pt].copyPoint(oldPoints[pt]);
		}
		for(int pt = numInitPoints; pt < numFinalPoints; ++pt) {
			count = 0;
			while(true) {
				++count;
				if(count > tries)
					;	// add later
				ptFound = AddRandomPointCoords(model, newPoints[pt]);
				if(ptFound) {
					break;
				}
			}
		}
	}

	private int countPoints;

	private void ObtainWideSpacedPoints() throws NimrodJavaAPIException {
		double[] coord;

		countPoints = 0;
		coord = new double[dimensionality];

		recursivelyGeneratePoints(0, 0, coord);

	}

	private void ObtainCentreSpacedPoints() throws NimrodJavaAPIException {
		double[] coord;

		countPoints = 0;
		coord = new double[dimensionality];

		recursivelyGeneratePoints(1, 0, coord);

	}

	private void recursivelyGeneratePoints(int mode, int index, double[] coord) throws NimrodJavaAPIException {
		if(index == numParams) {
			registerPoint(coord);
		} else if(startPointSteps[index] == 0) {
			recursivelyGeneratePoints(mode, index + 1, coord);
		} else if(startPointSteps[index] == 1) {
			coord[index] = modelPoint.min[index] + modelPoint.range[index] / 2.0;
			recursivelyGeneratePoints(mode, index + 1, coord);
		} else {
			for(int i = 0; i < startPointSteps[index]; ++i) {
				if(mode == 0) {
					coord[index] = modelPoint.min[index] + i * modelPoint.range[index] / (startPointSteps[index] - 1);
				} else if(mode == 1) {
					coord[index] = modelPoint.min[index] + (2.0 * i + 1.0) * modelPoint.range[index] / (2.0 * startPointSteps[index]);
				}
				recursivelyGeneratePoints(mode, index + 1, coord);
			}
		}

	}

	private void registerPoint(double[] coord) throws NimrodJavaAPIException {
		if(countPoints >= pointCount) {
			throw new NimrodJavaAPIException("System error 123 in Select Points: more points generated than expected");
		}
		for(int i = 0; i < dimensionality; ++i) {
			pointArray[countPoints].coords[i] = coord[i];
		}
		++countPoints;

	}

	private int parseSelectPoints(String parseString, int mode) {
		tries = NUM_TRIES_FOR_RANDOM_POINTS;

		loadSyntax();

		lines = getLines(parseString);
		lineIndex = -1;
		if(getNextLine() < 0) {
			return (-1);
		}

		return (parseLine(mode));
	}

	/**
	 * 
	 * @param s The string to parse.
	 * @return 
	 */
	private static String[] getLines(String s) {
		String[] rawLines = s.split("\\v+");

		List<String> lines = new ArrayList<>(rawLines.length);

		for(int i = 0; i < rawLines.length; ++i) {
			if((rawLines[i] = rawLines[i].trim()).isEmpty()) {
				continue;
			}

			int idx = rawLines[i].indexOf('#');
			if(idx >= 0) {
				rawLines[i] = rawLines[i].substring(0, idx);
				--i;
				continue;
			}

			lines.add(rawLines[i]);
		}

		return lines.toArray(new String[lines.size()]);
	}

	int getNextLine() {
		int index;
		while(true) {
			++lineIndex;
			if(lineIndex >= lines.length) {
				return (-1);
			}
			fileLine = lines[lineIndex];
			index = fileLine.indexOf('#');
			if(index >= 0) {
				fileLine = fileLine.substring(0, index);
			}
			fileLine = fileLine.trim();
			if(fileLine.length() > 0) {
				length = fileLine.length();
				startPos = 0;
				endPos = 0;
				//state = 0;

				return (0);
			}
		}

	}

	private int parseLine(int mode) {
		int ret;
		/* STARTING POINTS RANDOM
		 * 	 "	" centerspaced <> by <> ...
		 * "	"	widespaced <> by <>  ...
		 * "	"	specified
		 * 				( <>  <>  <> )
		 * 				(<>  <>  <> )
		 * "	"	in file "<>"
		 * randomize reset
		 * randomize timer
		 * randomize seed <> <> <>
		 */

		token = nextToken();
		switch(token) {
			case END_LINE:
				return (0);

			case STARTING_POINTS:
				token = nextToken();
				switch(token) {
					case RANDOM:
						selectionMethod = SelectMethod.RANDOM;
						token = nextToken();
						switch(token) {
							case END_LINE:
								parseRandomPoints();
								break;
							default:
								errorMessage = "Something unexpected after 'random'";
								return (-2);
						}
						return (0);
					case WIDE_SPACED:
						selectionMethod = SelectMethod.WIDESPACED;
						token = nextToken();
						ret = parseDimensions(mode);
						return (ret);
					case CENTRE_SPACED:
						selectionMethod = SelectMethod.CENTRESPACED;
						token = nextToken();
						ret = parseDimensions(mode);
						return (ret);
					case SPECIFIED:
						selectionMethod = SelectMethod.SPECIFIED;
						if(mode == 0) {
							ret = scanPoints();
						} else {
							ret = parseSpecPoints();
						}
						return (ret);
					default:
						errorMessage = "Unknown method for selecting starting points";
						return (-4);

				}
			default:
				return (0);

		}

	}

	//{
	//	pointCount = 0;
	private int parseRandomPoints() {
		if(getNextLine() == -1) {
			errorMessage = "Expecting number of random starts";
			return (-4);

		}
		token = nextToken();
		switch(token) {
			case STARTS:
				token = nextToken();
				switch(token) {
					case NUMBER_INT:
						pointCount = currentInteger;
						return (0);

					default:
						errorMessage = "Expecting a number";
						return (-4);
				}
			case TRIES:
				token = nextToken();
				switch(token) {
					case NUMBER_INT:
						tries = currentInteger;
						return (0);

					default:
						errorMessage = "Expecting a number";
						return (-4);
				}
			default:
				errorMessage = "Expecting number of random starts";
				return (-4);
		}
	}

	private int parseDimensions(int mode) {
		pointCount = 1;

		if(token != Toke.NUMBER_INT) {
			errorMessage = "expecting integer";
			return (-3);
		}
		currentString = fileLine.substring(startPos);
		String[] sss = currentString.trim().split("\\s+");
		int len = sss.length;
		if(len % 2 != 1) {
			errorMessage = "syntax error in specifying steps";
			return (-4);
		}
		numParams = (len + 1) / 2;
		startPointSteps = new int[numParams];
		startPointSteps[0] = currentInteger;
		if(currentInteger != 0) {
			pointCount *= currentInteger;
		}

		for(int i = 1; i < numParams; ++i) {
			token = nextToken();
			if(token != Toke.BY) {
				errorMessage = "expecting the word 'by'";
				return (-5);
			}
			token = nextToken();
			startPointSteps[i] = currentInteger;
			if(token != Toke.NUMBER_INT) {
				errorMessage = "expecting integer";
				return (-6);
			}
			startPointSteps[i] = currentInteger;
			if(currentInteger != 0) {
				pointCount *= currentInteger;
			}
		}
		return (0);

	}

	/*
	 * 	 "	" centerspaced <> by <> ...
	 * "	"	widespaced <> by <>  ...
	 * "	"	specified
	 * 				( <>  <>  <> )
	 * 				(<>  <>  <> )
	 * 
	 * */
	private int scanPoints() /* just a preliminary scan of the points supplied
	 * error checking not possible until parameter
	 * space information supplied
	 */ {
		pointCount = 0;
		while(true) {
			if(getNextLine() == -1) {
				break;
			}
			token = nextToken();
			if(token == Toke.LEFT_PAREN) {
				++pointCount;
			} else {
				errorMessage = "unexpected symbol";
				return (-7);

			}
		}
		if(numParams == 0) {
			errorMessage = "no points specified";
			return (-8);

		}
		return (0);
	}

	private int parseSpecPoints() {
		int count;
		int i;

		count = 0;
		while(true) {
			if(getNextLine() == -1) {
				break;
			}
			if(count >= pointCount) {
				errorMessage = "** System error: There are more points specified than expected";
				return (-7);
			}
			token = nextToken();
			if(token != Toke.LEFT_PAREN) {
				errorMessage = "expecting '('";
				return (-7);
			}

			for(i = 0; i < dimensionality; ++i) {
				token = nextToken();

				if((token == Toke.NUMBER_INT) || (token == Toke.NUMBER_FLOAT)) {
					pointArray[count].coords[i] = currentFloat;
				} else {
					errorMessage = "expecting an number";
					return (-7);

				}
			}
			token = nextToken();
			if(token != Toke.RIGHT_PAREN) {
				errorMessage = "expecting ')' symbol";
				return (-7);
			}

			++count;
		}

		if(count == 0) {
			errorMessage = "no points specified";
			return (-8);

		}
		return (0);

	}

	private enum Toke {
		NOMORELINES,
		UNFINISHED,
		NUMBER_FLOAT,
		NUMBER_INT,
		STRINGG,
		UNFINISHED_STG,
		FORMULA,
		END_LINE,
		NO_MORE_WORDS,
		ILLEGAL_NUMBER_FORMAT,
		CONTROL_CHAR,
		UNFINISHED_VAR_NAME,
		VAR_NAME,
		DUNNO,
		// single char tokens 
		LEFT_PAREN,
		RIGHT_PAREN,
		EQUALS,
		STARTS,
		STARTING_POINTS,
		RANDOM,
		CENTRE_SPACED,
		WIDE_SPACED,
		BY,
		SPECIFIED,
		TRIES

	}

	private static Toke nextToken() {
		int pos;
		char ch;
		String str;

		pos = endPos;

		while(pos < length) {
			if(Character.isWhitespace(fileLine.charAt(pos))) // skip prelim whitespace
			{
				++pos;
			} else {
				break;
			}

		}
		if(pos >= length) {
			//endPos = pos-1;

			return (Toke.END_LINE);
		}

		startPos = pos;
		ch = fileLine.charAt(pos);

		if(ch == '"') {
			while(true) {
				++pos;
				if(pos >= length) {
					endPos = pos;
					return (Toke.UNFINISHED_STG);
				}
				ch = fileLine.charAt(pos);
				if(Character.isISOControl(ch)) {
					endPos = pos;
					return (Toke.CONTROL_CHAR);
				}
				if(ch == '\"') {
					++pos;
					break;
				}
			}
			endPos = pos;
			currentString = fileLine.substring(startPos, endPos);
			return (Toke.STRINGG);
		} else if((Character.isDigit(ch)) || (ch == '.') || (ch == '-')) {
			while((Character.isDigit(ch)) || (ch == '.') || (ch == '-') || (ch == 'E') || (ch == 'e')) {
				++pos;
				if(pos >= length) {
					//--pos;
					break;
				}
				ch = fileLine.charAt(pos);
			}
			endPos = pos;
			str = fileLine.substring(startPos, endPos);

			try {
				currentInteger = Integer.valueOf(str);
				currentFloat = (double) currentInteger;

			} catch(NumberFormatException e1) {
				try {
					currentFloat = Double.valueOf(str);
				} catch(NumberFormatException e2) {
					// not a number really
					return (Toke.ILLEGAL_NUMBER_FORMAT);
				}
				return (Toke.NUMBER_FLOAT);
			}
			return (Toke.NUMBER_INT);
		} // single char tokenns
		else if((ch == ';') || (ch == '#')) {
			endPos = pos + 1;
			return (Toke.END_LINE);
		} else if(ch == '{') {
			endPos = pos + 1;
			return (Toke.LEFT_PAREN);
		} else if(ch == '}') {
			endPos = pos + 1;
			return (Toke.RIGHT_PAREN);
		} else if(ch == '=') {
			endPos = pos + 1;
			return (Toke.EQUALS);
		} else // keyword case maybe
		{
			while(pos < length) {
				++pos;
				if(pos == length) {
					break;
				}
				ch = fileLine.charAt(pos);
				if((Character.isWhitespace(ch)) || (ch == ';') || (ch == '#')) {
					break;
				}
			}

			endPos = pos;

			currentWord = fileLine.substring(startPos, endPos);

			Toke thistoken = hmp.get(currentWord.toLowerCase());
			if(thistoken == null) {
				return (Toke.DUNNO);
			} else {
				return (thistoken);
			}

		}
	}

	public void showParsingError() {
		int i;
		String indicatorLine;
		System.out.println("**Parsing error: " + errorMessage);
		System.out.println("  " + fileLine);

		indicatorLine = "";
		for(i = 0; i < startPos; ++i) {
			indicatorLine = indicatorLine + " ";

		}
		indicatorLine = indicatorLine + "|";
		if(endPos > startPos + 1) {
			for(; i < endPos - 2; ++i) {
				indicatorLine = indicatorLine + " ";
			}

			indicatorLine = indicatorLine + "|";
		}
		System.out.println("  " + indicatorLine);

	}

// for parsing
	private static String fileLine = null;
	int lineIndex;
	String[] lines;

	private static int length;
	private static int startPos;
	private static int endPos;

	private static Map<String, Toke> hmp;
	//private static int state;

	private static String errorMessage;
	private static String currentWord;
	private static String currentString;
	private static int currentInteger;
	private static double currentFloat;

	private static Toke token;

	private int loadSyntax() {

		hmp = new HashMap<String, Toke>();

		hmp.put("startingpoints", Toke.STARTING_POINTS);
		hmp.put("starts", Toke.STARTS);
		hmp.put("random", Toke.RANDOM);
		hmp.put("centrespaced", Toke.CENTRE_SPACED);
		hmp.put("centerspaced", Toke.CENTRE_SPACED);
		hmp.put("widespaced", Toke.WIDE_SPACED);
		hmp.put("by", Toke.BY);
		hmp.put("specified", Toke.SPECIFIED);

		return (0);
	}

}

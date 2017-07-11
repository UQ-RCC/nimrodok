package org.monash.nimrod.optim;

//import ptolemy.kernel.util.IllegalActionException;
//import java.lang.RuntimeException;
//import java.lang.Math;
import au.edu.uq.rcc.nimrod.optim.ArrayOfPoints;
import au.edu.uq.rcc.nimrod.optim.OptimPoint;
import au.edu.uq.rcc.nimrod.optim.SetOfParams;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//import ptolemy.data.Token;
//import OptimParameter.Toke;
//import OptimParameter.Toke;
public class SubdivisionAlgorithm extends OptimizationAlgorithm {

	public int optimIndex;

	// settings for optimization method
	public int maxIters = 1000;
	public Boolean onErrorFail = true;
	public Boolean driftAllowed = true;

	// search space and points in it
	int dimensionality;

	double[] below;
	double[] above;
	double[] range;
	int[] steps;
	double[] stepSize;

	public ArrayOfPoints evalSet;
	public int pointsEvaluated;

	int pointCount;
	OptimPoint model;

	boolean fullParsing = false;
	public boolean parsingSuccessful = false;
	public boolean stepsDefined = false;

	boolean pointsReady = false;

	//alternativePoint = new OptimPoint[4]; 
	public int iteration;
	boolean evaluated;
	boolean first;
	// now defined in aprent:  SetOfParams setOfParams;

	int indexOfBestPoint;
	/* index for best vertex */
	//int greatest;	  		/* worst vertex */
	//int next_greatest;	  		/* second worst vertex */

	//ArrayOfPoints resultPoints;
	public OptimPoint resultPoint;

	SubdivisionAlgorithm(String inputString) {
		//System.out.println("SubdivisionAlgorithm: constructor, without full parsing");
		fullParsing = false;
		int ret = ParseSettings(inputString);
		parsingSuccessful = (ret == 0);
		//System.out.println("SubdivisionAlgorithm: constructor, parsingSuccessful is "+parsingSuccessful);
	}

	SubdivisionAlgorithm(String inputString, OptimPoint pnt) {
		//System.out.println("SubdivisionAlgorithm: constructor, with full parsing");
		model = pnt;
		setOfParams = pnt.setOfParams;
		dimensionality = model.dimensionality;
		allocateArrays(model);

		fullParsing = true;
		int ret = ParseSettings(inputString);
		parsingSuccessful = (ret == 0);
		//System.out.println("SubdivisionAlgorithm: constructor, parsingSuccessful is "+parsingSuccessful);
		createBoundingBox(pnt);

	}

	SubdivisionAlgorithm(String inputString, SetOfParams sopIn) {

		//System.out.println("SubdivisionAlgorithm: constructor, with full parsing");
		setOfParams = sopIn;
		OptimPoint pnt = new OptimPoint(sopIn);
		model = pnt;
		dimensionality = model.dimensionality;
		allocateArrays(model);

		fullParsing = true;
		int ret = ParseSettings(inputString);
		parsingSuccessful = (ret == 0);
		//System.out.println("SubdivisionAlgorithm: constructor, parsingSuccessful is "+parsingSuccessful);
		createBoundingBox(pnt);

	}

	SubdivisionAlgorithm(String inputString, OptimPoint pnt, int index) {
		this(inputString, pnt);
		optimIndex = index;
	}

	SubdivisionAlgorithm(String inputString, SetOfParams sopIn, int index) {
		this(inputString, sopIn);
		optimIndex = index;
	}

	public SubdivisionAlgorithm(int maxIters, boolean drift, int[] steps, OptimPoint start) {
		this.maxIters = maxIters;
		this.driftAllowed = drift;
		if(steps.length != start.dimensionality) {
			throw new IllegalArgumentException("step count mismatch");
		}

		this.steps = Arrays.copyOf(steps, steps.length);

		model = new OptimPoint(start);
		setOfParams = model.setOfParams;
		dimensionality = model.dimensionality;
		allocateArrays(model);
		createBoundingBox(model);
	}

	private void allocateArrays(OptimPoint pnt) {
		below = new double[dimensionality];
		above = new double[dimensionality];
		range = new double[dimensionality];
		steps = new int[dimensionality];
		stepSize = new double[dimensionality];

	}

	/* Bounding box of the search space, defined by lower bounds below[]
	 * and the upper bounds above[]
	 */
	private void createBoundingBox(OptimPoint pnt) {
		for(int i = 0; i < dimensionality; ++i) {
			below[i] = pnt.min[i];
			above[i] = pnt.max[i];
			range[i] = above[i] - below[i];
			stepSize[i] = range[i] / steps[i];
		}

		iteration = 0;
		pointsReady = false;
		first = true;

	}

	public void selectSubdomain() {
		/* If all the points on the grid are evaluated then pick
		 * the optimum and determine the new bounding box around it. 
		 */

		OptimPoint[] pA = evalSet.pointArray;  // just to save typing
		int index_of_lowest;
		int pointCount = evalSet.numPoints;
		double tryit;

		//System.out.println("SubdivisionAlgorithm: selectSubdomain");
		//boolean drift_allowed;
		/* double *run_array; */
 /* double *costs; */
 /* double *objectives; */
		//double tolerance;
		double lowest_cost;
		double highest_cost;
		//int first;
		//double tol_compare;
		//int index_of_lowest;
		int[] best;

		//double store_val[MAX_DIMS];
		//int point_count;
		//int num_points;
		int dim;

		int i;

		best = new int[dimensionality];
		++iteration;
		/* Find minimum and maximum costs and point corresponding to minimum. */
 /* When coord values are rounded this can lead to problems if we always */
 /* take	the first minimum in the list. So oscillate between first and last */
		if(first) {
			first = false;
			lowest_cost = pA[0].cost;
			highest_cost = pA[0].cost;

			index_of_lowest = 0;
			for(i = 1; i < pointCount; ++i) {
				if(pA[i].cost < lowest_cost) {
					lowest_cost = pA[i].cost;
					index_of_lowest = i;
				} else if(pA[i].cost > highest_cost) {
					highest_cost = pA[i].cost;
				}
			}
		} else {
			first = true;
			lowest_cost = pA[pointCount - 1].cost;
			highest_cost = pA[pointCount - 1].cost;

			index_of_lowest = pointCount - 1;
			for(i = pointCount - 2; i >= 0; --i) {
				if(pA[i].cost < lowest_cost) {
					lowest_cost = pA[i].cost;
					index_of_lowest = i;
				} else if(pA[i].cost > highest_cost) {
					highest_cost = pA[i].cost;
				}
			}
		}
		indexOfBestPoint = index_of_lowest;
		//System.out.println("SubdivisionAlgorithm: selectSubdomain, best point has index "+index_of_lowest);

		/* now identify the coordinate indices (not the actual coords) for the optimum */
 /* this is a little tricky */
		for(dim = dimensionality - 1; dim >= 0; --dim) {
			best[dim] = index_of_lowest % (steps[dim] + 1);
			index_of_lowest = index_of_lowest / (steps[dim] + 1);
		}

		/* check for convergence 
		if(utilitiesProportionalDifferenceIsSmall(highest_cost, lowest_cost, tolerance, &tol_compare, debug, prefix) )
		{
			op_stat->how_opt_ended = 0;
			break;
		}
		 */

 /* for each coordinate, decide on bounds for next iteration */
		for(dim = 0; dim < dimensionality; ++dim) {

			if(best[dim] == 0) /* extreme left of interval */ {
				above[dim] = below[dim] + stepSize[dim];
				if(driftAllowed == true) {
					tryit = below[dim] - stepSize[dim];
					if(tryit < model.min[dim]) {
						below[dim] = model.min[dim];
					} else {
						below[dim] = tryit;
					}
				} else {
					/* below[dim] = below[dim];  no change */
				}
			} else if(best[dim] == steps[dim]) /* extreme right of interval */ {
				below[dim] = above[dim] - stepSize[dim];
				if(driftAllowed == true) {
					tryit = above[dim] + stepSize[dim];
					if(tryit > model.max[dim]) {
						above[dim] = model.max[dim];
					} else {
						above[dim] = tryit;
					}
				} else {
					/* above[dim] = above[dim];   no change */
				}
			} else /* optimum is inside interval */ {
				above[dim] = below[dim] + (best[dim] + 1) * stepSize[dim];
				below[dim] = below[dim] + (best[dim] - 1) * stepSize[dim];
			}
			range[dim] = above[dim] - below[dim];
			stepSize[dim] = range[dim] / steps[dim];
		}

		for(i = 0; i < dimensionality; ++i) {
			//System.out.println("SubdivisionAlgorithm: selectSubdomain, for dim "+i+" below is "+below[i]);
			//System.out.println("SubdivisionAlgorithm: selectSubdomain, for dim "+i+" above is "+above[i]);
			//System.out.println("SubdivisionAlgorithm: selectSubdomain, for dim "+i+" range is "+range[i]);
			//System.out.println("SubdivisionAlgorithm: selectSubdomain, for dim "+i+" stepSize is "+stepSize[i]);

		}

		//stepSize[i] = range[i]/steps[i];
	}

	/* end of iterations */
	public void Subdivide() {
		/* Determine the grid of points based on the current 
		bounding box
		 */
		//System.out.println("SubdivisionAlgorithm: Subdivide");

		double[] values = new double[dimensionality];

		int count = 1;
		for(int dim = 0; dim < dimensionality; ++dim) {
			count *= (steps[dim] + 1);
			//System.out.println("SubdivisionAlgorithm: Subdivide allocated "+count+ " points in evaluation set");
		}
		evalSet = new ArrayOfPoints(model, count);
		//System.out.println("SubdivisionAlgorithm: Subdivide allocated "+count+ " points in evaluation set");

		pointCount = 0;
		RecursionOnCoordsForSubdivision(0, 0, values);

		pointsReady = true;
	}

	private int RecursionOnCoordsForSubdivision(int level, int dim, double[] values) {
		for(int stepCount = 0; stepCount <= steps[dim]; ++stepCount) {
			values[dim] = below[dim] + stepCount * stepSize[dim];
			//System.out.println("RecursionOnCoordsForSubdivision values["+dim+"]  is "+values[dim] );
			if(dim == dimensionality - 1) {
				evalSet.pointArray[pointCount].copyCoords(values);
				++pointCount;
				//System.out.println("SubdivisionAlgorithm: Subdivide defined coords for point "+pointCount);
			} else {
				RecursionOnCoordsForSubdivision(level + 1, dim + 1, values);
			}
		}
		return (0);

	}

	public void ProduceResultPoints() {
		resultPoint = new OptimPoint(evalSet.pointArray[indexOfBestPoint]);
	}

	// *********  private	
	private int ParseSettings(String parseString) {
		/* default values are sufficient for a subdivision run,
		except for the array of number of steps that must be found 
		by the parser */
		stepsDefined = false;
		// flag ==0, preliminary mode
		// flag == 1, full parsing
		onErrorFail = false;
		int ret = 0;
		//System.out.println("SubdivisionAlgorithm: ParseSettings");

		try {
			loadSyntax();

			String[] lines;
			lines = parseString.split("\n");
			for(int i = 0; i < lines.length; ++i) {
				fileLine = lines[i];
				length = fileLine.length();
				startPos = 0;
				endPos = 0;
				state = 0;
				ret = parseLine();
				if(ret != 0) {
					//System.out.println("SubdivisionAlgorithm: ParseSettings, parseLine returned "+ret);
					showParsingError();
					throw new IllegalArgumentException(errorMessage);
				}
			}
			if(!stepsDefined) {
				throw new IllegalArgumentException("Subdivision Optimization lacks a 'steps' setting");
			}
		} catch(Exception e) {
			System.err.println("Parser exception: " + e.getMessage());

		}

		return (ret);

	}

	private int parseLine() {

		while(true) {
			switch(state) {
				case 0:
					token = nextToken();
					switch(token) {
						case END_LINE:
							return (0);
						case END_METHOD:
							state = 100;
							break;
						case METHOD:
							token = nextToken();
							switch(token) {
								case DUNNO:
									if(currentWord.equalsIgnoreCase("subdivision")) {
										state = 0;
										break;
									} else {
										errorMessage = "Expecting 'method subdivision'";
										return (-1);
									}

								default:
									errorMessage = "Expecting 'method subdivision'";
									return (-1);
							}
							break;
						case MAX_ITERS:
							token = nextToken();
							switch(token) {
								case NUMBER_INT:
									maxIters = currentInteger;
									state = 0;
									break;
								default:
									errorMessage = "Expecting integer value for maximum iterations";
									return (-1);
							}
							break;
						case DRIFT:
							token = nextToken();
							switch(token) {
								case ALLOWED:
									driftAllowed = true;
									state = 0;
									break;
								case DISABLED:
									driftAllowed = false;
									state = 0;
									break;
								default:
									errorMessage = "Expecting 'drift allowed' or 'drift disabled'";
									return (-1);
							}
							break;
						case STEPS:
							int ret = scanSteps();
							if(ret != 0) {
								stepsDefined = false;
								return (-1);
							}
							stepsDefined = true;
							state = 0;
							break;
						case ON_ERROR:
							token = nextToken();
							switch(token) {
								case FAIL:
									onErrorFail = true;
									state = 0;
									break;
								case IGNORE:
									onErrorFail = false;
									state = 0;
									break;
								default:
									errorMessage = "Expecting either 'fail' or 'ignore'";
									return (-1);
							}
							break;
						default:
							errorMessage = "Unxpecting keyword'";
							return (-4);
					}
					break;
				case 100:
					return 0;
			}
		}

	}

	private int scanSteps() {

		//System.out.println("SubdivisionAlgorithm: scanSteps, dimensionality is "+dimensionality);
		int i = 0;
		while(true) {
			token = nextToken();
			switch(token) {
				case END_LINE:
					if(i < dimensionality) {
						errorMessage = "Number of steps exceeds the number of dimensions";
						return (-1);
					}
					return (0);
				case NUMBER_INT:
					if(fullParsing) {
						if(i >= dimensionality) {
							errorMessage = "Number of steps exceeds the number of dimensions";
							return (-1);
						}
						steps[i] = currentInteger;
						//System.out.println("steps[i]  is "+steps[i]);

						i++;
					}
					continue;
				default:
					errorMessage = "Expecting integer value";
					return (-1);
			}
		}

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
		METHOD,
		END_METHOD,
		TOLERANCE,
		DRIFT,
		ALLOWED,
		DISABLED,
		STEPS,
		ON_ERROR,
		FAIL,
		IGNORE,
		MAX_ITERS,

	}

	private Toke nextToken() {
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
				currentFloat = (double)currentInteger;

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

			//System.out.println("startPos is "+startPos);
			endPos = pos;

			currentWord = fileLine.substring(startPos, endPos);
			//System.out.println("currentWord is "+currentWord);

			Toke thistoken = hmp.get(currentWord.toLowerCase());
			if(thistoken == null) {
				return (Toke.DUNNO);
			} else {
				return (thistoken);
			}

		}
	}

	private void showParsingError() {
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

	public boolean parsingSuccessful() {
		return (parsingSuccessful);
	}

// for parsing
	private String fileLine = null;
	private int length;
	private int startPos;
	private int endPos;
	//private static String quotedString;
	//private static int integer;
	//private static Double doubl;
	private Map<String, Toke> hmp;
	private int state;

	public String errorMessage;
	private String currentWord;
	public String currentString;
	private int currentInteger;
	private double currentFloat;

	private Toke token;

	// string parsing
	private int loadSyntax() {
		currentFloat = 42.2;
		double x = currentFloat;
		currentFloat = x;

		hmp = new HashMap<String, Toke>();
		hmp.put("method", Toke.METHOD);
		hmp.put("endmethod", Toke.END_METHOD);
		hmp.put("tolerance", Toke.TOLERANCE);
		hmp.put("onerror", Toke.ON_ERROR);
		hmp.put("fail", Toke.FAIL);
		hmp.put("ignore", Toke.IGNORE);
		hmp.put("drift", Toke.DRIFT);
		hmp.put("allowed", Toke.ALLOWED);
		hmp.put("disabled", Toke.DISABLED);
		hmp.put("steps", Toke.STEPS);
		hmp.put("maxiterations", Toke.MAX_ITERS);

		return (0);
	}

}

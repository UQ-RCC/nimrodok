package org.monash.nimrod.optim;

import java.util.*;

public class SimplexSettings extends Settings {

	public enum Toke {
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
		ORIGINAL_METHOD,
		MAX_ITERS,
		ON_ERROR,
		FAIL,
		IGNORE,

	}
	
	private static final HashMap<String, Toke> strTok; // strtok. geddit?

	static {
		strTok = new HashMap<String, Toke>();
		strTok.put("method", Toke.METHOD);
		strTok.put("endmethod", Toke.END_METHOD);
		strTok.put("tolerance", Toke.TOLERANCE);
		strTok.put("originalmethod", Toke.ORIGINAL_METHOD);
		strTok.put("maxiterations", Toke.MAX_ITERS);
		strTok.put("onerror", Toke.ON_ERROR);
		strTok.put("fail", Toke.FAIL);
		strTok.put("ignore", Toke.IGNORE);
	}


	
	private int length;

	private int state;

	private String currentWord;
	private String currentString;
	private double currentFloat;
	private int currentInteger;

	private Toke token;

	public boolean onErrorFail;

	
	public double tolerance;
	public boolean originalMethod;
	public int maxIters;
	
	
	public SimplexSettings(String parseString) {
		tolerance = 0.0;
		originalMethod = false;
		maxIters = 1000;
		onErrorFail = false;

		String[] lines;
		lines = parseString.split("\n");
		for(int i = 0; i < lines.length; ++i)
		{
			fileLine = lines[i];
			length = fileLine.length();
			startPos = 0;
			endPos = 0;
			state = 0;
			int ret = parseLine();
			if(ret != 0)
			{
				showParsingError(System.err);
				throw new IllegalArgumentException(errorMessage);
			}				
		}
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
									if(currentWord.equalsIgnoreCase("simplex")) {
										state = 0;
										break;
									} else {
										errorMessage = "Expecting 'method simplex': " + fileLine.substring(startPos);
										return (-1);
									}

								default:
									errorMessage = "Expecting 'method simplex': " + fileLine.substring(startPos);
									return (-1);
							}
							break;
						case TOLERANCE:
							token = nextToken();
							switch(token) {
								case NUMBER_INT:
									tolerance = currentInteger;
									state = 0;
									break;
								case NUMBER_FLOAT:
									tolerance = currentFloat;
									state = 0;
									break;
								default:
									errorMessage = "Expecting tolerance value: " + fileLine.substring(startPos);
									return (-1);
							}
							break;
						case ORIGINAL_METHOD:
							originalMethod = true;
							state = 0;
							break;
						case MAX_ITERS:
							token = nextToken();
							switch(token) {
								case NUMBER_INT:
									maxIters = currentInteger;
									state = 0;
									break;
								default:
									errorMessage = "Expecting integer value for maximum iterations: " + fileLine.substring(startPos);
									return (-1);
							}
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
									errorMessage = "Expecting either 'fail' or 'ignore': " + fileLine.substring(startPos);
									return (-1);
							}
							break;
						default:
							errorMessage = "Unxpecting keyword: " + fileLine.substring(startPos);
							return (-4);
					}
					break;
				case 100:
					return 0;
			}
		}

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

			endPos = pos;

			currentWord = fileLine.substring(startPos, endPos);
			//Debug.write("currentWord is "+currentWord);

			Toke thistoken = strTok.get(currentWord.toLowerCase());
			if(thistoken == null) {
				return (Toke.DUNNO);
			} else {
				return (thistoken);
			}

		}
	}
}

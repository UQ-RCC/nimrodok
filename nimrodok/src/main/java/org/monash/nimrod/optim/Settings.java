package org.monash.nimrod.optim;

import java.io.*;

public class Settings {

	protected String errorMessage;
	protected String fileLine = null;
	protected int startPos;
	protected int endPos;

	protected final void showParsingError(PrintStream stream) {
		stream.printf("**Parsing error: %s\n", errorMessage);
		stream.printf("  %s\n", fileLine);

		stream.print("  ");

		int i;

		for(i = 0; i < startPos; ++i) {
			stream.print(" ");
		}

		stream.print("|");

		if(endPos > startPos + 1) {
			for(; i < endPos - 2; ++i) {
				stream.print(" ");
			}

			stream.print("|");
		}

		stream.print('\n');
	}
}

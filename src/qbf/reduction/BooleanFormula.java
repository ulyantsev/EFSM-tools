package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public abstract class BooleanFormula {
	private static final boolean USE_COPROCESSOR = true;
	
	public static class DimacsConversionInfo {
		private final StringBuilder dimacsBuilder = new StringBuilder();
		private final Map<Integer, Integer> limbooleNumberToDimacs = new HashMap<>();
		private final Map<Integer, Integer> dimacsNumberToLimboole = new HashMap<>();
		private String title;
		private Integer varCount;

		void acceptLine(String line) {
			if (line.startsWith("c")) {
				String[] tokens = line.split(" ");
				assert tokens.length == 3;
				int first = Integer.parseInt(tokens[1]);
				int second = Integer.parseInt(tokens[2]);
				limbooleNumberToDimacs.put(second, first);
				dimacsNumberToLimboole.put(first, second);
			} else if (line.startsWith("p")) {
				title = line;
				varCount = Integer.parseInt(line.split(" ")[2]);
			} else {
				dimacsBuilder.append(line + "\n");
			}
		}
		
		public String title() {
			assert title != null;
			return title;
		}
		
		public String output() {
			return dimacsBuilder.toString();
		}
		
		public Optional<Integer> toDimacsNumber(int num) {
			return Optional.of(limbooleNumberToDimacs.get(num));
		}
		
		public int varCount() {
			return varCount;
		}
		
		public Optional<Integer> toLimbooleNumber(int num) {
			return Optional.ofNullable(dimacsNumberToLimboole.get(num));
		}
	}
	
	public abstract String toLimbooleString();
	
	public DimacsConversionInfo toDimacs(Logger logger) throws IOException {
		final String beforeLimbooleFilename = "_tmp.limboole";
		DimacsConversionInfo info = new DimacsConversionInfo();

		final String limbooleInput = toLimbooleString();
		
		try (PrintWriter pw = new PrintWriter(beforeLimbooleFilename)) {
			pw.print(limbooleInput);
		}
		
		if (!USE_COPROCESSOR) {
			final String limbooleStr = "limboole -d -s " + beforeLimbooleFilename;
			logger.info(limbooleStr);
			Process p = Runtime.getRuntime().exec(limbooleStr);
			try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				input.lines().forEach(info::acceptLine);
			}
		} else {
			final String afterLimbooleFilename = "_tmp.after.limboole.dimacs";
			
			final String mapFilename = "_tmp.map";
			final String whiteVarFilename = "_tmp.white.var";
			
			// transforming formula to DIMACS
			final String limbooleStr = "limboole -d -s -o " + afterLimbooleFilename + " " + beforeLimbooleFilename;
			logger.info(limbooleStr);
			Process limboole = Runtime.getRuntime().exec(limbooleStr);
			try {
				limboole.waitFor();
			} catch (InterruptedException e) {
				throw new AssertionError();
			}
			
			// obtaining variable name mapping
			try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(afterLimbooleFilename)))) {
				input.lines().filter(l -> l.startsWith("c")).forEach(info::acceptLine);
			}
			
			// writing variables we cannot exclude from DIMACS CNF
			try (PrintWriter pw = new PrintWriter(whiteVarFilename)) {
				info.dimacsNumberToLimboole.keySet().forEach(pw::println);
			}
			
			// simplifying CNF, not excluding important variables
			final String coprocessorStr = "coprocessor " + afterLimbooleFilename + " -CP_mapFile=" + mapFilename + " -CP_print=1 -CP_whiteFile=" + whiteVarFilename;
			logger.info(coprocessorStr);
			Process coprocessor = Runtime.getRuntime().exec(coprocessorStr);
			
			try (BufferedReader input = new BufferedReader(new InputStreamReader(coprocessor.getInputStream()))) {
				input.lines().forEach(info::acceptLine);
			}
		}

		return info;
	}
	
	public BooleanFormula not() {
		return new NotOperation(this);
	}
	
	public BooleanFormula and(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.AND);
	}
	
	public BooleanFormula or(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.OR);
	}
	
	public BooleanFormula implies(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.IMPLIES);
	}
	
	public BooleanFormula equivalent(BooleanFormula other) {
		return new BinaryOperation(this, other, BinaryOperations.EQ);
	}
}

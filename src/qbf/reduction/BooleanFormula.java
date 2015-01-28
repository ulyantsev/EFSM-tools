package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

public abstract class BooleanFormula {
	private static final boolean USE_COPROCESSOR = false;
	
	public static Optional<Assignment> fromDimacsToken(String token, DimacsConversionInfo dimacs) {
		boolean isTrue = token.charAt(0) != '-';
		if (!isTrue) {
			token = token.substring(1);
		}
		int dimacsIndex = Integer.parseInt(token);
		Optional<Integer> limbooleIndex = dimacs.toLimbooleNumber(dimacsIndex);
		return limbooleIndex.map(index -> new Assignment(BooleanVariable.getVarByNumber(index), isTrue));
	}
	
	public static Pair<List<Assignment>, Long> solveAsSat(String formula, Logger logger, String solverParams, int timeoutSeconds) throws IOException {
		logger.info("Final SAT formula length: " + formula.length());
		DimacsConversionInfo info = BooleanFormula.toDimacs(formula, logger);
		final String dimaxFilename = "_tmp.dimacs";
		try (PrintWriter pw = new PrintWriter(dimaxFilename)) {
			pw.print(info.title() + "\n" + info.output());
		}
		logger.info("CREATED DIMACS FILE");
		
		long time = System.currentTimeMillis();
		List<Assignment> list = new ArrayList<>();
		String solverStr = "cryptominisat --maxtime=" + timeoutSeconds + " " + dimaxFilename + " " + solverParams;
		logger.info(solverStr);
		Process depqbf = Runtime.getRuntime().exec(solverStr);
		
		try (BufferedReader input = new BufferedReader(new InputStreamReader(depqbf.getInputStream()))) {
			input.lines().filter(s -> s.startsWith("v")).forEach(certificateLine ->
				Arrays.stream(certificateLine.split(" ")).skip(1).forEach(token ->
					fromDimacsToken(token, info).ifPresent(list::add)
				)
			);
		}
		
		time = System.currentTimeMillis() - time;
		return Pair.of(list, time);
	}
	
	public static class DimacsConversionInfo {
		private final StringBuilder dimacsBuilder = new StringBuilder();
		private final Map<Integer, Integer> limbooleNumberToDimacs = new LinkedHashMap<>();
		private final Map<Integer, Integer> dimacsNumberToLimboole = new LinkedHashMap<>();
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
	
	public static DimacsConversionInfo toDimacs(String limbooleFormula, Logger logger) throws IOException {
		final String beforeLimbooleFilename = "_tmp.limboole";
		final DimacsConversionInfo info = new DimacsConversionInfo();
		final String afterLimbooleFilename = "_tmp.after.limboole.dimacs";
		
		try (PrintWriter pw = new PrintWriter(beforeLimbooleFilename)) {
			pw.print(limbooleFormula);
		}
		
		final String limbooleStr = "limboole -d -s -o " + afterLimbooleFilename + " " + beforeLimbooleFilename;
		logger.info(limbooleStr);
		final Process limboole = Runtime.getRuntime().exec(limbooleStr);
		try {
			limboole.waitFor();
		} catch (InterruptedException e) {
			throw new AssertionError();
		}
		
		if (!USE_COPROCESSOR) {
			try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(afterLimbooleFilename)))) {
				input.lines().forEach(info::acceptLine);
			}
		} else {
			final String mapFilename = "_tmp.map";
			final String whiteVarFilename = "_tmp.white.var";
			
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
	
	public DimacsConversionInfo toDimacs(Logger logger) throws IOException {
		return toDimacs(toLimbooleString(), logger);
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
	
	public abstract BooleanFormula substitute(BooleanVariable v, BooleanFormula replacement);
	
	/*
	 * Remove TRUE and FALSE.
	 */
	public abstract BooleanFormula simplify();
	
	public static BooleanFormula fromBoolean(boolean value) {
		return value ? TrueFormula.INSTANCE : FalseFormula.INSTANCE;
	}

}

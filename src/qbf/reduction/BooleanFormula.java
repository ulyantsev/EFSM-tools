package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

public abstract class BooleanFormula {
	public static Optional<Assignment> fromDimacsToken(String token, DimacsConversionInfo dimacs) {
		boolean isTrue = token.charAt(0) != '-';
		if (!isTrue) {
			token = token.substring(1);
		}
		int dimacsIndex = Integer.parseInt(token);
		Optional<Integer> limbooleIndex = dimacs.toLimbooleNumber(dimacsIndex);
		return limbooleIndex.map(index -> new Assignment(BooleanVariable.getVarByNumber(index), isTrue));
	}
	
	public static final String DIMACS_FILENAME = "_tmp.dimacs";
	
	public static Pair<List<Assignment>, Long> solveAsSat(String formula, Logger logger, String solverParams,
			int timeoutSeconds, SatSolver solver) throws IOException {
		logger.info("Final SAT formula length: " + formula.length());
		DimacsConversionInfo info = BooleanFormula.toDimacs(formula, logger, DIMACS_FILENAME);
		info.close();
		logger.info("CREATED DIMACS FILE");
		
		long time = System.currentTimeMillis();
		Map<String, Assignment> list = new LinkedHashMap<>();
		final int maxtime = Math.max(2, timeoutSeconds); // cryptominisat does not accept time=1
		String solverStr = solver.command + maxtime + " " + DIMACS_FILENAME + " " + solverParams;
		logger.info(solverStr);
		Process p = Runtime.getRuntime().exec(solverStr);
		
		try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			input.lines().filter(s -> s.startsWith("v")).forEach(certificateLine ->
				Arrays.stream(certificateLine.split(" ")).skip(1).forEach(token ->
					fromDimacsToken(token, info).ifPresent(ass -> {
						assert !list.containsKey(ass.var.name);
						list.put(ass.var.name, ass);
					})
				)
			);
		}
		
		time = System.currentTimeMillis() - time;
		return Pair.of(new ArrayList<>(list.values()), time);
	}
	
	public static class DimacsConversionInfo implements AutoCloseable {
		private final Map<Integer, Integer> limbooleNumberToDimacs = new HashMap<>();
		private final Map<Integer, Integer> dimacsNumberToLimboole = new HashMap<>();
		private String title;
		private Integer varCount;
		private PrintWriter pw;
		
		public DimacsConversionInfo(String filename) throws FileNotFoundException {
			 pw = new PrintWriter(new File(filename));
		}
		
		void acceptLine(String line) {
			if (line.startsWith("c")) {
				String[] tokens = line.split(" ");
				assert tokens.length == 3;
				int first = Integer.parseInt(tokens[1]);
				int second = Integer.parseInt(tokens[2]);
				assert !limbooleNumberToDimacs.containsKey(second);
				assert !dimacsNumberToLimboole.containsKey(first);
				limbooleNumberToDimacs.put(second, first);
				dimacsNumberToLimboole.put(first, second);
			} else if (line.startsWith("p")) {
				pw.println(line);
				title = line;
				varCount = Integer.parseInt(line.split(" ")[2]);
			} else {
				pw.println(line);
			}
		}
		
		public String title() {
			assert title != null;
			return title;
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

		@Override
		public void close() {
			pw.close();
		}
	}
	
	public abstract String toLimbooleString();
	
	public static DimacsConversionInfo toDimacs(String limbooleFormula, Logger logger, String dimacsFilename) throws IOException {
		final String beforeLimbooleFilename = "_tmp.limboole";
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
		
		final DimacsConversionInfo info = new DimacsConversionInfo(dimacsFilename);
		try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(afterLimbooleFilename)))) {
			input.lines().forEach(info::acceptLine);
		}
		info.close();

		return info;
	}
	
	public DimacsConversionInfo toDimacs(Logger logger, String dimacsFilename) throws IOException {
		return toDimacs(simplify().toLimbooleString(), logger, dimacsFilename);
	}
	
	public BooleanFormula not() {
		return new NotOperation(this);
	}
	
	public BooleanFormula and(BooleanFormula other) {
		return BinaryOperation.and(this, other);
	}
	
	public BooleanFormula or(BooleanFormula other) {
		return BinaryOperation.or(this, other);
	}
	
	public BooleanFormula implies(BooleanFormula other) {
		return BinaryOperation.implies(this, other);
	}
	
	public BooleanFormula equivalent(BooleanFormula other) {
		return BinaryOperation.equivalent(this, other);
	}
	
	//public abstract BooleanFormula substitute(BooleanVariable v, BooleanFormula replacement);
	public abstract BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement);
	
	/*
	 * Removes TRUE and FALSE.
	 */
	public abstract BooleanFormula simplify();
	
	public static BooleanFormula fromBoolean(boolean value) {
		return value ? TrueFormula.INSTANCE : FalseFormula.INSTANCE;
	}

}

package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
	private static final String COPY_DIMACS_FILENAME = "_tmp.dimacs.copy";
	
	public static class SolveAsSatResult {
		private final List<Assignment> list;
		public final long time;
		public final DimacsConversionInfo info;
		
		public List<Assignment> list() {
			return Collections.unmodifiableList(list);
		}
		
		public SolveAsSatResult(List<Assignment> list, long time, DimacsConversionInfo info) {
			this.list = list;
			this.time = time;
			this.info = info;
		}
	}
	
	public static void appendProhibitionConstraintsToDimacs(Logger logger,
			List<List<Assignment>> prohibitedFSMs, DimacsConversionInfo info) throws IOException {
		final List<String> newClauses = prohibitedFSMs.stream()
			.map(fsm -> Assignment.toDimacsString(fsm, info))
			.collect(Collectors.toList());
		try (final BufferedReader input = new BufferedReader(new FileReader(new File(DIMACS_FILENAME)))) {
			final String[] tokens = input.readLine().split(" ");
			assert tokens.length == 4;
			final int oldConstraintsNum = Integer.parseInt(tokens[3]);
			final int newConstraintNum = oldConstraintsNum + prohibitedFSMs.size();
			String line = null;
			try (final PrintWriter pw = new PrintWriter(COPY_DIMACS_FILENAME)) {
				pw.println(tokens[0] + " " + tokens[1] + " " + tokens[2] + " " + newConstraintNum);
				for (String clause : newClauses) {
					pw.println(clause);
				}
				while ((line = input.readLine()) != null) {
					pw.println(line);
				}
			}
		}
		final boolean moved = new File(COPY_DIMACS_FILENAME).renameTo(new File(DIMACS_FILENAME)); // mv
		assert moved;
		logger.info("ALTERED DIMACS FILE");
	}
	
	private static int SOLVER_SEED = 0;
	
	public static SolveAsSatResult solveDimacs(Logger logger, int timeoutSeconds, SatSolver solver,
			String solverParams, DimacsConversionInfo info) throws IOException {
		long time = System.currentTimeMillis();
		final Map<String, Assignment> list = new LinkedHashMap<>();
		if (solver == SatSolver.LINGELING) {
			solverParams += " --seed=" + SOLVER_SEED++;
			timeoutSeconds = Math.max(1, timeoutSeconds);
		} else if (solver == SatSolver.CRYPTOMINISAT) {
			solverParams += " --random=" + SOLVER_SEED++;
			timeoutSeconds = Math.max(2, timeoutSeconds); // cryptominisat does not accept time=1
		}
		final String solverStr = solver.command + timeoutSeconds + " " + solverParams
				+ " " + DIMACS_FILENAME;
		logger.info(solverStr);
		final Process p = Runtime.getRuntime().exec(solverStr);
		
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
		return new SolveAsSatResult(new ArrayList<>(list.values()), time, info);
	}
	
	public static SolveAsSatResult solveAsSat(String formula, Logger logger, String solverParams,
			int timeoutSeconds, SatSolver solver) throws IOException {
		logger.info("Final SAT formula length: " + formula.length());
		DimacsConversionInfo info = BooleanFormula.toDimacs(formula, logger, DIMACS_FILENAME);
		info.close();
		logger.info("CREATED DIMACS FILE");
		return solveDimacs(logger, timeoutSeconds, solver, solverParams, info);
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

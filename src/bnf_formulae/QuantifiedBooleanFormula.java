package bnf_formulae;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sat_solving.Assignment;
import sat_solving.QbfSolver;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import algorithms.exception.TimeLimitExceededException;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;

public class QuantifiedBooleanFormula {
	private final List<BooleanVariable> existVars;
	private final List<BooleanVariable> forallVars;
	private final BooleanFormula formulaExist; // the part which does not use forall-variables
	private final BooleanFormula formulaTheRest;

	public BooleanFormula existentialPart() {
		return formulaExist;
	}
	
	private BooleanFormula formula() {
		return formulaExist.and(formulaTheRest);
	}
	
	public QuantifiedBooleanFormula(List<BooleanVariable> existVars, List<BooleanVariable> forallVars,
			BooleanFormula formulaExist, BooleanFormula formulaTheRest) {
		this.existVars = existVars;
		this.forallVars = forallVars;
		this.formulaExist = formulaExist;
		this.formulaTheRest = formulaTheRest;
	}
	
	@Override
	public String toString() {
		List<BooleanVariable> e = existVars.stream().sorted().collect(Collectors.toList());
		List<BooleanVariable> a = forallVars.stream().sorted().collect(Collectors.toList());
		return "EXIST\n" + e + "\nFORALL\n" + a + "\n" + formula();
	}
	
	private static class QdimacsConversionInfo {
		public final String qdimacsString;
		private final DimacsConversionInfo info;
		
		public QdimacsConversionInfo(String qdimacsString, DimacsConversionInfo info) {
			this.qdimacsString = qdimacsString;
			this.info = info;
		}
	}
	
	private String otherVars(DimacsConversionInfo info) {
		List<Integer> nums = new ArrayList<>();
		for (int i = 1; i <= info.varNumber(); i++) {
			if (!info.toLimbooleNumber(i).isPresent()) {
				nums.add(i);
			}
		}
		return nums.toString().replaceAll("[,\\[\\]]", "");
	}
	
	public QdimacsConversionInfo toQdimacs(Logger logger) throws IOException {
		StringBuilder sb = new StringBuilder();
		DimacsConversionInfo info = formula().toDimacs(logger, BooleanFormula.DIMACS_FILENAME);
		
		sb.append(info.title() + "\n");
		sb.append("e " + varsToNumbers(existVars, info) + " 0\n");
		sb.append("a " + varsToNumbers(forallVars, info) + " 0\n");
		sb.append("e " + otherVars(info) + " 0\n");
		try (BufferedReader input = new BufferedReader(new FileReader(BooleanFormula.DIMACS_FILENAME))) {
			// skip title
			input.lines().skip(1).forEach(line -> sb.append(line + "\n"));
		}
		
		return new QdimacsConversionInfo(sb.toString(), info);
	}
	
	private String varsToNumbers(List<BooleanVariable> vars, DimacsConversionInfo info) {
		List<Integer> nums = new ArrayList<>();
		vars.forEach(v -> {
			Optional<Integer> dimacsNumber = info.toDimacsNumber(v.number);
			dimacsNumber.map(nums::add);
			if (!dimacsNumber.isPresent()) {
				System.out.println("Warning: unused variable " + v.name + " " + v.number);
			}
		});
		Collections.sort(nums);
		return nums.toString().replaceAll("[\\[\\],]", "");
	}

	private static final String QDIMACS_FILENAME = "_tmp.qdimacs";
	
	private SolverResult depqbfSolve(Logger logger, int timeoutSeconds,
			QdimacsConversionInfo qdimacs, String params) throws IOException {
		long time = System.currentTimeMillis();
		List<Assignment> list = new ArrayList<>();
		String depqbfStr = "depqbf --max-secs=" + timeoutSeconds + " --qdo " + QDIMACS_FILENAME + " " + params;
		logger.info(depqbfStr);
		Process depqbf = Runtime.getRuntime().exec(depqbfStr);
		try (BufferedReader input = new BufferedReader(new InputStreamReader(depqbf.getInputStream()))) {
			input.lines().filter(s -> s.startsWith("V")).forEach(line -> {
				String[] tokens = line.split(" ");
				assert tokens.length == 3 && tokens[2].equals("0");
				BooleanFormula.fromDimacsToken(tokens[1], qdimacs.info).ifPresent(list::add);
			});
		}
		time = System.currentTimeMillis() - time;
		
		if (list.isEmpty()) {
			return new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT);
		} else if (assignmentIsOk(list)) {
			return new SolverResult(list);
		} else {
			logger.severe("DEPQBF PRODUCED A BAD ASSIGNMENT, GIVING UP");
			return new SolverResult(SolverResults.UNKNOWN);
		}
	}
	
	public boolean assignmentIsOk(List<Assignment> assignments) {
		final Set<String> properVars = existVars.stream().map(v -> v.name).collect(Collectors.toCollection(TreeSet::new));
		final Set<String> actualVars = assignments.stream().map(a -> a.var.name).collect(Collectors.toCollection(TreeSet::new));
		return properVars.equals(actualVars);
	}

	public SolverResult solve(Logger logger, QbfSolver solver, String solverParams,
			int timeoutSeconds) throws IOException {
		final QdimacsConversionInfo qdimacs = toQdimacs(logger);
		logger.info("DIMACS CNF: " + qdimacs.info.title());
		
		try (PrintWriter pw = new PrintWriter(QDIMACS_FILENAME)) {
			pw.print(qdimacs.qdimacsString);
		}
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(toString());
		}
		
		switch (solver) {
		case DEPQBF:
			return depqbfSolve(logger, timeoutSeconds, qdimacs, solverParams);
		default:
			throw new AssertionError();
		}
	}
	
	/*
	 * Produce an equivalent Boolean formula as a Limboole string.
	 * The size of the formula is exponential of forallVars.size().
	 */
	public String flatten(int statesNum, int k, Logger logger,
			List<String> events, List<String> actions,
			Set<String> forbiddenYs, long finishTime, int sizeLimit, boolean withExistPart) throws FormulaSizeException, TimeLimitExceededException {
		final FormulaBuffer buffer = new FormulaBuffer(finishTime, sizeLimit);
		logger.info("Number of 'forall' variables: " + forallVars.size());
		long time = System.currentTimeMillis();
		if (withExistPart) {
			buffer.append(formulaExist.simplify());
		}
		findAllAssignmentsSigmaEps(events, statesNum, actions, k, 0, formulaTheRest,
				buffer, -1, -1, new HashMap<>(), forbiddenYs);
		
		time = System.currentTimeMillis() - time;
		logger.info("Formula generation time: " + time + " ms.");
		
		return buffer.toString();
	}

	// recursive
	/*
	 * Equivalent to constraints sigma_0_0 = 0 and A_1 and A_2 and B.
	 */
	private void findAllAssignmentsSigmaEps(List<String> events, int statesNum, List<String> actions,
			int k, int j, BooleanFormula formulaToAppend, FormulaBuffer buffer,
			int lastStateIndex, int lastPairIndex, Map<String, Integer> yAssignment, Set<String> forbiddenYs) throws FormulaSizeException, TimeLimitExceededException {
		formulaToAppend = formulaToAppend.simplify();
		if (j == k + 1) {
			assert formulaToAppend != FalseFormula.INSTANCE; // in this case the formula is obviously unsatisfiable
			if (formulaToAppend != TrueFormula.INSTANCE) {
				buffer.append(formulaToAppend);
			}
		} else {
			int iMax = j == 0 ? 1 : statesNum;
			for (int i = 0; i < iMax; i++) {
				final Map<BooleanVariable, BooleanFormula> replacement = new HashMap<>();
				
				// deal with ys
				String yKey = null;
				boolean wasNull = false;
				if (j > 0) {
					final int i1 = lastStateIndex;
					final String e = events.get(lastPairIndex);
					final int i2 = i;
					if (forbiddenYs.contains("y_" + i1 + "_" + i2 + "_" + lastPairIndex)) {
						// this y is forbidden due to BFS constraints
						continue;
					}
					yKey = i1 + "_" + e;
					final Integer curYValue = yAssignment.get(yKey);
					wasNull = curYValue == null;
					if (wasNull) {
						yAssignment.put(yKey, i2); // assign
					} else if (curYValue != i2) {
						continue; // already assigned to a different variable
					}
				}
				
				for (int iOther = 0; iOther < statesNum; iOther++) {
					replacement.put(BooleanVariable.byName("sigma", iOther, j).get(),
							FalseFormula.INSTANCE);
				}
				replacement.put(BooleanVariable.byName("sigma", i, j).get(), TrueFormula.INSTANCE);
				for (int pIndex = 0; pIndex < events.size(); pIndex++) {
					String e = events.get(pIndex);
					for (String eOther : events) {
						replacement.put(BooleanVariable.byName("eps", eOther, j).get(),
									FalseFormula.INSTANCE);
					}
					replacement.put(BooleanVariable.byName("eps", e, j).get(),
							TrueFormula.INSTANCE);
					
					// deal with zetas
					for (String action : actions) {
						replacement.put(BooleanVariable.byName("zeta", action, j).get(),
								BooleanVariable.byName("z", i, action, e).get());
					}
					
					// recursive call
					findAllAssignmentsSigmaEps(events, statesNum, actions, k, j + 1,
							formulaToAppend.multipleSubstitute(replacement), buffer,
							i, pIndex, yAssignment, forbiddenYs);
				}		
				
				if (j > 0 && wasNull) {
					yAssignment.remove(yKey);
				}
			}
		}
	}

	static class FormulaBuffer {
		private final StringBuilder formula = new StringBuilder();
		private final int sizeLimit;
		private final long timeToFinish;
		
		public FormulaBuffer(long timeToFinish, int sizeLimit) {
			this.timeToFinish = timeToFinish;
			this.sizeLimit = sizeLimit;
		}
		
		public void append(BooleanFormula f) throws FormulaSizeException, TimeLimitExceededException {
			if (System.currentTimeMillis() > timeToFinish) {
				throw new TimeLimitExceededException();
			}
			if (formula.length() > 0) {
				formula.append("&");
			}
			formula.append(f.toLimbooleString());
			if (formula.length() > sizeLimit) {
				throw new FormulaSizeException();
			}
		}
		
		@Override
		public String toString() {
			return formula.toString();
		}
	}
	
	public static class FormulaSizeException extends Exception {
		@Override
		public Throwable fillInStackTrace() {
			return this;
		}
	}
}

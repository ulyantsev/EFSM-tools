package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import bool.MyBooleanExpression;
import qbf.reduction.BooleanFormula.DimacsConversionInfo;
import qbf.reduction.SolverResult.SolverResults;
import structures.ScenariosTree;

public class QuantifiedBooleanFormula {
	private final List<BooleanVariable> existVars;
	private final List<BooleanVariable> forallVars;
	private final BooleanFormula formulaExist; // the part which does not use forall-variables
	private final BooleanFormula formulaTheRest;

	private BooleanFormula formula() {
		return formulaExist.and(formulaTheRest);
	}
	
	public QuantifiedBooleanFormula(List<BooleanVariable> existVars, List<BooleanVariable> forallVars, BooleanFormula formulaExist, BooleanFormula formulaTheRest) {
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
		for (int i = 1; i <= info.varCount(); i++) {
			if (!info.toLimbooleNumber(i).isPresent()) {
				nums.add(i);
			}
		}
		return nums.toString().replaceAll("[,\\[\\]]", "");
	}
	
	public QdimacsConversionInfo toQdimacs(Logger logger, boolean useCoprocessor) throws IOException {
		StringBuilder sb = new StringBuilder();
		DimacsConversionInfo info = formula().toDimacs(logger, useCoprocessor);
		
		sb.append(info.title() + "\n");
		sb.append("e " + varsToNumbers(existVars, info) + " 0\n");
		sb.append("a " + varsToNumbers(forallVars, info) + " 0\n");
		sb.append("e " + otherVars(info) + " 0\n");
		sb.append(info.output());
		
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
	
	private SolverResult depqbfSolve(Logger logger, int timeoutSeconds, QdimacsConversionInfo qdimacs, String params) throws IOException {
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
			return new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time);
		} else if (assignmentIsOk(list)) {
			return new SolverResult(list, (int) time);
		} else {
			logger.severe("DEPQBF PRODUCED A BAD ASSIGNMENT, GIVING UP");
			return new SolverResult(SolverResults.UNKNOWN, (int) time);
		}
	}
	
	private boolean assignmentIsOk(List<Assignment> assignments) {
		Set<String> properVars = existVars.stream().map(v -> v.name).collect(Collectors.toCollection(TreeSet::new));
		Set<String> actualVars = assignments.stream().map(a -> a.var.name).collect(Collectors.toCollection(TreeSet::new));
		return properVars.equals(actualVars);
	}
	
	private SolverResult skizzoSolve(Logger logger, int timeoutSeconds, QdimacsConversionInfo qdimacs, String params) throws IOException {
		long time = System.currentTimeMillis();
		List<Assignment> list = new ArrayList<>();
		File skizzoLog = new File(QDIMACS_FILENAME + ".sKizzo.log");
		File certificate = new File(QDIMACS_FILENAME + ".qdc");
		
		String skizzoStr = "sKizzo -log text -v 0 " + params + " "
			+ QDIMACS_FILENAME + " " + timeoutSeconds;
		logger.info(skizzoStr);
		Process skizzo = Runtime.getRuntime().exec(skizzoStr);
		int code;
		try {
			code = skizzo.waitFor();
		} catch (InterruptedException e) {
			throw new AssertionError();
		}
		time = System.currentTimeMillis() - time;

		switch (code) {
		case 10:
			List<String> vars = new ArrayList<>();
			for (BooleanVariable v : existVars) {
				vars.add(String.valueOf(qdimacs.info.toDimacsNumber(v.number).get()));
			}

			// find the partial certificate
			String ozziksStr = "ozziKs -var " + String.join(",", vars) + " -dump qdc " + skizzoLog.getName();
			logger.info(ozziksStr);
			Process ozziks = Runtime.getRuntime().exec(ozziksStr);
			try {
				ozziks.waitFor();
			} catch (InterruptedException e) {
				throw new AssertionError();
			}
			if (!certificate.exists()) {
				logger.warning("NO CERTIFICATE PRODUCED BY OZZIKS, TRYING DEPQBF");
				return depqbfSolve(logger, timeoutSeconds, qdimacs, "");
			}
			
			try (BufferedReader input = new BufferedReader(new FileReader(certificate))) {
				input.lines().filter(s -> s.startsWith("v")).forEach(certificateLine ->
					Arrays.stream(certificateLine.split(" ")).skip(1).forEach(token ->
						BooleanFormula.fromDimacsToken(token, qdimacs.info).ifPresent(list::add)
					)
				);
			}

			if (!assignmentIsOk(list)) {
				logger.warning("NOT ALL VARS ARE PRESENT IN CERTIFICATE, TRYING DEPQBF");
				return depqbfSolve(logger, timeoutSeconds, qdimacs, "");
			}
			
			return new SolverResult(list, (int) time);
		case 20:
			return new SolverResult(SolverResults.UNSAT, (int) time);
		case 30:
			return new SolverResult(SolverResults.UNKNOWN, (int) time);
		case 250:
			logger.warning("MEMOUT");
			return new SolverResult(SolverResults.UNKNOWN, (int) time);
		default:
			logger.severe("Something went wrong during sKizzo execution, exit code = " + code);
			return new SolverResult(SolverResults.UNKNOWN, (int) time);
		}
	}
	
	public SolverResult solve(Logger logger, Solvers solver, String solverParams, int timeoutSeconds, boolean useCoprocessor) throws IOException {
		QdimacsConversionInfo qdimacs = toQdimacs(logger, useCoprocessor);
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
		case SKIZZO:
			return skizzoSolve(logger, timeoutSeconds, qdimacs, solverParams);
		default:
			throw new AssertionError();
		}
	}
	
	// recursive
	/*
	 * Enumerates 2^exponentialAssignmentVars.size() variable values.
	 */
	private static void findAllAssignmentsOther(int pos, BooleanVariable[] otherAssignmentVars,
			BooleanFormula formulaToAppend, List<String> listToAppend) {
		if (pos == otherAssignmentVars.length) {
			listToAppend.add(formulaToAppend.toLimbooleString().replace(" ", ""));
		} else {
			BooleanVariable currentVar = otherAssignmentVars[pos];
			findAllAssignmentsOther(pos + 1, otherAssignmentVars,
					formulaToAppend.substitute(currentVar, FalseFormula.INSTANCE).simplify(), listToAppend);
			findAllAssignmentsOther(pos + 1, otherAssignmentVars,
					formulaToAppend.substitute(currentVar, TrueFormula.INSTANCE).simplify(), listToAppend);
		}
	}
	
	// recursive
	/*
	 * Equivalent to constrains sigma_0_0 = 0 and A_1 and A_2 and B.
	 */
	private void findAllAssignmentsSigmaEps(ScenariosTree tree, int statesNum, int k, int j, BooleanFormula formulaToAppend,
			List<String> listToAppend) {
		if (j == k + 1) {
			List<BooleanVariable> otherAssignmentVars = forallVars.stream()
					.filter(v -> !v.name.startsWith("sigma") && !v.name.startsWith("eps"))
					.collect(Collectors.toList());
			findAllAssignmentsOther(0, otherAssignmentVars.toArray(new BooleanVariable[otherAssignmentVars.size()]),
					formulaToAppend, listToAppend);
		} else {
			int iMax = (k == 0 ? 0 : statesNum);
			for (int i = 0; i < iMax; i++) {
				BooleanFormula curFormula1 = formulaToAppend.substitute(BooleanVariable.byName("sigma", i, j).get(),
						TrueFormula.INSTANCE);
				for (int iOther = 0; iOther < statesNum; iOther++) {
					curFormula1 = curFormula1.substitute(BooleanVariable.byName("sigma", iOther, j).get(),
							FalseFormula.INSTANCE);
				}
				for (String event : tree.getEvents()) {
					for (MyBooleanExpression f : tree.getPairsEventExpression().get(event)) {
						BooleanFormula curFormula2 = curFormula1.substitute(BooleanVariable.byName("eps", event, f, j).get(),
								TrueFormula.INSTANCE);
						for (String eventOther : tree.getEvents()) {
							for (MyBooleanExpression fOther : tree.getPairsEventExpression().get(eventOther)) {
								curFormula2 = curFormula2.substitute(BooleanVariable.byName("eps", eventOther, fOther, j).get(),
										FalseFormula.INSTANCE);
							}
						}
						// recursive call
						findAllAssignmentsSigmaEps(tree, statesNum, k, j + 1, curFormula2.simplify(), listToAppend);
					}
				}
			}
		}
	}
	
	/*
	 * Produce an equivalent Boolean formula as a Limboole string.
	 * The size of the formula is exponential of forallVars.size().
	 */
	private String flatten(ScenariosTree tree, int statesNum, int k, Logger logger) {
		List<String> mainList = new ArrayList<>();
		logger.info("Number of 'forall' variables: " + forallVars.size());
		logger.info("List of 'forall' variables: " + forallVars);
		logger.info("Initial 'inner' SAT formula length: " + formulaTheRest.toLimbooleString().length());
		
		long time = System.currentTimeMillis();
		
		findAllAssignmentsSigmaEps(tree, statesNum, k, 0, formulaTheRest, mainList);
		
		time = System.currentTimeMillis() - time;
		logger.info("Formula generation time: " + time + " ms.");
		
		return formulaExist.toLimbooleString().replace(" ", "")
				+ "&" + String.join("&", mainList);
	}

	public SolverResult solveAsSat(ScenariosTree tree, int statesNum, int k, Logger logger, String solverParams, int timeoutSeconds) throws IOException {
		String flatFormula = flatten(tree, statesNum, k, logger);
		Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(flatFormula, logger, solverParams, timeoutSeconds);
		List<Assignment> list = solution.getLeft();
		long time = solution.getRight();
		
		if (list.isEmpty()) {
			return new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time);
		} else if (assignmentIsOk(list)) {
			return new SolverResult(list, (int) time);
		} else {
			assert false;
			return null;
		}
	}
}

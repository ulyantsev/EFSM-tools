package qbf.reduction;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import qbf.reduction.BooleanFormula.DimacsConversionInfo;
import qbf.reduction.SolverResult.SolverResults;

public class QuantifiedBooleanFormula {
	private final List<BooleanVariable> existVars;
	private final List<BooleanVariable> forallVars;
	private final BooleanFormula formula;

	public QuantifiedBooleanFormula(List<BooleanVariable> existVars, List<BooleanVariable> forallVars, BooleanFormula formula) {
		this.existVars = existVars;
		this.forallVars = forallVars;
		this.formula = formula;
	}
	
	@Override
	public String toString() {
		List<BooleanVariable> e = existVars.stream().sorted().collect(Collectors.toList());
		List<BooleanVariable> a = forallVars.stream().sorted().collect(Collectors.toList());
		return "EXIST\n" + e + "\nFORALL\n" + a + "\n" + formula;
	}
	
	private static class QdimacsConversionInfo {
		public final String qdimacsString;
		private final DimacsConversionInfo info;
		
		public QdimacsConversionInfo(String qdimacsString, DimacsConversionInfo info) {
			this.qdimacsString = qdimacsString;
			this.info = info;
		}
		
		public Integer toLimbooleNumber(int num) {
			return info.toLimbooleNumber(num);
		}
	}
	
	private String otherVars(DimacsConversionInfo info) {
		List<Integer> nums = new ArrayList<>();
		for (int i = 1; i <= info.varCount(); i++) {
			if (info.toLimbooleNumber(i) == null) {
				nums.add(i);
			}
		}
		return nums.toString().replaceAll("[,\\[\\]]", "");
	}
	
	public QdimacsConversionInfo toQdimacs(Logger logger) throws IOException {
		StringBuilder sb = new StringBuilder();
		DimacsConversionInfo info = formula.toDimacs(logger);
		
		sb.append(info.title() + "\n");
		sb.append("e " + varsToNumbers(existVars, info) + " 0\n");
		sb.append("a " + varsToNumbers(forallVars, info) + " 0\n");
		sb.append("e " + otherVars(info) + " 0\n");
		sb.append(info.output());
		
		return new QdimacsConversionInfo(sb.toString(), info);
	}
	
	private String varsToNumbers(List<BooleanVariable> vars, DimacsConversionInfo info) {
		List<Integer> nums = new ArrayList<>();
		for (BooleanVariable v : vars) {
			Integer dimacsNumber = info.toDimacsNumber(v.number);
			if (dimacsNumber != null) {
				nums.add(dimacsNumber);
			} else {
				System.out.println("Warning: unused variable " + v.name + " " + v.number);
			}
		}
		Collections.sort(nums);
		return nums.toString().replaceAll("[\\[\\],]", "");
	}

	private Assignment fromDimacsToken(String token, QdimacsConversionInfo qdimacs) {
		boolean isTrue = token.charAt(0) != '-';
		if (!isTrue) {
			token = token.substring(1);
		}
		int dimacsIndex = Integer.parseInt(token);
		Integer limbooleIndex = qdimacs.toLimbooleNumber(dimacsIndex);
		return limbooleIndex == null ? null : new Assignment(BooleanVariable.getVarByNumber(limbooleIndex), isTrue);
	}
	
	private static final String qdimacsFilename = "_tmp.qdimacs";
	
	private SolverResult depqbfSolve(Logger logger, int timeoutSeconds, QdimacsConversionInfo qdimacs) throws IOException {
		long time = System.currentTimeMillis();
		List<Assignment> list = new ArrayList<>();
		String depqbfStr = "depqbf --max-secs=" + timeoutSeconds + " --qdo " + qdimacsFilename;
		logger.info(depqbfStr);
		Process p = Runtime.getRuntime().exec(depqbfStr);
		try (Scanner input = new Scanner(p.getInputStream())) {
			while (input.hasNextLine()) {
				String line = input.nextLine();
				if (line.startsWith("V")) {
					String[] tokens = line.split(" ");
					assert tokens.length == 3 && tokens[2].equals("0");
					Assignment ass = fromDimacsToken(tokens[1], qdimacs);
					if (ass != null) {
						list.add(ass);
					}
				}
			}
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
	
	private SolverResult skizzoSolve(Logger logger, int timeoutSeconds, QdimacsConversionInfo qdimacs) throws IOException {
		long time = System.currentTimeMillis();
		List<Assignment> list = new ArrayList<>();
		// delete previous files
		File skizzoLog = new File(qdimacsFilename + ".sKizzo.log");
		if (skizzoLog.exists()) {
			skizzoLog.delete();
		}
		File certificate = new File(qdimacsFilename + ".qdc");
		if (certificate.exists()) {
			certificate.delete();
		}
		
		String skizzoStr = "sKizzo -log text -v 0 " + qdimacsFilename + " " + timeoutSeconds;
		logger.info(skizzoStr);
		Process p = Runtime.getRuntime().exec(skizzoStr);
		int code;
		try {
			code = p.waitFor();
		} catch (InterruptedException e) {
			throw new AssertionError();
		}
		time = System.currentTimeMillis() - time;

		switch (code) {
		case 10:
			List<String> vars = new ArrayList<>();
			for (BooleanVariable v : existVars) {
				vars.add(String.valueOf(qdimacs.info.toDimacsNumber(v.number)));
			}

			// find the partial certificate
			String ozziksStr = "ozziKs -var " + String.join(",", vars) + " -dump qdc " + skizzoLog.getName();
			logger.info(ozziksStr);
			p = Runtime.getRuntime().exec(ozziksStr);
			try {
				p.waitFor();
			} catch (InterruptedException e) {
				throw new AssertionError();
			}
			if (!certificate.exists()) {
				logger.warning("NO CERTIFICATE PRODUCED BY OZZIKS, TRYING DEPQBF");
				return depqbfSolve(logger, timeoutSeconds, qdimacs);
			}
			
			try (Scanner sc = new Scanner(certificate)) {
				while (sc.hasNextLine()) {
					String certificateLine = sc.nextLine();
					if (certificateLine.startsWith("v")) {
						String[] tokens = certificateLine.split(" ");
						for (int i = 1; i < tokens.length; i++) {
							Assignment ass = fromDimacsToken(tokens[i], qdimacs);
							if (ass != null) {
								list.add(ass);
							}
						}
					}
				}
			}

			if (!assignmentIsOk(list)) {
				logger.warning("NOT ALL VARS ARE PRESENT IN CERTIFICATE, TRYING DEPQBF");
				return depqbfSolve(logger, timeoutSeconds, qdimacs);
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
	
	public SolverResult solve(Logger logger, Solvers solver, int timeoutSeconds) throws IOException {
		QdimacsConversionInfo qdimacs = toQdimacs(logger);
		logger.info("DIMACS CNF: " + qdimacs.info.title());
		
		try (PrintWriter pw = new PrintWriter(qdimacsFilename)) {
			pw.print(qdimacs.qdimacsString);
		}
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(toString());
		}
		
		switch (solver) {
		case DEPQBF:
			return depqbfSolve(logger, timeoutSeconds, qdimacs);
		case SKIZZO:
			return skizzoSolve(logger, timeoutSeconds, qdimacs);
		default:
			throw new AssertionError();
		}
	}
}

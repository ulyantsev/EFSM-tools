package sat_solving;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import bnf_formulae.BinaryOperation;
import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;
import bnf_formulae.BooleanFormula.SolveAsSatResult;
import bnf_formulae.FormulaList;

public class IncrementalInterface {
	private final Logger logger;
	private final SatSolver satSolver;
	private final String solverParams;
	private DimacsConversionInfo info;
	private final Set<String> existingConstraints = new HashSet<>();
	
	private final Process solver;
	private final PrintWriter pw;
	private final Scanner sc;
	
	public IncrementalInterface(BooleanFormula positiveConstraints, String actionspec, Logger logger, SatSolver satSolver,
			String solverParams) throws IOException {
		this.logger = logger;
		this.satSolver = satSolver;
		this.solverParams = solverParams;
		
		info = positiveConstraints.toDimacs_plant(logger, BooleanFormula.DIMACS_FILENAME, actionspec, null, null);
		info.close();

		// FIXME time limits
		solver = Runtime.getRuntime().exec("incremental-cryptominisat " + info.varNumber() + " 1");
		pw = new PrintWriter(solver.getOutputStream());
		sc = new Scanner(solver.getInputStream());
		
		try (BufferedReader input = new BufferedReader(new FileReader(BooleanFormula.DIMACS_FILENAME))) {
			input.lines().skip(1).forEach(pw::println);
		}
	}
	
	public void halt() {
		pw.println("halt");
		pw.flush();
		pw.close();
		sc.close();
	}
	
	public SolveAsSatResult solve(List<BooleanFormula> newConstraints, int timeLeftForSolver) throws IOException {
		final List<BooleanFormula> diff = new ArrayList<>();
		for (BooleanFormula f : newConstraints) {
			final String str = f.toString();
			if (!existingConstraints.contains(str)) {
				existingConstraints.add(str);
				diff.add(f);
			}
		}
		System.out.println(diff.size() + " new constraints");
		
		info = BinaryOperation.and(diff.toArray(new BooleanFormula[diff.size()]))
				.toDimacs_plant(logger, null, null, info, pw);
		long time = System.currentTimeMillis();
		pw.println("solve");
		pw.flush();
		final String verdict = sc.nextLine();
		time = System.currentTimeMillis() - time;
		final List<Assignment> list = new ArrayList<>();
		logger.info(verdict);
		if (verdict.equals("SAT")) {
			final String assignment = sc.nextLine();
			Arrays.stream(assignment.split(" ")).skip(1).forEach(token ->
				BooleanFormula.fromDimacsToken(token, info).ifPresent(ass -> {
					list.add(ass);
				})
			);
			return new SolveAsSatResult(list, time, info);
		} else if (verdict.equals("UNSAT")) {
			halt();
		} else if (verdict.equals("UNKNOWN")) {
			halt();
		} else {
			throw new AssertionError();
		}
		return new SolveAsSatResult(list, time, info);
	}
}

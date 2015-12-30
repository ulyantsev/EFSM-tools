package sat_solving;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import bnf_formulae.BinaryOperation;
import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;
import bnf_formulae.BooleanFormula.SolveAsSatResult;

public class IncrementalInterface {
	private final DimacsConversionInfo info;
	private final Process solver;
	private final PrintWriter pw;
	private final Scanner sc;
	
	public IncrementalInterface(BooleanFormula positiveConstraints, String actionspec, Logger logger) throws IOException {	
		info = positiveConstraints.toDimacs_plant(logger, BooleanFormula.DIMACS_FILENAME, actionspec);
		info.close();

		// FIXME time limits
		solver = Runtime.getRuntime().exec("incremental-cryptominisat " + info.varNumber() + " 1");
		pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(solver.getOutputStream())));
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
		long tDim = System.currentTimeMillis();
		BinaryOperation.and(newConstraints.toArray(new BooleanFormula[newConstraints.size()]))
				.toDimacs_plant(info, pw);
		System.out.println("@ToDimacs: " + (System.currentTimeMillis() - tDim));
		
		long time = System.currentTimeMillis();
		pw.println("solve");
		pw.flush();
		final String verdict = sc.nextLine();
		time = System.currentTimeMillis() - time;
		final List<Assignment> list = new ArrayList<>();
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

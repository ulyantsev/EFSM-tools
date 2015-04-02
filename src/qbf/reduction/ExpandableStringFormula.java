package qbf.reduction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import qbf.reduction.BooleanFormula.DimacsConversionInfo;
import qbf.reduction.BooleanFormula.SolveAsSatResult;

public class ExpandableStringFormula implements AutoCloseable {
	private final String initialFormula;
	private final Logger logger;
	private final SatSolver satSolver;
	private final String solverParams;
	private boolean closed = false;
	
	private DimacsConversionInfo info;
	
	private Process solverProcess;
	private OutputStream solverOutput;
	private InputStream solverInput;
	private PrintWriter solverPrintWriter;
	private Scanner solverScanner;
	
	public ExpandableStringFormula(String initialFormula, Logger logger, SatSolver satSolver,
			String solverParams) {
		this.initialFormula = initialFormula;
		this.logger = logger;
		this.satSolver = satSolver;
		this.solverParams = solverParams;
	}
	
	/*
	 * Should be called after 'solve' was called at least once.
	 */
	public void addProhibitionConstraint(List<Assignment> constraints)
			throws IOException {
		if (satSolver == SatSolver.ITERATIVE_CRYPTOMINISAT) {
			String newConstraint = Assignment.toDimacsString(constraints, info);
			solverPrintWriter.println(Assignment.toDimacsString(constraints, info));
		} else {
			assert info != null;
			BooleanFormula.appendProhibitionConstraintsToDimacs(logger,
					Collections.singletonList(constraints), info);
		}
	}
	
	private SolveAsSatResult iterativeSolve() {
		long time = System.currentTimeMillis();
		solverPrintWriter.println("solve");
		solverPrintWriter.flush();
		String result = solverScanner.nextLine();
		time = System.currentTimeMillis() - time;
		if (result.equals("SAT")) {
			String vars = solverScanner.nextLine();
			List<Assignment> list = new ArrayList<>();
			Arrays.stream(vars.split(" ")).skip(1).forEach(token ->
				BooleanFormula.fromDimacsToken(token, info).ifPresent(ass -> {
					list.add(ass);
				})
			);
			return new SolveAsSatResult(list, time, info);
		} else {
			assert result.equals("UNSAT") || result.equals("UNKNOWN");
			return new SolveAsSatResult(Collections.emptyList(), time, info);
		}
	}
	
	public SolveAsSatResult solve(int timeLeftForSolver) throws IOException {
		if (satSolver == SatSolver.ITERATIVE_CRYPTOMINISAT) {
			if (info == null) {
				info = BooleanFormula.toDimacs(initialFormula, logger, BooleanFormula.DIMACS_FILENAME);
				info.close();
				logger.info("CREATED DIMACS FILE");
				int vars = info.varCount();
				solverProcess = Runtime.getRuntime().exec(satSolver.command + " " + vars + " " + timeLeftForSolver);
				solverOutput = solverProcess.getOutputStream();
				solverInput = solverProcess.getInputStream();
				solverPrintWriter = new PrintWriter(solverOutput);
				solverScanner = new Scanner(solverInput);
				
				// add constraints
				try (BufferedReader input = new BufferedReader(new FileReader(
						new File(BooleanFormula.DIMACS_FILENAME)))) {
					input.lines()
						.filter(l -> !l.startsWith("p") && !l.startsWith("c") && l.endsWith("0"))
						.forEach(solverPrintWriter::println);
				}
			}
			return iterativeSolve();
		} else {
			if (info == null) {
				final SolveAsSatResult solution = BooleanFormula.solveAsSat(initialFormula,
						logger, solverParams, timeLeftForSolver, satSolver);
				info = solution.info;
				return solution;
			}
			return BooleanFormula.solveDimacs(logger, timeLeftForSolver, satSolver,
					solverParams, info);
		}
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		if (satSolver == SatSolver.ITERATIVE_CRYPTOMINISAT) {
			solverPrintWriter.println("halt");
			solverPrintWriter.close();
			solverScanner.close();
			try {
				solverProcess.waitFor();
			} catch (InterruptedException e) {
				throw new AssertionError();
			}
		}
		closed = true;
	}
}

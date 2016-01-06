package sat_solving;

/**
 * (c) Igor Buzhinsky
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;
import bnf_formulae.BooleanFormula.SolveAsSatResult;
import bnf_formulae.BooleanVariable;

public class IncrementalInterface {
	private final DimacsConversionInfo info;
	private final Process solver;
	private final DataOutputStream writer;
	private final DataInputStream reader;
	
	public IncrementalInterface(List<int[]> positiveConstraints, String actionspec, Logger logger) throws IOException {	
		info = BooleanFormula.actionSpecToDimacs(logger, BooleanFormula.DIMACS_FILENAME, actionspec);
		info.close();

		// FIXME time limits
		solver = Runtime.getRuntime().exec("incremental-cryptominisat-binary " + info.varNumber() + " 0");
		writer = new DataOutputStream(new BufferedOutputStream(solver.getOutputStream()));
		reader = new DataInputStream(solver.getInputStream());
		
		try (BufferedReader input = new BufferedReader(new FileReader(BooleanFormula.DIMACS_FILENAME))) {
			input.readLine();
			String line;
			while ((line = input.readLine()) != null) {
				final String[] tokens = line.split(" ");
				writer.writeInt(0);
				for (String token : tokens) {
					writer.writeInt(Integer.parseInt(token));
				}
			}
		}
		
		BooleanFormula.appendConstraints(positiveConstraints, info, writer);
	}
	
	public void halt() throws IOException {
		writer.writeInt(3);
		writer.flush();
		writer.close();
		reader.close();
	}
	
	public SolveAsSatResult solve(List<int[]> newConstraints, int timeLeftForSolver) throws IOException {
		//long tDim = System.currentTimeMillis();
		BooleanFormula.appendConstraints(newConstraints, info, writer);
		//System.out.println("@ToDimacs: " + (System.currentTimeMillis() - tDim));
		
		long time = System.currentTimeMillis();
		writer.writeInt(2);
		writer.flush();
		final int verdict = reader.readInt();
		time = System.currentTimeMillis() - time;
		final List<Assignment> list = new ArrayList<>();
		if (verdict == 0 /* SAT */) {
			while (true) {
				final int value = reader.readInt();
				if (value == 0) {
					break;
				}
				info.toLimbooleNumber(Math.abs(value)).ifPresent(limbooleNum -> {
					final BooleanVariable var = BooleanVariable.getVarByNumber(limbooleNum);
					list.add(new Assignment(var, value > 0));
				});
			}
			return new SolveAsSatResult(list, time, info);
		} else if (verdict == 1 /* UNSAT */) {
			halt();
		} else if (verdict == 2 /* UNKNOWN */) {
			halt();
		} else {
			throw new AssertionError();
		}
		return new SolveAsSatResult(list, time, info);
	}
}

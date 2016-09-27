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

import sat_solving.SolverResult.SolverResults;
import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;
import bnf_formulae.BooleanVariable;

public class IncrementalInterface implements SolverInterface {
    private final DimacsConversionInfo info;
    private final Process solverProcess;
    private final DataOutputStream writer;
    private final DataInputStream reader;
    
    public IncrementalInterface(List<int[]> positiveConstraints, String actionspec, Logger logger, SatSolver solver)
            throws IOException {
        if (!solver.isIncremental) {
            throw new AssertionError("An incremental solver was expected!");
        }
        info = BooleanFormula.actionSpecToDimacs(logger, BooleanFormula.DIMACS_FILENAME, actionspec);
        info.close();

        solverProcess = Runtime.getRuntime().exec(solver.command + info.varNumber());
        writer = new DataOutputStream(new BufferedOutputStream(solverProcess.getOutputStream()));
        reader = new DataInputStream(solverProcess.getInputStream());
        
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

    @Override
    public void halt() throws IOException {
        writer.writeInt(3);
        writer.flush();
        writer.close();
        reader.close();
    }

    @Override
    public SolverResult solve(List<int[]> newConstraints, int timeLeftForSolver) throws IOException {
        BooleanFormula.appendConstraints(newConstraints, info, writer);
        writer.writeInt(2); // solve
        writer.writeInt(timeLeftForSolver); // with a time limit
        writer.flush();
        final int verdict = reader.readInt();
        switch (verdict) {
        case 0:
            final List<Assignment> list = new ArrayList<>();
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
            return new SolverResult(list);
        case 1:
            halt();
            return new SolverResult(SolverResults.UNSAT);
        case 2:
            halt();
            return new SolverResult(SolverResults.UNKNOWN);
        default:
            throw new AssertionError();
        }
    }
}

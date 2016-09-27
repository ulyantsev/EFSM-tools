package sat_solving;

/**
 * (c) Igor Buzhinsky
 */

import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;
import bnf_formulae.BooleanVariable;
import sat_solving.SolverResult.SolverResults;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RestartInterface implements SolverInterface {
    private final DimacsConversionInfo info;
    private final Logger logger;
    private final SatSolver solver;

    public RestartInterface(List<int[]> positiveConstraints, String actionspec, Logger logger, SatSolver solver)
            throws IOException {
        if (solver.isIncremental) {
            throw new AssertionError("A non-incremental solver was expected!");
        }
        this.logger = logger;
        this.solver = solver;
        info = BooleanFormula.actionSpecToDimacs(logger, BooleanFormula.DIMACS_FILENAME, actionspec);
        info.close();
        appendConstraints(positiveConstraints);
    }

    private void appendConstraints(List<int[]> constraints) throws IOException {
        BooleanFormula.transformConstraints(constraints, info);
        final List<String> strClauses = constraints.stream()
                .map(arr -> Arrays.toString(arr).replace(",", "").replace("[", "").replace("]", "") + " 0")
                .collect(Collectors.toList());
        BooleanFormula.appendConstraintsToDimacs(logger, strClauses, info);
    }

    @Override
    public void halt() {
    }

    @Override
    public SolverResult solve(List<int[]> newConstraints, int timeLeftForSolver) throws IOException {
        appendConstraints(newConstraints);
        return BooleanFormula.solveDimacs(logger, timeLeftForSolver, solver, info).toSolverResult(timeLeftForSolver);
    }
}

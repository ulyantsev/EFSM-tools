package qbf.reduction;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import qbf.reduction.BooleanFormula.DimacsConversionInfo;
import qbf.reduction.BooleanFormula.SolveAsSatResult;

public class ExpandableStringFormula {
	private final String initialFormula;
	private final Logger logger;
	private final SatSolver satSolver;
	private final String solverParams;
	private DimacsConversionInfo info;
	
	public DimacsConversionInfo info() {
		return info;
	}
	
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
	public void addConstraints(List<String> constraints) throws IOException {
		assert info != null;
		BooleanFormula.appendConstraintsToDimacs(logger, constraints, info);
	}
	
	public SolveAsSatResult solve(int timeLeftForSolver) throws IOException {
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

package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import algorithms.FormulaBuilder.EventExpressionPair;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.SolverResult;
import qbf.reduction.Verifier;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Solvers;
import structures.Automaton;
import structures.ScenariosTree;

public class QbfAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int colorSize, String ltlFilePath,
			int timeoutSeconds, Solvers solver, String solverParams, boolean extractSubterms,
			boolean complete, boolean useSat, boolean bfsConstraints,
			List<EventExpressionPair> efPairs, List<String> actions) throws IOException {
		
		final Verifier verifier = new Verifier(colorSize, logger, ltlFilePath, EventExpressionPair.getEvents(efPairs), actions);
		for (int k = 0; ; k++) {
			logger.info("TRYING k = " + k);
			deleteTrash();
			QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
					formulae, colorSize, k, extractSubterms, complete, bfsConstraints, efPairs, actions).getFormula(useSat);
				
			SolverResult ass = useSat
					? qbf.solveAsSat(tree, colorSize, k, logger, solverParams, timeoutSeconds, efPairs, bfsConstraints)
					: qbf.solve(logger, solver, solverParams, timeoutSeconds);

			logger.info(ass.toString().split("\n")[0]);

			if (ass.type() == SolverResults.SAT) {
				final Automaton a = constructAutomatonFromAssignment(logger, ass, tree, colorSize, true).getLeft();
				if (verifier.verify(a)) {
					return Optional.of(a);
				} else {
					continue;
				}
			} else {
				return Optional.empty();
			}
		}
	}
}

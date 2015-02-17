package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BooleanFormula;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.QuantifiedBooleanFormula.FormulaSizeException;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Solvers;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;
import algorithms.FormulaBuilder.EventExpressionPair;

public class QbfAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {	
	public final static String PRECOMPUTED_DIR_NAME = "qbf/bfs-prohibited-ys";
	
	public static Set<String> getForbiddenYs(int states, int events) throws FileNotFoundException {
		Set<String> ys = new TreeSet<>();
		try (Scanner sc = new Scanner(new File(PRECOMPUTED_DIR_NAME + "/" + states + "_" + events))) {
			while (sc.hasNext()) {
				ys.add(sc.next());
			}
		}
		return ys;
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int colorSize, String ltlFilePath,
			int timeoutSeconds, Solvers solver, String solverParams, boolean extractSubterms,
			boolean complete, boolean useSat, boolean bfsConstraints, boolean inlineZVars,
			List<EventExpressionPair> efPairs, List<String> actions) throws IOException {
		
		final Verifier verifier = new Verifier(colorSize, logger, ltlFilePath,
				EventExpressionPair.getEvents(efPairs), actions);
		final long finishTime = System.currentTimeMillis() + timeoutSeconds * 1000;
		
		if (useSat) {
			final Set<String> forbiddenYs = getForbiddenYs(colorSize, efPairs.size());
			logger.info("FORBIDDEN YS: " + forbiddenYs);
			for (int k = 0; ; k++) {
				if (System.currentTimeMillis() > finishTime) {
					logger.info("TIME LIMIT EXCEEDED");
					return Optional.empty();
				}
				logger.info("TRYING k = " + k);
				deleteTrash();
				QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
						formulae, colorSize, k, extractSubterms, complete, bfsConstraints,
						inlineZVars, efPairs, actions).getFormula(false);
				final int timeLeft = (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
				
				String formula;
				try {
					formula = qbf.flatten(tree, colorSize, k, logger, efPairs, actions,
							bfsConstraints, forbiddenYs, finishTime, false);
				} catch (FormulaSizeException | TimeLimitExceeded e) {
					logger.info("FORMULA FOR k = " + k + " IS TOO LARGE OR REQUIRES TOO MUCH TIME TO CONSTRUCT");
					logger.info(new SolverResult(SolverResults.UNKNOWN).toString());
					return Optional.empty();
				}
				Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(formula,
						logger, solverParams, timeLeft);
				List<Assignment> list = solution.getLeft();
				long time = solution.getRight();
				if (list.isEmpty()) {
					logger.info(new SolverResult(time >= timeLeft * 1000
							? SolverResults.UNKNOWN : SolverResults.UNSAT).toString());
					return Optional.empty();
				} else {
					final Automaton a = constructAutomatonFromAssignment(logger,
							list, tree, colorSize, true).getLeft();
					if (verifier.verify(a)) {
						logger.info(new SolverResult(list).toString().split("\n")[0]);
						return Optional.of(a);
					} else {
						continue;
					}
				}
			}
		} else {
			for (int k = 0; ; k++) {
				if (System.currentTimeMillis() > finishTime) {
					logger.info("TIME LIMIT EXCEEDED");
					return Optional.empty();
				}
				logger.info("TRYING k = " + k);
				deleteTrash();
				QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
						formulae, colorSize, k, extractSubterms, complete, bfsConstraints,
						inlineZVars, efPairs, actions).getFormula(false);
				final int timeLeft = (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
				SolverResult ass = qbf.solve(logger, solver, solverParams, timeLeft);
				logger.info(ass.toString().split("\n")[0]);

				if (ass.type() == SolverResults.SAT) {
					final Automaton a = constructAutomatonFromAssignment(logger, ass.list(),
							tree, colorSize, true).getLeft();
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
}

package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.FormulaList;
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
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree,
			List<LtlNode> formulae, int colorSize, String ltlFilePath,
			int timeoutSeconds, Solvers solver, String solverParams, boolean extractSubterms,
			boolean complete, boolean useSat, boolean bfsConstraints,
			List<EventExpressionPair> efPairs, List<String> actions) throws IOException {
		
		final Verifier verifier = new Verifier(colorSize, logger, ltlFilePath,
				EventExpressionPair.getEvents(efPairs), actions);
		final long finishTime = System.currentTimeMillis() + timeoutSeconds * 1000;
		
		if (useSat) {
			int k = 0;
			boolean maxKFound = false;
			int iteration = 1;
			String curFormula = null;
			FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
			while (true) {
				if (System.currentTimeMillis() > finishTime) {
					logger.info("TIME LIMIT EXCEEDED");
					return Optional.empty();
				}
				deleteTrash();
				logger.info("TRYING k = " + k + (maxKFound ? (", iteration " + ++iteration) : ""));
				String formula = null;
				if (maxKFound) {
					formula = curFormula + "&" + additionalConstraints.assemble().toLimbooleString();
				} else {
					// try next k
					k++;
					long time = 0;
					try {
						QuantifiedBooleanFormula qbf = new QbfFormulaBuilder(logger, tree,
								formulae, colorSize, k, extractSubterms, complete, bfsConstraints,
								efPairs, actions).getFormula(true);
						time = System.currentTimeMillis();
						formula = qbf.flatten(tree, colorSize, k, logger, efPairs, bfsConstraints);
						curFormula = formula;
						additionalConstraints = new FormulaList(BinaryOperations.AND);
					} catch (FormulaSizeException | TimeLimitExceeded e) {
						logger.info("FORMULA FOR k = " + k + " IS TOO LARGE OR REQUIRES TOO MUCH TIME TO CONSTRUCT, STARTING ITERATIONS");
						logger.info("TRIED CREATING FORMULA FOR " + (System.currentTimeMillis() - time) + "ms");
						k--;
						maxKFound = true;
						continue;
					}
				}
				
				final int timeLeft = (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
				Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(formula,
						logger, solverParams, timeLeft);
				List<Assignment> list = solution.getLeft();
				long time = solution.getRight();
				if (list.isEmpty()) {
					SolverResult sr = new SolverResult(time >= timeLeft * 1000
							? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time);
					logger.info(sr.toString());
					return Optional.empty();
				} else {
					final Automaton a = constructAutomatonFromAssignment(logger,
							list, tree, colorSize, true).getLeft();
					if (verifier.verify(a)) {
						SolverResult sr = new SolverResult(list, (int) time);
						logger.info(sr.toString().split("\n")[0]);
						return Optional.of(a);
					}
				}
				
				final List<BooleanFormula> constraints = list
						.stream().filter(va -> va.var.name.startsWith("y") || va.var.name.startsWith("z"))
						.map(v -> v.value ? v.var : v.var.not()).collect(Collectors.toList());
				additionalConstraints.add(BinaryOperation.and(constraints).not());
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
						efPairs, actions).getFormula(false);
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

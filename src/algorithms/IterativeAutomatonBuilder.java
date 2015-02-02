package algorithms;

import java.io.IOException;
import java.io.PrintWriter;
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
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;
import algorithms.FormulaBuilder.EventExpressionPair;

public class IterativeAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Optional<Automaton> automatonFromFormula(BooleanFormula bf, Logger logger, String solverParams,
			int timeoutSeconds, ScenariosTree tree, int colorSize, FormulaList additionalConstraints) throws IOException {
		deleteTrash();
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(bf.toString());
		}
		
		// SAT-solve
		final String strBf = bf.simplify().toLimbooleString();
		final Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(strBf, logger, solverParams, timeoutSeconds);
		final List<Assignment> list = solution.getLeft();
		final long time = solution.getRight();
		
		final SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT, (int) time)
			: new SolverResult(list, (int) time);
		logger.info(ass.toString().split("\n")[0]);

		// add new constraints
		final List<Assignment> assList = list.stream()
				.filter(a -> !a.var.name.startsWith("x"))
				.collect(Collectors.toList());
		final List<BooleanFormula> constraints = assList.stream()
				.map(a -> a.value ? a.var : a.var.not())
				.collect(Collectors.toList());
		additionalConstraints.add(BinaryOperation.and(constraints).not());
		
		return ass.type() != SolverResults.SAT ? Optional.empty()
				: Optional.of(constructAutomatonFromAssignment(logger, ass, tree, colorSize, false));
	}
	
	private static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + iterations);
		return a;
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, String solverParams, boolean complete,
			int timeoutSeconds, String resultFilePath, String ltlFilePath, List<LtlNode> formulae, boolean bfsConstraints,
			List<EventExpressionPair> efPairs, List<String> actions) throws IOException {
		final BooleanFormula initialBf = new SatFormulaBuilder(tree, colorSize, complete, bfsConstraints, efPairs, actions).getFormula();
		final FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		
		final long finishTime = System.currentTimeMillis() + timeoutSeconds * 1000;
		final Verifier verifier = new Verifier(colorSize, logger, ltlFilePath, EventExpressionPair.getEvents(efPairs), actions);
		for (int iterations = 0; System.currentTimeMillis() < finishTime; iterations++) {
			iterations++;
			BooleanFormula actualFormula = initialBf.and(additionalConstraints.assemble());
			final int secondsLeft = (int) ((finishTime - System.currentTimeMillis()) / 1000 + 1);
			Optional<Automaton> automaton = automatonFromFormula(actualFormula, logger, solverParams,
					secondsLeft, tree, colorSize, additionalConstraints);
			if (automaton.isPresent()) {
				if (verifier.verify(automaton.get())) {
					if (complete) {
						try {
							// extra transition search with verification
							new AutomatonCompleter(verifier, automaton.get(), efPairs, actions, finishTime).ensureCompleteness();
						} catch (AutomatonFound e) {
							// verified, complete
							return reportResult(logger, iterations, Optional.of(e.automaton));
						} catch (TimeLimitExceeded e) {
							// terminate the loop
							break;
						}
						// no complete extensions, continue search
					} else {
						// verified, completeness is not required
						return reportResult(logger, iterations, automaton);
					}
				}
			} else {
				// no solution due to UNSAT or UNKNOWN, stop search
				return reportResult(logger, iterations, Optional.empty());
			}
		}
		logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
		return Optional.empty();
	}
}

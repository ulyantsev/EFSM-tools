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
import qbf.reduction.SatSolver;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;

public class IterativeAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Optional<Automaton> automatonFromFormula(BooleanFormula bf, Logger logger,
			String solverParams, int timeoutSeconds, ScenariosTree tree, int colorSize,
			FormulaList additionalConstraints, SatSolver satSolver) throws IOException {
		deleteTrash();
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(bf.toString());
		}
		
		// SAT-solve
		final String strBf = bf.simplify().toLimbooleString();
		final Pair<List<Assignment>, Long> solution = BooleanFormula.solveAsSat(strBf, logger,
				solverParams, timeoutSeconds, satSolver);
		final List<Assignment> list = solution.getLeft();
		final long time = solution.getRight();
		
		final SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT)
			: new SolverResult(list);
		logger.info(ass.toString().split("\n")[0]);

		if (ass.type() == SolverResults.SAT) {
			Pair<Automaton, List<Assignment>> p = constructAutomatonFromAssignment(logger, ass.list(), tree, colorSize, false);

			// add new constraints
			// important: scenario-unsupported y-variables are not included!
			// (because we will try all the redirections of such transitions)
			final List<BooleanFormula> constraints = p.getRight().stream()
					.map(a -> a.value ? a.var : a.var.not())
					.collect(Collectors.toList());
			additionalConstraints.add(BinaryOperation.and(constraints).not());

			return Optional.of(p.getLeft());
		} else {
			return Optional.empty();
		}
		
	}
	
	private static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + iterations);
		return a;
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, String solverParams,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, boolean complete) throws IOException {
		final BooleanFormula initialBf = new SatFormulaBuilder(tree, colorSize, events, actions).getFormula();
		final FormulaList additionalConstraints = new FormulaList(BinaryOperations.AND);
		
		for (int iterations = 0; System.currentTimeMillis() < finishTime; iterations++) {
			iterations++;
			BooleanFormula actualFormula = initialBf.and(additionalConstraints.assemble());
			final int secondsLeft = (int) ((finishTime - System.currentTimeMillis()) / 1000 + 1);
			Optional<Automaton> automaton = automatonFromFormula(actualFormula, logger, solverParams,
					secondsLeft, tree, colorSize, additionalConstraints, satSolver);
			if (automaton.isPresent()) {
				if (verifier.verify(automaton.get())) {
					if (!complete) {
						return reportResult(logger, iterations, automaton);
					}
					try {
						// extra transition search with verification
						new AutomatonCompleter(verifier, automaton.get(), events, actions, finishTime).ensureCompleteness();
					} catch (AutomatonFound e) {
						// verified, complete
						return reportResult(logger, iterations, Optional.of(e.automaton));
					} catch (TimeLimitExceeded e) {
						// terminate the loop
						break;
					}
					// no complete extensions, continue search
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

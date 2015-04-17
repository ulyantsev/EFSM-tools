package algorithms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import algorithms.AutomatonCompleter.CompletenessType;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Assignment;
import qbf.reduction.BooleanFormula.SolveAsSatResult;
import qbf.reduction.ExpandableStringFormula;
import qbf.reduction.SatSolver;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;

public class IterativeAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Optional<Automaton> automatonFromFormula(ExpandableStringFormula f, Logger logger,
			int timeoutSeconds, ScenariosTree tree, int size) throws IOException {
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(f.toString());
		}
		
		// SAT-solve
		final SolveAsSatResult solution = f.solve(timeoutSeconds);
		final List<Assignment> list = solution.list();
		final long time = solution.time;
		
		final SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT)
			: new SolverResult(list);
		logger.info(ass.type().toString());

		if (ass.type() == SolverResults.SAT) {
			final Pair<Automaton, List<Assignment>> p = constructAutomatonFromAssignment
					(logger, ass.list(), tree, size, false);

			// add new constraints
			// important: scenario-unsupported y-variables are not included!
			// (because we will try all the redirections of such transitions)
			f.addProhibitionConstraint(p.getRight().stream()
					.map(v -> v.negate()).collect(Collectors.toList()));
			return Optional.of(p.getLeft());
		} else {
			return Optional.empty();
		}
	}
	
	private static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + iterations);
		return a;
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int size, String solverParams,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, boolean complete, CompletenessType completenessType) throws IOException {
		deleteTrash();
		try (final ExpandableStringFormula f = new ExpandableStringFormula(
				new SatFormulaBuilder(tree, size, events, actions, false,
						CompletenessType.NORMAL, false).getFormula().simplify()
				.toLimbooleString(), logger, satSolver, solverParams)) {
			for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
				final int secondsLeft = (int) ((finishTime - System.currentTimeMillis()) / 1000 + 1);
				final Optional<Automaton> automaton = automatonFromFormula(f, logger,
						secondsLeft, tree, size);
				if (automaton.isPresent()) {
					if (verifier.verify(automaton.get())) {
						if (!complete) {
							return reportResult(logger, iteration, automaton);
						}
						try {
							// extra transition search with verification
							new AutomatonCompleter(verifier, automaton.get(), events, actions,
									finishTime, completenessType).ensureCompleteness();
						} catch (AutomatonFound e) {
							// verified, complete
							return reportResult(logger, iteration, Optional.of(e.automaton));
						} catch (TimeLimitExceeded e) {
							// terminate the loop
							break;
						}
						// no complete extensions, continue search
					}
				} else {
					// no solution due to UNSAT or UNKNOWN, stop search
					return reportResult(logger, iteration, Optional.empty());
				}
			}
			logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
			return Optional.empty();
		}
	}
}

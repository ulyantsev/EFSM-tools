package algorithms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.verifier.VerifierFactory.Counterexample;
import qbf.reduction.Assignment;
import qbf.reduction.BooleanFormula.SolveAsSatResult;
import qbf.reduction.BooleanVariable;
import qbf.reduction.ExpandableStringFormula;
import qbf.reduction.SatSolver;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;
import structures.Transition;
import algorithms.AutomatonCompleter.CompletenessType;
import bool.MyBooleanExpression;

public class CounterexampleAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Optional<Automaton> automatonFromFormula(ExpandableStringFormula f, Logger logger,
			int timeoutSeconds, ScenariosTree tree, int size,
			CompletenessType completenessType) throws IOException {
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
			return Optional.of(constructAutomatonFromAssignment
					(logger, ass.list(), tree, size, true, completenessType).getLeft());
		} else {
			return Optional.empty();
		}
	}
	
	private static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + iterations);
		return a;
	}
	
	private static List<List<Assignment>> counterexamplesToAssignments(Logger logger, List<Counterexample> counterexamples,
			Automaton a, List<String> actions) {
		final List<List<Assignment>> allConstraints = new ArrayList<>();
		final Set<String> uniqueCounterexamples = new HashSet<>();
		for (Counterexample counterexample : counterexamples) {
			if (counterexample.isEmpty()) {
				continue;
			}
			
			final Set<String> prohibitedVarNames = new LinkedHashSet<>();
			final List<Assignment> prohibitedVars = new ArrayList<>();
			
			int state = a.getStartState().getNumber();
			for (String event : counterexample.events()) {
				final Transition t = a.getState(state).getTransition(event, MyBooleanExpression.getTautology());
				final int newState = t.getDst().getNumber();
				final BooleanVariable yVar = FormulaBuilder.yVar(state, newState, event);
				if (!prohibitedVarNames.contains(yVar.name)) {
					prohibitedVarNames.add(yVar.name);
					prohibitedVars.add(new Assignment(yVar, false));
				}
				final Set<String> transActions = new LinkedHashSet<>(Arrays.asList(t.getActions().getActions()));
				for (String action : actions) {
					final BooleanVariable zVar = FormulaBuilder.zVar(state, action, event);
					if (!prohibitedVarNames.contains(zVar.name)) {
						prohibitedVarNames.add(zVar.name);
						final Assignment zAss = new Assignment(zVar, !transActions.contains(action));
						prohibitedVars.add(zAss);
					}
				}
				state = newState;
			}
			
			if (!uniqueCounterexamples.contains(prohibitedVars.toString())) {
				uniqueCounterexamples.add(prohibitedVars.toString());
				allConstraints.add(prohibitedVars);
			}
		}
		return allConstraints;
	}

	// try to replace actions, or add counterexamples in case of failure
	public static Optional<Automaton> addCounterexamples(Logger logger, List<Counterexample> counterexamples,
			Automaton a, ExpandableStringFormula f, List<String> actions) throws IOException {
		final List<List<Assignment>> allConstraints = counterexamplesToAssignments(logger, counterexamples, a, actions);
		f.addProhibitionConstraints(allConstraints);
		logger.info("ADDED " + allConstraints.size() + " COUNTEREXAMPLES");
		return Optional.empty();
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int size, String solverParams,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, CompletenessType completenessType) throws IOException {
		deleteTrash();
		try (final ExpandableStringFormula f = new ExpandableStringFormula(
				new SatFormulaBuilder(tree, size, events, actions, completenessType, true).getFormula().simplify()
				.toLimbooleString(), logger, satSolver, solverParams)) {
			for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
				final int secondsLeft = (int) ((finishTime - System.currentTimeMillis()) / 1000 + 1);
				Optional<Automaton> automaton
						= automatonFromFormula(f, logger, secondsLeft, tree, size, completenessType);
				if (automaton.isPresent()) {
					final List<Counterexample> counterexamples
						= verifier.verifyWithCounterExamples(automaton.get());
					final boolean verified = counterexamples.stream().allMatch(Counterexample::isEmpty);
					if (verified) {
						return reportResult(logger, iteration, Optional.of(automaton.get()));
					} else {
						Optional<Automaton> a = addCounterexamples(logger, counterexamples, automaton.get(),
								f, actions);
						if (a.isPresent()) {
							return reportResult(logger, iteration, a);
						}
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

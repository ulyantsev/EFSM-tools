package algorithms;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.verifier.VerifierFactory.Counterexample;
import qbf.reduction.Assignment;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanFormula.SolveAsSatResult;
import qbf.reduction.SatSolver;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import scenario.StringScenario;
import structures.Automaton;
import structures.NegativeScenariosTree;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
import algorithms.AutomatonCompleter.CompletenessType;
import bool.MyBooleanExpression;

public class TrueCounterexampleAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static Optional<Automaton> automatonFromFormula(String f,
			Logger logger, int timeoutSeconds, ScenariosTree tree, int size, SatSolver satSolver,
			String solverParams, CompletenessType completenessType) throws IOException {
		try (PrintWriter pw = new PrintWriter("_tmp.pretty")) {
			pw.print(f.toString());
		}
		
		// SAT-solve
		final SolveAsSatResult solution = BooleanFormula.solveAsSat(f,
				logger, solverParams, timeoutSeconds, satSolver);
		final List<Assignment> list = solution.list();
		final long time = solution.time;
		
		final SolverResult ass = list.isEmpty()
			? new SolverResult(time >= timeoutSeconds * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT)
			: new SolverResult(list);
		logger.info(ass.type().toString());

		if (ass.type() == SolverResults.SAT) {
			//logger.info(list.stream().filter(a -> a.var.name.startsWith("xx_") && a.value).collect(Collectors.toList()).toString());
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
	
	private static void addCounterexample(Logger logger, Automaton a,
			Counterexample counterexample, NegativeScenariosTree negativeTree) {
		int state = a.getStartState().getNumber();
		final List<MyBooleanExpression> expressions = new ArrayList<>();
		final List<StringActions> actions = new ArrayList<>();
		List<String> description = new ArrayList<>();
		List<Integer> states = new ArrayList<>();
		states.add(state);
		for (String event : counterexample.events()) {
			final Transition t = a.getState(state).getTransition(event, MyBooleanExpression.getTautology());
			expressions.add(t.getExpr());
			actions.add(t.getActions());
			description.add(event + "/[" + t.getActions() + "]");
			final int newState = t.getDst().getNumber();			
			state = newState;
			states.add(newState);
		}
		logger.info("ADDING COUNTEREXAMPLE: " + description);
		try {
			negativeTree.addScenario(new StringScenario(true, counterexample.events(), expressions, actions),
					counterexample.loopLength);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int size, String solverParams,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, CompletenessType completenessType) throws IOException {
		deleteTrash();
		
		final NegativeScenariosTree negativeTree = new NegativeScenariosTree();
		
		for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
			String formula = new SatFormulaBuilderNegativeSC(tree, size, events, actions, completenessType, negativeTree)
				.getFormula().simplify().toLimbooleString();
			final int secondsLeft = (int) ((finishTime - System.currentTimeMillis()) / 1000 + 1);
			final Optional<Automaton> automaton = automatonFromFormula(formula, logger,
					secondsLeft, tree, size, satSolver, solverParams, completenessType);
			if (automaton.isPresent()) {
				List<Counterexample> counterexamples = verifier.verifyWithCounterExamples(automaton.get());
				boolean verified = counterexamples.stream().allMatch(Counterexample::isEmpty);
				if (verified) {
					return reportResult(logger, iteration, automaton);
				}
				for (Counterexample counterexample : counterexamples) {
					if (!counterexample.isEmpty()) {
						addCounterexample(logger, automaton.get(), counterexample, negativeTree);
					} else {
						logger.info("NOT ADDING COUNTEREXAMPLE");
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
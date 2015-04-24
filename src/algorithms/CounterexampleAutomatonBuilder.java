package algorithms;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.verifier.VerifierFactory.Counterexample;
import qbf.reduction.Assignment;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanFormula.SolveAsSatResult;
import qbf.reduction.BooleanVariable;
import qbf.reduction.FormulaList;
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

public class CounterexampleAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	protected static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + iterations);
		return a;
	}
	
	protected static void addCounterexample(Logger logger, Automaton a,
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
		logger.info("ADDING COUNTEREXAMPLE: " + description + ", LOOP LENGTH " + counterexample.loopLength);
		try {
			negativeTree.addScenario(new StringScenario(true, counterexample.events(), expressions, actions),
					counterexample.loopLength);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected static void addProhibited(Logger logger, List<Assignment> list,
			List<BooleanFormula> prohibited) {
		final FormulaList options = new FormulaList(BinaryOperations.OR);
		for (Assignment ass : list) {
			if (ass.var.name.startsWith("y_") && ass.value || ass.var.name.startsWith("z_")) {
				options.add(ass.value ? ass.var.not() : ass.var);
			}
		}
		prohibited.add(options.assemble());
	}
	
	protected final static boolean USE_COMPLETENESS_HEURISTICS = true;
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int size, String solverParams,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, SatSolver satSolver,
			Verifier verifier, long finishTime, CompletenessType completenessType,
			NegativeScenariosTree negativeTree) throws IOException {
		deleteTrash();
		
		final List<BooleanFormula> prohibited = new ArrayList<>();
		final CompletenessType effectiveCompletenessType = USE_COMPLETENESS_HEURISTICS
				? CompletenessType.NO_DEAD_ENDS : completenessType;
		
		for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
			BooleanVariable.eraseVariables();
			final String formula = new SatFormulaBuilderNegativeSC(tree, size, events, actions,
					effectiveCompletenessType, negativeTree, prohibited)
					.getFormula().simplify().toLimbooleString();
			// SAT-solve
			final int secondsLeft = timeLeftForSolver(finishTime);
			final SolveAsSatResult solution = BooleanFormula.solveAsSat(formula,
					logger, solverParams, secondsLeft, satSolver);
			final List<Assignment> list = solution.list();
			final long time = solution.time;
			
			final SolverResult ass = list.isEmpty()
				? new SolverResult(time >= secondsLeft * 1000 ? SolverResults.UNKNOWN : SolverResults.UNSAT)
				: new SolverResult(list);
			logger.info(ass.type().toString());

			final Optional<Automaton> automaton = ass.type() == SolverResults.SAT
				? Optional.of(constructAutomatonFromAssignment
						(logger, ass.list(), tree, size, true, effectiveCompletenessType).getLeft())
				: Optional.empty();
			
			if (automaton.isPresent()) {
				final List<Counterexample> counterexamples = verifier.verifyWithCounterExamples(automaton.get());
				final boolean verified = counterexamples.stream().allMatch(Counterexample::isEmpty);
				if (verified) {
					if (completenessType == CompletenessType.NORMAL && USE_COMPLETENESS_HEURISTICS) {
						logger.info("STARTING HEURISTIC COMPLETION");
						try {
							new AutomatonCompleter(verifier, automaton.get(), events, actions,
									finishTime, CompletenessType.NORMAL).ensureCompleteness();
						} catch (AutomatonFound e) {
							return reportResult(logger, iteration, Optional.of(e.automaton));
						} catch (TimeLimitExceeded e) {
							logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
							return reportResult(logger, iteration, Optional.empty());
						}
						addProhibited(logger, list, prohibited);
						logger.info("ADDED PROHIBITED FSM");
					} else {
						return reportResult(logger, iteration, automaton);
					}
				} else {
					for (Counterexample counterexample : counterexamples) {
						if (!counterexample.isEmpty()) {
							addCounterexample(logger, automaton.get(), counterexample, negativeTree);
						} else {
							logger.info("NOT ADDING COUNTEREXAMPLE");
						}
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
	
	protected static int timeLeftForSolver(long finishTime) {
		return (int) (finishTime - System.currentTimeMillis()) / 1000 + 1;
	}
}
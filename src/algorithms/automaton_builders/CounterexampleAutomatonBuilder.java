package algorithms.automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import egorov.ltl.grammar.LtlNode;
import egorov.verifier.Counterexample;
import egorov.verifier.Verifier;
import sat_solving.Assignment;
import sat_solving.ExpandableStringFormula;
import sat_solving.SatSolver;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import scenario.StringActions;
import scenario.StringScenario;
import structures.Automaton;
import structures.NegativeScenariosTree;
import structures.ScenarioTree;
import structures.Transition;
import algorithms.AutomatonCompleter;
import algorithms.AutomatonCompleter.CompletenessType;
import algorithms.exception.AutomatonFoundException;
import algorithms.exception.TimeLimitExceededException;
import algorithms.formula_builders.CounterexampleFormulaBuilder;
import bnf_formulae.BinaryOperations;
import bnf_formulae.BooleanFormula;
import bnf_formulae.FormulaList;
import bnf_formulae.BooleanFormula.SolveAsSatResult;
import bool.MyBooleanExpression;

public class CounterexampleAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	protected static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + (iterations + 1));
		return a;
	}
	
	protected static void addCounterexample(Logger logger, Automaton a,
			Counterexample counterexample, NegativeScenariosTree negativeTree) {
		int state = a.startState().number();
		final List<MyBooleanExpression> expressions = new ArrayList<>();
		final List<StringActions> actions = new ArrayList<>();
		for (String event : counterexample.events()) {
			final Transition t = a.state(state).transition(event, MyBooleanExpression.getTautology());
			expressions.add(t.expr());
			actions.add(t.actions());
			final int newState = t.dst().number();			
			state = newState;
		}
		logger.info("ADDING COUNTEREXAMPLE: " + counterexample);
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
		
	public static Optional<Automaton> build(Logger logger, ScenarioTree tree, int size, String solverParams,
			String resultFilePath, List<LtlNode> formulae, List<String> events, List<String> actions,
			SatSolver satSolver, Verifier verifier, long finishTime, CompletenessType completenessType,
			NegativeScenariosTree negativeTree, boolean useCompletenessHeuristics) throws IOException {
		deleteTrash();
		
		final List<BooleanFormula> prohibited = new ArrayList<>();
		final CompletenessType effectiveCompletenessType = useCompletenessHeuristics
				? CompletenessType.NO_DEAD_ENDS : completenessType;
		
		ExpandableStringFormula expandableFormula = null;
		final Set<BooleanFormula> previousConstraints = new LinkedHashSet<>();
		final Set<BooleanFormula> negationConstraints = new LinkedHashSet<>();
		
		for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
			final CounterexampleFormulaBuilder builder = new CounterexampleFormulaBuilder(tree, size, events, actions,
					effectiveCompletenessType, negativeTree, prohibited);
			final FormulaList negationList = new FormulaList(BinaryOperations.AND);
			if (expandableFormula == null) {
				final BooleanFormula basicFormula = builder.getBasicFormula();
				negationConstraints.addAll(builder.getNegationConstraints());
				negationConstraints.stream().forEach(negationList::add);
				final String formula = basicFormula.and(negationList.assemble())
						.simplify().toLimbooleString();
				expandableFormula = new ExpandableStringFormula(formula, logger, satSolver, solverParams);
			} else {
				negationConstraints.addAll(builder.getNegationConstraints());
				final Set<BooleanFormula> diffConstraints = new LinkedHashSet<>(negationConstraints);
				diffConstraints.removeAll(previousConstraints);
				diffConstraints.stream().forEach(negationList::add);
				final String negationFormula = negationList.assemble().simplify().toLimbooleString();
				final List<String> negativeDimacsConstraints = BooleanFormula.extendDimacs(negationFormula,
						logger, "_tmp.incremental.dimacs", expandableFormula.info());
				expandableFormula.addConstraints(negativeDimacsConstraints);
			}
			previousConstraints.addAll(negationConstraints);

			// SAT-solve
			final int secondsLeft = timeLeftForSolver(finishTime);
			final SolveAsSatResult solution = expandableFormula.solve(secondsLeft);
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
				negativeTree.checkColoring(list, size);
				final List<Counterexample> counterexamples = verifier.verifyWithCounterexamples(automaton.get());
				final boolean verified = counterexamples.stream().allMatch(Counterexample::isEmpty);
				if (verified) {
					if (completenessType == CompletenessType.NORMAL && useCompletenessHeuristics) {
						logger.info("STARTING HEURISTIC COMPLETION");
						try {
							new AutomatonCompleter(verifier, automaton.get(), events, actions,
									finishTime, CompletenessType.NORMAL).ensureCompleteness();
						} catch (AutomatonFoundException e) {
							return reportResult(logger, iteration, Optional.of(e.automaton));
						} catch (TimeLimitExceededException e) {
							logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
							return reportResult(logger, iteration, Optional.empty());
						}
						addProhibited(logger, list, prohibited);
						logger.info("ADDED PROHIBITED FSM");
					} else {
						return reportResult(logger, iteration, automaton);
					}
				} else {
					final Set<String> unique = new HashSet<String>();
					for (Counterexample counterexample : counterexamples) {
						if (!counterexample.isEmpty()) {
							if (!unique.contains(counterexample.toString())) {
								unique.add(counterexample.toString());
								addCounterexample(logger, automaton.get(), counterexample, negativeTree);
							} else {
								logger.info("DUPLICATE COUNTEREXAMPLES ON THE SAME ITERATION");
							}
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
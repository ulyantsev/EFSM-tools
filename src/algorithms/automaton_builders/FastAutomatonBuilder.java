package algorithms.automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sat_solving.IncrementalInterface;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import scenario.StringActions;
import scenario.StringScenario;
import structures.Automaton;
import structures.NegativeScenarioTree;
import structures.ScenarioTree;
import algorithms.AutomatonCompleter.CompletenessType;
import algorithms.formula_builders.FastAutomatonFormulaBuilder;
import bnf_formulae.BinaryOperations;
import bnf_formulae.BooleanVariable;
import bnf_formulae.FormulaList;
import bool.MyBooleanExpression;
import egorov.ltl.grammar.LtlNode;
import egorov.verifier.Counterexample;
import egorov.verifier.Verifier;

public class FastAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	protected static Optional<Automaton> reportResult(Logger logger, int iterations, Optional<Automaton> a) {
		logger.info("ITERATIONS: " + (iterations + 1));
		return a;
	}
	
	/*
	 * LTL, G(<propositional formula>)
	 */
	private static String transitionSpecification(List<String> strFormulae, int states,
			List<String> events, List<String> actions) {
		if (strFormulae.isEmpty()) {
			return null;
		}
		final List<String> additionalFormulae = new ArrayList<>();
		final String eventRegexStart = "(event|wasEvent)\\((ep\\.)?";
		final String actionRegexStart = "(action|wasAction)\\((co\\.)?";
		for (String formula : strFormulae) {
			if (formula.isEmpty()) {
				continue;
			}
			if (formula.startsWith("G(") && formula.endsWith(")")) {
				formula = formula.substring(2, formula.length() - 1);
			} else {
				continue;
			}
			
			// FIXME make a better check
			if (formula.matches("^.*[GFXUR]\\s*\\(.*$")) {
				continue;
			}
			
			formula = ltl2limboole(formula);
			for (int i = 0; i < states; i++) {
				for (String event : events) {
					String constraint = formula;
					final FormulaList options = new FormulaList(BinaryOperations.OR);
					for (int j = 0; j < states; j++) {
						options.add(BooleanVariable.byName("y", i, j, event).get());
					}
					final String eRepl = options.assemble().toLimbooleString();
					constraint = constraint.replaceAll(eventRegexStart + event + "\\)", eRepl);
					for (String otherEvent : events) {
						if (!otherEvent.equals(event)) {
							constraint = constraint.replaceAll(eventRegexStart + otherEvent + "\\)", "(1&!1)");
						}
					}
					for (String action : actions) {
						final String acRepl = BooleanVariable.byName("z", i, action, event).get().toLimbooleString();
						constraint = constraint.replaceAll(actionRegexStart + action + "\\)", acRepl);
					}
					additionalFormulae.add(constraint);
				}
			}
		}
		return additionalFormulae.isEmpty()
				? null
				: ("(" + String.join(")&(", additionalFormulae) + ")");
	}
	
	private static boolean containsTrue(boolean[] array){
	    for (boolean val : array) {
	        if (val) {
	            return true;
	        }
	    }
	    return false;
	}
	
	public static Optional<Automaton> build(Logger logger, ScenarioTree positiveTree,
			NegativeScenarioTree negativeTree, int size, String resultFilePath,
			List<String> strFormulae, List<LtlNode> formulae, List<String> events,
			List<String> actions, Verifier verifier, long finishTime,
			boolean complete, boolean bfsConstraints, boolean useGlobalTree) throws IOException {
		deleteTrash();
		
		final boolean[] ltlIsG = new boolean[strFormulae.size()];
		for (int i = 0; i < strFormulae.size(); i++) {
			ltlIsG[i] = strFormulae.get(i).matches(Verifier.G_REGEX);
		}
		if (!containsTrue(ltlIsG)) {
			useGlobalTree = false;
		}
		final Verifier globalVerifier = useGlobalTree
				? verifier.globalVerifier() : null;
		final NegativeScenarioTree globalTree = new NegativeScenarioTree();
		
		IncrementalInterface incr = null;
		
		for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
			final FastAutomatonFormulaBuilder builder = new FastAutomatonFormulaBuilder(size, positiveTree,
					negativeTree, globalTree, events, actions, complete, bfsConstraints);
			builder.createVars();
			final int secondsLeft = timeLeftForSolver(finishTime);
			if (iteration == 0) {
				final List<int[]> constraints = builder.positiveConstraints();
				final String transSpec = transitionSpecification(strFormulae, size, events, actions);
				incr = new IncrementalInterface(constraints, transSpec, logger);
			}
			
			// SAT-solve
			final SolverResult ass = incr.solve(builder.negativeConstraints(), secondsLeft);
			logger.info(ass.type().toString());
			if (ass.type() != SolverResults.SAT) {
				return reportResult(logger, iteration, Optional.empty());
			}

			final Automaton automaton = constructAutomatonFromAssignment(logger, ass.list(),
					positiveTree, size, true,
					complete ? CompletenessType.NORMAL : CompletenessType.NO_DEAD_ENDS).getLeft();

			// verify
			final List<Counterexample> counterexamples =
					verifier.verifyWithCounterexamplesWithNoDeadEndRemoval(automaton);
			
			if (counterexamples.stream().allMatch(Counterexample::isEmpty)) {
				incr.halt();
				return reportResult(logger, iteration, Optional.of(automaton));
			} else if (useGlobalTree) {
				final List<Counterexample> globalCounterexamples =
						globalVerifier.verifyWithCounterexamplesWithNoDeadEndRemoval(automaton);
				final Set<Counterexample> normalCEs = new LinkedHashSet<>();
				final Set<Counterexample> globalCEs = new LinkedHashSet<>();
				int globalIndex = 0;
				for (int i = 0; i < counterexamples.size(); i++) {
					final Counterexample normalCE = counterexamples.get(i);
					if (ltlIsG[i]) {
						final Counterexample globalCE = globalCounterexamples.get(globalIndex++);
						if (!globalCE.isEmpty() && globalCE.loopLength == 0) {
							globalCEs.add(globalCE);
						} else if (!normalCE.isEmpty()) {
							normalCEs.add(normalCE);
						}
					} else if (!normalCE.isEmpty()) {
						normalCEs.add(normalCE);
					}
				}
				normalCEs.stream().forEach(ce -> addCounterexample(logger, size, ce, negativeTree));
				globalCEs.stream().forEach(ce -> addCounterexample(logger, size, ce, globalTree));
			} else {
				counterexamples.stream()
						.filter(ce -> !ce.isEmpty()).distinct()
						.forEach(ce -> addCounterexample(logger, size, ce, negativeTree));
			}
		}
		incr.halt();
		logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
		return Optional.empty();
	}

	protected static void addCounterexample(Logger logger, int size,
			Counterexample counterexample, NegativeScenarioTree negativeForest) {
		final List<MyBooleanExpression> expr = new ArrayList<>();
		for (int i = 0; i < counterexample.events().size(); i++) {
			expr.add(MyBooleanExpression.getTautology());
		}
		final List<StringActions> actions = counterexample.actions().stream().map(
				action -> new StringActions(String.join(",", action))
		).collect(Collectors.toList());
		try {
			negativeForest.addScenario(new StringScenario(true,
					counterexample.events(), expr, actions), counterexample.loopLength);
		} catch (ParseException e) {
			throw new AssertionError();
		}
		logger.info("ADDING COUNTEREXAMPLE: " + counterexample);
	}
}
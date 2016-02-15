package algorithms.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import sat_solving.Assignment;
import sat_solving.IncrementalInterface;
import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import scenario.StringActions;
import scenario.StringScenario;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NegativePlantScenarioForest;
import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import algorithms.automaton_builders.ScenarioAndLtlAutomatonBuilder;
import bnf_formulae.BooleanVariable;
import bool.MyBooleanExpression;
import egorov.ltl.grammar.LtlNode;
import egorov.verifier.Counterexample;
import egorov.verifier.SimpleVerifier;
import egorov.verifier.NondetMooreVerifierPair;

public class PlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	protected static Optional<NondetMooreAutomaton> reportResult(Logger logger, int iterations, Optional<NondetMooreAutomaton> a) {
		logger.info("ITERATIONS: " + (iterations + 1));
		return a;
	}
		
	private static NondetMooreAutomaton constructAutomatonFromAssignment(Logger logger, List<Assignment> ass,
			PositivePlantScenarioForest forest, int colorSize) {
		final List<Boolean> isStart = Arrays.asList(ArrayUtils.toObject(new boolean[colorSize]));
		final List<List<String>> actions = new ArrayList<>();
		for (int i = 0; i < isStart.size(); i++) {
			actions.add(new ArrayList<>());
		}
		
		final Map<Integer, Integer> coloring = new HashMap<>();
		for (Assignment a : ass) {
			if (a.value) {
				final String tokens[] = a.var.name.split("_");
				if (tokens[0].equals("x")) {
					for (MooreNode root : forest.roots()) {
						if (tokens[1].equals(root.number() + "")) {
							final int state = Integer.parseInt(tokens[2]);
							isStart.set(state, true);
						}
					}
					final int node = Integer.parseInt(tokens[1]);
					final int color = Integer.parseInt(tokens[2]);
					coloring.put(node, color);
				} else if (tokens[0].equals("z")) {
					final int state = Integer.parseInt(tokens[1]);
					final String action = tokens[2];
					actions.get(state).add(action);
				}
			}
		}
		
		final NondetMooreAutomaton automaton = new NondetMooreAutomaton(colorSize,
				actions.stream().map(l -> new StringActions(String.join(",", l)))
				.collect(Collectors.toList()), isStart);
		
		for (Assignment a : ass) {
			if (a.value) {
				final String tokens[] = a.var.name.split("_");
				if (tokens[0].equals("y")) {
					final int from = Integer.parseInt(tokens[1]);
					final int to = Integer.parseInt(tokens[2]);
					final String event = tokens[3];
					automaton.state(from).addTransition(event, automaton.state(to));
				}
			}
		}
		
		// remove unused transitions unless this does not violate completeness
		final Set<MooreTransition> usedTransitions = new HashSet<>();
		for (MooreNode node : forest.nodes()) {
			final int source = coloring.get(node.number());
			final int transitionNumber = node.transitions().size();
			if (transitionNumber == 0) {
				continue;
			} else if (transitionNumber > 1) {
				throw new AssertionError();
			}
			final MooreTransition scTransition = node.transitions().iterator().next();
			final String event = scTransition.event();
			final int dest = coloring.get(scTransition.dst().number());
			for (MooreTransition t : automaton.state(source).transitions()) {
				if (t.event().equals(event) && t.dst().number() == dest) {
					usedTransitions.add(t);
				}
			}
		}
		for (int i = 0; i < colorSize; i++) {
			final List<MooreTransition> copy = new ArrayList<>(automaton.state(i).transitions());
			for (MooreTransition tCopy : copy) {
				if (!usedTransitions.contains(tCopy)) {
					automaton.state(i).removeTransition(tCopy);
					if (!automaton.state(i).hasTransition(tCopy.event())) {
						// return the transition
						automaton.addTransition(automaton.state(i), tCopy);
					}
				}
			}
		}
		
		return automaton;
	}

	/*
	 * If the filename is not null, returns additional action specification.
	 * The specification is given as in LTL, but without temporal operators.
	 * If there is a propositional formula F over action(...), then the meaning of this formula would be:
	 *   In all states of the automaton, F holds.
	 */
	private static String actionSpecification(String actionspecFilePath, int states,
			List<String> actions) throws FileNotFoundException {
		if (actionspecFilePath == null) {
			return null;
		}
		final List<String> additionalFormulae = new ArrayList<>();
		try (final Scanner sc = new Scanner(new File(actionspecFilePath))) {
			while (sc.hasNextLine()) {
				final String line = sc.nextLine();
				if (line.isEmpty()) {
					continue;
				}
				final String transformed = ltl2limboole(line);
				for (int i = 0; i < states; i++) {
					String constraint = transformed;
					for (String action : actions) {
						constraint = constraint.replaceAll("action\\(" + action + "\\)",
								BooleanVariable.byName("z", i, action).get().toLimbooleString());
					}
					additionalFormulae.add(constraint);
				}
			}
		}
		return additionalFormulae.isEmpty()
				? null
				: ("(" + String.join(")&(", additionalFormulae) + ")");
	}
	
	public static Optional<NondetMooreAutomaton> build(Logger logger, PositivePlantScenarioForest positiveForest,
			NegativePlantScenarioForest negativeForest, int size,
			String actionspecFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, NondetMooreVerifierPair verifier, long finishTime) throws IOException {
		deleteTrash();
		SimpleVerifier.setLoopWeight(size);
		
		final NegativePlantScenarioForest globalNegativeForest = new NegativePlantScenarioForest();
		
		IncrementalInterface incr = null;
		
		for (int iteration = 0; System.currentTimeMillis() < finishTime; iteration++) {
			final PlantFormulaBuilder builder = new PlantFormulaBuilder(size, positiveForest,
					negativeForest, globalNegativeForest, events, actions);
			builder.createVars();
			final int secondsLeft = timeLeftForSolver(finishTime);
			if (iteration == 0) {
				// create
				final List<int[]> positiveConstraints = builder.positiveConstraints();
				final String actionSpec = actionSpecification(actionspecFilePath, size, actions);
				incr = new IncrementalInterface(positiveConstraints, actionSpec, logger);
			}
			
			// SAT-solve
			final SolverResult ass = incr.solve(builder.negativeConstraints(), secondsLeft);
			logger.info(ass.type().toString());
			if (ass.type() != SolverResults.SAT) {
				return reportResult(logger, iteration, Optional.empty());
			}

			final NondetMooreAutomaton automaton = constructAutomatonFromAssignment(logger, ass.list(),
					positiveForest, size);

			// verify
			final Pair<List<Counterexample>, List<Counterexample>> counterexamples =
					verifier.verifyNondetMoore(automaton);
			
			//System.out.println(automaton);
			
			final List<Counterexample> mixedCE = new ArrayList<>(counterexamples.getLeft());
			mixedCE.addAll(counterexamples.getRight());
			if (mixedCE.stream().allMatch(Counterexample::isEmpty)) {
				incr.halt();
				return reportResult(logger, iteration, Optional.of(automaton));
			} else {
				// find minimum CEs of both types
				final List<Counterexample> normalCEs = counterexamples.getLeft().stream()
						.filter(ce -> !ce.isEmpty())
						.map(ce -> collapseLoop(ce, size))
						.distinct()
						.collect(Collectors.toList());
				final List<Counterexample> globalCEs = counterexamples.getRight().stream()
						.filter(ce -> !ce.isEmpty())
						.map(ce -> collapseLoop(ce, size))
						.distinct()
						.collect(Collectors.toList());
				int normalMinIndex = -1;
				for (int i = 0; i < normalCEs.size(); i++) {
					final int prevLength = normalMinIndex == -1
							? Integer.MAX_VALUE : normalCEs.get(normalMinIndex).events().size();
					final int curLength = normalCEs.get(i).events().size();
					if (curLength < prevLength) {
						normalMinIndex = i;
					}
				}
				int globalMinIndex = -1;
				for (int i = 0; i < globalCEs.size(); i++) {
					final int prevLength = globalMinIndex == -1
							? Integer.MAX_VALUE : globalCEs.get(globalMinIndex).events().size();
					final int curLength = globalCEs.get(i).events().size();
					if (curLength < prevLength) {
						globalMinIndex = i;
					}
				}
				if (normalMinIndex == -1 && globalMinIndex != -1) {
					// add global CE
					addCounterexample(logger, size, globalCEs.get(globalMinIndex), globalNegativeForest);
				} else if (normalMinIndex != -1 && globalMinIndex == -1) {
					// add normal CE
					addCounterexample(logger, size, normalCEs.get(normalMinIndex), negativeForest);
				} else {
					final int normalLength = normalCEs.get(normalMinIndex).events().size();
					final int globalLength = globalCEs.get(globalMinIndex).events().size();
					// always add global
					addCounterexample(logger, size, globalCEs.get(globalMinIndex), globalNegativeForest);
					// add normal if it is not longer
					if (normalLength <= globalLength) {
						addCounterexample(logger, size, normalCEs.get(normalMinIndex), negativeForest);
					}
				}
			}
		}
		incr.halt();
		logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN");
		return Optional.empty();
	}
	
	private static Counterexample collapseLoop(Counterexample ce, int size) {
		final List<String> events = new ArrayList<>();
		final List<List<String>> actions = new ArrayList<>();
		final int length = ce.events().size();
		final int loopStart = length - ce.loopLength;
		events.addAll(ce.events().subList(0, loopStart));
		actions.addAll(ce.actions().subList(0, loopStart));
		for (int i = 0; i < size; i++) {
			events.addAll(ce.events().subList(loopStart, length));
			actions.addAll(ce.actions().subList(loopStart, length));
		}
		return new Counterexample(events, actions, 0);
	}
	
	protected static void addCounterexample(Logger logger, int size,
			Counterexample counterexample, NegativePlantScenarioForest negativeForest) {
		final List<MyBooleanExpression> expr = new ArrayList<>();
		for (int i = 0; i < counterexample.events().size(); i++) {
			expr.add(MyBooleanExpression.getTautology());
		}
		final List<StringActions> actions = counterexample.actions().stream().map(
				action -> new StringActions(String.join(",", action))
		).collect(Collectors.toList());
		negativeForest.addScenario(new StringScenario(true, counterexample.events(), expr, actions));
		logger.info("ADDING COUNTEREXAMPLE: " + counterexample);
	}
}
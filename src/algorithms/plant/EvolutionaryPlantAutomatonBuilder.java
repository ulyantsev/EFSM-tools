package algorithms.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import scenario.StringActions;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import verification.ltl.grammar.LtlNode;
import verification.verifier.Counterexample;
import verification.verifier.Verifier;
import algorithms.automaton_builders.ScenarioAndLtlAutomatonBuilder;

public class EvolutionaryPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static double fitness(NondetMooreAutomaton automaton, Verifier verifier) {
		final List<Counterexample> ces = verifier.verifyNondetMoore(automaton);
		double value = 0;
		for (Counterexample ce : ces) {
			if (!ce.isEmpty()) {
				value += 1. / ce.events().size();
			}
		}
		value /= ces.size();
		return value;
	}
	
	private static void evAlgo(NondetMooreAutomaton automaton, Verifier verifier,
			List<Pair<Integer, String>> changeableTransitions) {
		final Random rnd = new Random(25734);
		double bestFitness = fitness(automaton, verifier);
		final int stagnationCalls = 10000;
		
		int iteration = 0;
		int lastSuccessfulIteration = 0;
		System.out.println(">>> " + bestFitness);

		while (iteration - lastSuccessfulIteration < stagnationCalls && bestFitness > 0) {
			System.out.print(">>> iteration " + iteration + "; ");
			// mutation
			final int transitionIndex = rnd.nextInt(changeableTransitions.size());
			final Pair<Integer, String> transitionDescription = changeableTransitions.get(transitionIndex);
			final MooreNode state = automaton.state(transitionDescription.getLeft());
			final String event = transitionDescription.getRight();

			final List<MooreTransition> oldTransitions = state
					.transitions().stream()
					.filter(t -> t.event().equals(event))
					.collect(Collectors.toList());
			if (oldTransitions.size() > 1) {
				throw new AssertionError();
			}
			final MooreTransition oldTransition = oldTransitions.get(0);
			final int oldDst = oldTransition.dst().number();
			automaton.removeTransition(state, oldTransition);
			final int newDst = (oldDst + rnd.nextInt(automaton.stateCount() - 1) + 1)
					% automaton.stateCount();
			final MooreTransition newTransition = new MooreTransition(state,
					automaton.state(newDst), event);
			automaton.addTransition(state, newTransition);
			final double newFitness = fitness(automaton, verifier);
			System.out.println("t:" + transitionIndex + " s:" + state.number()
					+ " e:" + event + " nd:" + newDst + " nf:" + (float) newFitness);
			if (newFitness < bestFitness) {
				bestFitness = newFitness;
				lastSuccessfulIteration = iteration;
				System.out.println(">>> " + bestFitness);
			} else {
				automaton.removeTransition(state, newTransition);
				automaton.addTransition(state, oldTransition);
			}
			
			iteration++;
		}
		System.out.println(">>> Finished evolution");
	}
	
	public static Optional<NondetMooreAutomaton> build(Logger logger, PositivePlantScenarioForest positiveForest,
			String ltlFilePath, List<LtlNode> formulae, List<String> events,
			List<String> actions, Verifier verifier, long finishTime) throws IOException {
		
		final Map<StringActions, Set<MooreNode>> map = new LinkedHashMap<>();
		final Map<MooreNode, Integer> nodeToState = new HashMap<>();
		final Map<StringActions, Integer> actionsToState = new HashMap<>();
		final List<StringActions> stateToActions = new ArrayList<>();
		for (MooreNode node : positiveForest.nodes()) {
			Set<MooreNode> cluster = map.get(node.actions());
			if (cluster == null) {
				cluster = new LinkedHashSet<>();
				map.put(node.actions(), cluster);
				actionsToState.put(node.actions(), stateToActions.size());
				stateToActions.add(node.actions());
			}
			nodeToState.put(node, actionsToState.get(node.actions()));
			cluster.add(node);
		}
		final List<Boolean> isStart = new ArrayList<>();
		for (Map.Entry<StringActions, Set<MooreNode>> entry : map.entrySet()) {
			boolean initial = false;
			for (MooreNode root : positiveForest.roots()) {
				if (entry.getValue().contains(root)) {
					initial = true;
					break;
				}
			}
			isStart.add(initial);
		}
				
		// transitions from scenarios
		final NondetMooreAutomaton automaton = new NondetMooreAutomaton(map.size(),
				stateToActions, isStart);
		for (MooreNode node : positiveForest.nodes()) {
			final MooreNode sourceState = automaton.state(nodeToState.get(node));
			for (MooreTransition t : node.transitions()) {
				final MooreNode destState = automaton.state(nodeToState.get(t.dst()));
				if (sourceState.scenarioDst(t.event(), destState.actions()) == null) {
					sourceState.addTransition(t.event(), destState);
				}
			}
		}
		
		final List<Pair<Integer, String>> changeableTransitions = new ArrayList<>();

		// completion with loops
		for (MooreNode state : automaton.states()) {
			for (String event : events) {
				if (!state.hasTransition(event)) {
					state.addTransition(event, state);
					changeableTransitions.add(Pair.of(state.number(), event));
				}
			}
		}
		
		if (!changeableTransitions.isEmpty()) {
			evAlgo(automaton, verifier, changeableTransitions);
		}
		
		return Optional.of(automaton);
	}
}
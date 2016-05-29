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
import java.util.Set;

import scenario.StringActions;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import algorithms.automaton_builders.ScenarioAndLtlAutomatonBuilder;

public class RapidPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	public static Optional<NondetMooreAutomaton> build(PositivePlantScenarioForest positiveForest,
			List<String> events) throws IOException {
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
		// completion with loops
		for (MooreNode state : automaton.states()) {
			for (String event : events) {
				if (!state.hasTransition(event)) {
					final MooreTransition t = new MooreTransition(state, state, event);
					state.addTransition(t);
					automaton.addUnsupportedTransition(t);
				}
			}
		}
		return Optional.of(automaton);
	}
}
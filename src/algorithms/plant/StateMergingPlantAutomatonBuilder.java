package algorithms.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

import scenario.StringActions;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import algorithms.automaton_builders.ScenarioAndLtlAutomatonBuilder;
import egorov.ltl.grammar.LtlNode;
import egorov.verifier.Counterexample;
import egorov.verifier.Verifier;

public class StateMergingPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static NondetMooreAutomaton mergeMinimize(Random rnd,
			NondetMooreAutomaton currentPlant, Verifier verifier) {
		l: while (true) {
			final Map<StringActions, Set<MooreNode>> startClusters = new LinkedHashMap<>();
			for (MooreNode node : currentPlant.states()) {
				Set<MooreNode> cluster = startClusters.get(node.actions());
				if (cluster == null) {
					cluster = new LinkedHashSet<>();
					startClusters.put(node.actions(), cluster);
				}
				cluster.add(node);
			}
			
			final List<Set<MooreNode>> mergeableClusters = new ArrayList<>();
			for (Set<MooreNode> cluster : startClusters.values()) {
				if (cluster.size() > 1) {
					mergeableClusters.add(cluster);
				}
			}
			
			final List<Pair<MooreNode, MooreNode>> mergeablePairs = new ArrayList<>();
			for (int i = 0; i < mergeableClusters.size(); i++) {
				final Set<MooreNode> cluster = mergeableClusters.get(i);
				final List<MooreNode> clusterList = new ArrayList<>(cluster);

				for (int j = 0; j < cluster.size(); j++) {
					for (int k = j + 1; k < cluster.size(); k++) {
						mergeablePairs.add(Pair.of(clusterList.get(j), clusterList.get(k)));
					}
				}
			}
			Collections.shuffle(mergeablePairs, rnd);
			
			for (Pair<MooreNode, MooreNode> p : mergeablePairs) {
				final MooreNode x = p.getLeft();
				final MooreNode y = p.getRight();
				// TODO	implement dead end removal
				
				final NondetMooreAutomaton merged = currentPlant.merge(x, y);
				
				final boolean verified = verifier.verifyNondetMoore(merged).stream()
						.allMatch(Counterexample::isEmpty);
				if (verified) {
					currentPlant = merged;
					continue l;
				}
			}
			return currentPlant; // no more correct merges
		}
	}
	
	public static Optional<NondetMooreAutomaton> build(Logger logger, PositivePlantScenarioForest positiveForest,
			String ltlFilePath, List<LtlNode> formulae, List<String> events,
			List<String> actions, Verifier verifier, long finishTime) throws IOException {
		
		final List<MooreNode> sortedNodes = new ArrayList<>(positiveForest.nodes());
		Collections.sort(sortedNodes, (n1, n2) -> Integer.compare(n1.number(), n2.number()));
		final List<StringActions> initialActions = new ArrayList<>();
		final List<Boolean> isStart = new ArrayList<>();
		for (MooreNode node : sortedNodes) {
			initialActions.add(node.actions());
			isStart.add(positiveForest.roots().contains(node));
		}
		// transitions from scenarios
		final NondetMooreAutomaton forestAutomaton = new NondetMooreAutomaton(isStart.size(), initialActions, isStart);
		for (MooreNode node : positiveForest.nodes()) {
			final MooreNode sourceState = forestAutomaton.state(node.number());
			for (MooreTransition t : node.transitions()) {
				final MooreNode destState = forestAutomaton.state(t.dst().number());
				sourceState.addTransition(t.event(), destState);
			}
		}
		
		final Random rnd = new Random(435568);
		final int attempts = 1;
		final List<Pair<NondetMooreAutomaton, Boolean>> foundSolutions = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			final NondetMooreAutomaton candidate = mergeMinimize(rnd, forestAutomaton, verifier);
			foundSolutions.add(Pair.of(candidate, false));
			// TODO completion to match formulae
			
			// completion with loops
			for (MooreNode state : candidate.states()) {
				for (String event : events) {
					if (!state.hasTransition(event)) {
						state.addTransition(event, state);
					}
				}
			}
		}
		
		return Optional.of(foundSolutions.get(0).getLeft());
	}
}
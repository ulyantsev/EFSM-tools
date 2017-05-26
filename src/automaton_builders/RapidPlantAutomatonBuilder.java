package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import scenario.StringActions;
import structures.moore.MooreNode;
import structures.moore.MooreTransition;
import structures.moore.NondetMooreAutomaton;
import structures.moore.PositivePlantScenarioForest;

import java.io.IOException;
import java.util.*;

public class RapidPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    /*
     * positiveForest must be constructed with separatePaths
     */
    public static Optional<NondetMooreAutomaton> build(PositivePlantScenarioForest positiveForest,
            List<String> events, boolean timedConstraints) throws IOException {
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
        final List<Boolean> isInitial = new ArrayList<>();
        for (Map.Entry<StringActions, Set<MooreNode>> entry : map.entrySet()) {
            boolean initial = false;
            for (MooreNode root : positiveForest.roots()) {
                if (entry.getValue().contains(root)) {
                    initial = true;
                    break;
                }
            }
            isInitial.add(initial);
        }
        // transitions from scenarios
        final NondetMooreAutomaton automaton = new NondetMooreAutomaton(map.size(), stateToActions, isInitial);
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
                    state.addTransition(new MooreTransition(state, state, event));
                    // now handled by scenario compliance check
                    //automaton.unsupportedTransitions().add(t);
                }
            }
        }

        if (timedConstraints) {
            final Set<MooreNode> processedNodes = new HashSet<>();
            final Map<MooreNode, Integer> loopConstraints = new HashMap<>();
            for (MooreNode node : positiveForest.nodes()) {
                if (!processedNodes.add(node)) {
                    continue;
                }
                int limit = 0;
                MooreNode curNode = node;
                while (!curNode.transitions().isEmpty()) {
                    final MooreNode newNode = curNode.transitions().iterator().next().dst();
                    processedNodes.add(curNode);
                    if (newNode.actions().equals(curNode.actions())) {
                        curNode = newNode;
                        limit++;
                    } else {
                        break;
                    }
                }
                final MooreNode automatonState = automaton.state(nodeToState.get(node));
                Integer maxLimit = loopConstraints.get(automatonState);
                maxLimit = maxLimit == null ? limit : Math.max(limit, maxLimit);
                loopConstraints.put(automatonState, maxLimit);
            }
            automaton.setLoopConstraints(loopConstraints);
        }

        return Optional.of(automaton);
    }
}
package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.MooreNodeIterable;
import continuous_trace_builders.MooreNodeIterator;
import org.apache.commons.lang3.tuple.Pair;
import scenario.StringActions;
import structures.moore.MooreNode;
import structures.moore.MooreTransition;
import structures.moore.NondetMooreAutomaton;

import java.io.IOException;
import java.util.*;

public class RapidPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    /*
     * positiveForest must be constructed with separatePaths
     */
    public static Optional<NondetMooreAutomaton> build(MooreNodeIterable iterable, List<String> events,
                                                       boolean timedConstraints) throws IOException {
        System.out.println("Construction: initialization...");
        final Map<StringActions, Integer> actionsToState = new HashMap<>();
        final List<StringActions> stateToActions = new ArrayList<>();
        final List<Boolean> isInitial = new ArrayList<>();
        MooreNodeIterator it = iterable.nodeIterator();
        Pair<MooreNode, Boolean> p;
        while ((p = it.next()) != null) {
            final MooreNode node = p.getLeft();
            final boolean initial = p.getRight();

            if (!actionsToState.containsKey(node.actions())) {
                actionsToState.put(node.actions(), stateToActions.size());
                stateToActions.add(node.actions());
                isInitial.add(initial);
            } else if (initial) {
                isInitial.set(actionsToState.get(node.actions()), true);
            }
        }
        // transitions from scenarios
        final NondetMooreAutomaton automaton = new NondetMooreAutomaton(stateToActions.size(), stateToActions,
                isInitial);

        System.out.println("Construction: adding transitions...");
        it = iterable.nodeIterator();
        while ((p = it.next()) != null) {
            final MooreNode node = p.getLeft();
            final MooreNode sourceState = automaton.state(actionsToState.get(node.actions()));
            for (MooreTransition t : node.transitions()) {
                final MooreNode destState = automaton.state(actionsToState.get(t.dst().actions()));
                if (sourceState.scenarioDst(t.event(), destState.actions()) == null) {
                    sourceState.addTransition(t.event(), destState);
                }
            }
        }
        System.out.println("Construction: adding looping unsupported transitions...");
        // completion with loops
        for (MooreNode state : automaton.states()) {
            events.stream().filter(event -> !state.hasTransition(event)).forEach(event -> {
                final MooreTransition t = new MooreTransition(state, state, event);
                state.addTransition(t);
                automaton.unsupportedTransitions().add(t);
            });
        }

        if (timedConstraints) {
            System.out.println("Construction: timed constraints...");
            final Set<MooreNode> processedNodes = new HashSet<>();
            final Map<MooreNode, Integer> loopConstraints = new HashMap<>();
            it = iterable.nodeIterator();
            while ((p = it.next()) != null) {
                final MooreNode node = p.getLeft();
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
                final MooreNode automatonState = automaton.state(actionsToState.get(node.actions()));
                Integer maxLimit = loopConstraints.get(automatonState);
                maxLimit = maxLimit == null ? limit : Math.max(limit, maxLimit);
                loopConstraints.put(automatonState, maxLimit);
            }
            automaton.setLoopConstraints(loopConstraints);
        }

        return Optional.of(automaton);
    }
}
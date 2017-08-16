package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import continuous_trace_builders.MooreNodeIterable;
import continuous_trace_builders.MooreNodeIterator;
import scenario.StringActions;
import structures.moore.MooreNode;
import structures.moore.MooreTransition;
import structures.moore.NondetMooreAutomaton;

import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class RapidPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    private static class TransitionStub {
        private final int from;
        private final String event;
        private final int to;

        private TransitionStub(int from, String event, int to) {
            this.from = from;
            this.event = event;
            this.to = to;
        }
    }

    /*
     * positiveForest must be constructed with separatePaths
     */
    public static NondetMooreAutomaton build(MooreNodeIterable iterable, List<String> events,
                                                       boolean timedConstraints) throws IOException {
        System.out.println("Construction: main phase...");
        final Map<StringActions, Integer> actionsToState = new HashMap<>();
        final List<StringActions> stateToActions = new ArrayList<>();
        final List<Boolean> isInitial = new ArrayList<>();
        final List<Integer> loopConstraints = new ArrayList<>();
        final List<TransitionStub> transitionStubs = new ArrayList<>();

        final BiFunction<MooreNode, Boolean, Integer> processNode = (node, initial) -> {
            final StringActions actions = node.actions();
            Integer result = actionsToState.get(actions);
            if (result == null) {
                result = stateToActions.size();
                actionsToState.put(actions, result);
                stateToActions.add(actions);
                isInitial.add(initial);
                loopConstraints.add(null);
            } else if (initial) {
                isInitial.set(result, true);
            }
            return result;
        };

        final BiConsumer<Integer, Integer> updateLoopConstraints = (stateIndex, limit) -> {
            final Integer maxLimit = loopConstraints.get(stateIndex);
            if (maxLimit == null || limit > maxLimit) {
                loopConstraints.set(stateIndex, limit);
            }
        };

        MooreNodeIterator it = iterable.nodeIterator();
        MooreNode root;
        while ((root = it.next()) != null) {
            MooreNode curNode = root;
            int curValue = processNode.apply(root, true);
            int limit = 1;

            while (true) {
                final Collection<MooreTransition> c = curNode.transitions();
                if (c.isEmpty()) {
                    if (timedConstraints) {
                        updateLoopConstraints.accept(curValue, limit);
                    }
                    break;
                } else {
                    final MooreTransition t = c.iterator().next();
                    final MooreNode newNode = t.dst();
                    final int nextValue = processNode.apply(newNode, false);
                    transitionStubs.add(new TransitionStub(curValue, t.event(), nextValue));

                    if (timedConstraints) {
                        if (newNode.actions().equals(curNode.actions())) {
                            limit++;
                        } else {
                            updateLoopConstraints.accept(curValue, limit);
                            limit = 1;
                        }
                    }

                    curNode = newNode;
                    curValue = nextValue;
                }
            }
        }
        // transitions from scenarios
        final NondetMooreAutomaton automaton = new NondetMooreAutomaton(stateToActions.size(), stateToActions,
                isInitial);

        System.out.println("Construction: adding transitions...");
        for (TransitionStub stub : transitionStubs) {
            final MooreNode sourceState = automaton.state(stub.from);
            final MooreNode destState = automaton.state(stub.to);
            if (sourceState.scenarioDst(stub.event, destState.actions()) == null) {
                sourceState.addTransition(stub.event, destState);
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
            automaton.setLoopConstraints(loopConstraints);
        }

        return automaton;
    }
}
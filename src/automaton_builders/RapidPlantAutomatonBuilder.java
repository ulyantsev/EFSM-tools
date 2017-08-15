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
import java.util.function.BiFunction;

public class RapidPlantAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    /*
     * positiveForest must be constructed with separatePaths
     */
    public static NondetMooreAutomaton build(MooreNodeIterable iterable, List<String> events,
                                                       boolean timedConstraints) throws IOException {
        System.out.println("Construction: main phase...");
        final Map<StringActions, Integer> actionsToState = new HashMap<>();
        final List<StringActions> stateToActions = new ArrayList<>();
        final List<Boolean> isInitial = new ArrayList<>();
        MooreNodeIterator it = iterable.nodeIterator();
        MooreNode root;

        final BiFunction<MooreNode, Boolean, Integer> processNode = (node, initial) -> {
            final StringActions actions = node.actions();
            Integer result = actionsToState.get(actions);
            if (result == null) {
                result = stateToActions.size();
                actionsToState.put(actions, result);
                stateToActions.add(actions);
                isInitial.add(initial);
            } else if (initial) {
                isInitial.set(result, true);
            }
            return result;
        };

        class TransitionStub {
            private final int from;
            private final String event;
            private final int to;

            private TransitionStub(int from, String event, int to) {
                this.from = from;
                this.event = event;
                this.to = to;
            }
        }

        final List<TransitionStub> transitionStubs = new ArrayList<>();

        while ((root = it.next()) != null) {
            MooreNode node = root;
            int curValue = processNode.apply(root, true);

            while (true) {
                final Collection<MooreTransition> c = node.transitions();
                if (c.isEmpty()) {
                    break;
                } else {
                    final MooreTransition t = c.iterator().next();
                    final MooreNode newNode = t.dst();
                    final int nextValue = processNode.apply(newNode, false);
                    transitionStubs.add(new TransitionStub(curValue, t.event(), nextValue));
                    node = newNode;
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
            System.out.println("Construction: timed constraints...");
            final Map<MooreNode, Integer> loopConstraints = new HashMap<>();
            it = iterable.nodeIterator();
            while ((root = it.next()) != null) {
                MooreNode curNode = root;
                int limit = 1;
                do {
                    final MooreNode newNode = curNode.transitions().isEmpty() ? null
                            : curNode.transitions().iterator().next().dst();
                    if (newNode != null && newNode.actions().equals(curNode.actions())) {
                        limit++;
                    } else {
                        final MooreNode automatonState = automaton.state(actionsToState.get(curNode.actions()));
                        final Integer maxLimit = loopConstraints.get(automatonState);
                        if (maxLimit == null || limit > maxLimit) {
                            loopConstraints.put(automatonState, limit);
                        }
                        limit = 1;
                    }
                    curNode = newNode;
                } while (curNode != null);
            }
            automaton.setLoopConstraints(loopConstraints);
        }

        return automaton;
    }
}
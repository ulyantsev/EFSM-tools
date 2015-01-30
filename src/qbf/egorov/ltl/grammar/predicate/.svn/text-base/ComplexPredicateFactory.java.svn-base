/**
 * ComplexPredicateUtils.java, 02.05.2008
 */
package ru.ifmo.ltl.grammar.predicate;

import ru.ifmo.verifier.automata.statemachine.ComplexState;
import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.automata.statemachine.IStateMachine;
import ru.ifmo.ltl.grammar.predicate.annotation.Predicate;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class ComplexPredicateFactory extends PredicateFactory<ComplexState> {

    @Predicate
    public Boolean isInState(IStateMachine<? extends IState> a, IState s) {
        if (!wasTransition()) {
            return null;
        }
        ComplexState cs = (ComplexState) transition.getTarget();
        return cs.getStateMachineState(a).equals(s);
    }

    @Predicate
    public Boolean wasInState(IStateMachine<? extends IState> a, IState s) {
        return (wasTransition()) ? state.getStateMachineState(a).equals(s): null;
    }
}

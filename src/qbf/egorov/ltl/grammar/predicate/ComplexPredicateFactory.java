/**
 * ComplexPredicateUtils.java, 02.05.2008
 */
package qbf.egorov.ltl.grammar.predicate;

import qbf.egorov.ltl.grammar.predicate.annotation.Predicate;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;
import qbf.egorov.verifier.automata.statemachine.ComplexState;

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

/**
 * IPredicateFactory.java, 10.05.2008
 */
package qbf.egorov.ltl.grammar.predicate;

import qbf.egorov.ltl.grammar.predicate.annotation.Predicate;
import qbf.egorov.statemachine.Action;
import qbf.egorov.statemachine.Event;
import qbf.egorov.statemachine.SimpleState;
import qbf.egorov.statemachine.StateMachine;
import qbf.egorov.statemachine.StateTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IPredicateFactory {
    void setAutomataState(SimpleState state, StateTransition transition);

    @Predicate
    Boolean wasEvent(Event e);

    @Predicate
    Boolean isInState(StateMachine a, SimpleState s);

    @Predicate
    Boolean wasInState(StateMachine a, SimpleState s);

    @Predicate
    boolean cameToFinalState();

    @Predicate
    Boolean wasAction(Action z);

    @Predicate
    Boolean wasFirstAction(Action z);
}

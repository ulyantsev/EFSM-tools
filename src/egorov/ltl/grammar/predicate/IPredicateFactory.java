/**
 * IPredicateFactory.java, 10.05.2008
 */
package egorov.ltl.grammar.predicate;

import egorov.ltl.grammar.predicate.annotation.Predicate;
import egorov.statemachine.Action;
import egorov.statemachine.Event;
import egorov.statemachine.SimpleState;
import egorov.statemachine.StateMachine;
import egorov.statemachine.StateTransition;

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

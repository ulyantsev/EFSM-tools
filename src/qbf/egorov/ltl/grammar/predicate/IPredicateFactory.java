/**
 * IPredicateFactory.java, 10.05.2008
 */
package qbf.egorov.ltl.grammar.predicate;

import qbf.egorov.ltl.grammar.predicate.annotation.Predicate;
import qbf.egorov.statemachine.*;
import qbf.egorov.statemachine.impl.Action;
import qbf.egorov.statemachine.impl.Event;
import qbf.egorov.statemachine.impl.StateMachine;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IPredicateFactory<S extends IState> {
    void setAutomataState(S state, IStateTransition transition);

    @Predicate
    Boolean wasEvent(Event e);

    @Predicate
    Boolean isInState(StateMachine<? extends IState> a, IState s);

    @Predicate
    Boolean wasInState(StateMachine<? extends IState> a, IState s);

    @Predicate
    boolean cameToFinalState();

    @Predicate
    Boolean wasAction(Action z);

    @Predicate
    Boolean wasFirstAction(Action z);

    @Predicate
    boolean wasTrue(ICondition cond);

    @Predicate
    boolean wasFalse(ICondition cond);
}

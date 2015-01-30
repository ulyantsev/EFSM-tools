/**
 * IPredicateFactory.java, 10.05.2008
 */
package ru.ifmo.ltl.grammar.predicate;

import ru.ifmo.automata.statemachine.*;
import ru.ifmo.ltl.grammar.predicate.annotation.Predicate;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IPredicateFactory<S extends IState> {

    void setAutomataState(S state, IStateTransition transition);

    @Predicate
    Boolean wasEvent(IEvent e);

    @Predicate
    Boolean isInState(IStateMachine<? extends IState> a, IState s);

    @Predicate
    Boolean wasInState(IStateMachine<? extends IState> a, IState s);

    @Predicate
    boolean cameToFinalState();

    @Predicate
    Boolean wasAction(IAction z);

    @Predicate
    Boolean wasFirstAction(IAction z);

    @Predicate
    boolean wasTrue(ICondition cond);

    @Predicate
    boolean wasFalse(ICondition cond);
}

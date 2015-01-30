/**
 * IPredicateUtils.java, 12.04.2008
 */
package ru.ifmo.ltl.grammar.predicate;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.automata.statemachine.IStateTransition;

/**
 * Predicate provider.
 * Use @Predicate annotation, to mark method as predicate;
 *
 * @author Kirill Egorov
 */
public abstract class AbstractPredicateFactory<S extends IState> implements IPredicateFactory<S>, Cloneable {

    protected S state;
    protected IStateTransition transition;

    /**
     * To check predicate in transition.getTarget() state.
     * @param state previous state
     * @param transition transition from state to transition.getTarget()
     */
    public void setAutomataState(S state, IStateTransition transition) {
        this.state = state;
        this.transition = transition;
    }

    public AbstractPredicateFactory<S> clone() throws CloneNotSupportedException {
        return (AbstractPredicateFactory<S>) super.clone();
    }

    protected boolean wasTransition() {
        return !(transition.getEvent() == null && transition.getCondition() == null
                && transition.getTarget() == state && (state.getOutcomingTransitions().size() > 1));
    }
}

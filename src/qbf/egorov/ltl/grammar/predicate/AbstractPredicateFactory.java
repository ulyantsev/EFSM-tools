/**
 * IPredicateUtils.java, 12.04.2008
 */
package qbf.egorov.ltl.grammar.predicate;

import qbf.egorov.statemachine.SimpleState;
import qbf.egorov.statemachine.StateTransition;

/**
 * Predicate provider.
 * Use @Predicate annotation, to mark method as predicate;
 *
 * @author Kirill Egorov
 */
public abstract class AbstractPredicateFactory implements IPredicateFactory, Cloneable {
    protected SimpleState state;
    protected StateTransition transition;

    /**
     * To check predicate in transition.getTarget() state.
     * @param state previous state
     * @param transition transition from state to transition.getTarget()
     */
    public void setAutomataState(SimpleState state, StateTransition transition) {
        this.state = state;
        this.transition = transition;
    }

	public AbstractPredicateFactory clone() throws CloneNotSupportedException {
        return (AbstractPredicateFactory) super.clone();
    }

    protected boolean wasTransition() {
        return !(transition.event == null
                && transition.getTarget() == state && state.getOutcomingTransitions().size() > 1);
    }
}

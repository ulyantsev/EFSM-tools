/**
 * PredicateUtils.java, 12.03.2008
 */
package egorov.ltl.grammar;

import egorov.ltl.grammar.annotation.Predicate;
import egorov.statemachine.SimpleState;
import egorov.statemachine.StateTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class PredicateFactory {
	private SimpleState state;
	private StateTransition transition;

    /**
     * To check predicate in transition.getTarget() state.
     * @param state previous state
     * @param transition transition from state to transition.getTarget()
     */
    public void setAutomataState(SimpleState state, StateTransition transition) {
        this.state = state;
        this.transition = transition;
    }

    private boolean wasTransition() {
        return !(transition.event == null
                && transition.getTarget() == state && state.getOutcomingTransitions().size() > 1);
    }
    
	@Predicate
    public Boolean wasEvent(String e) {
        return (wasTransition()) ? e.equals(transition.event) : null;
    }

    @Predicate
    public Boolean wasAction(String z) {
        return (wasTransition())
                ? transition.getActions().contains(z) || transition.getTarget().getActions().contains(z)
                : null;
    }
}

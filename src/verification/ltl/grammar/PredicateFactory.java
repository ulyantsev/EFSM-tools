/**
 * PredicateUtils.java, 12.03.2008
 */
package verification.ltl.grammar;

import verification.ltl.grammar.annotation.Predicate;
import verification.statemachine.SimpleState;
import verification.statemachine.StateTransition;

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
                && transition.getTarget() == state && state.outgoingTransitions().size() > 1);
    }
    
	@Predicate
    public Boolean event(String e) {
        return wasTransition() ? e.equals(transition.event) : null;
    }

    @Predicate
    public Boolean action(String z) {
        return wasTransition() ? transition.getActions().contains(z) : null;
    }
}

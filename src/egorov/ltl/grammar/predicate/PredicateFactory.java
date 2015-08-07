/**
 * PredicateUtils.java, 12.03.2008
 */
package egorov.ltl.grammar.predicate;

import egorov.ltl.grammar.predicate.annotation.Predicate;
import egorov.statemachine.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class PredicateFactory extends AbstractPredicateFactory {
    @Predicate
    public Boolean wasEvent(Event e) {
        return (wasTransition()) ? e.equals(transition.event) : null;
    }

    @Predicate
    public Boolean isInState(StateMachine a, SimpleState s) {
        return (wasTransition()) ? transition.getTarget().equals(s) : null;
    }

    @Predicate
    public Boolean wasInState(StateMachine a, SimpleState s) {
        return (wasTransition()) ? state.equals(s): null;
    }

    @Predicate
    public boolean cameToFinalState() {
        return transition.getTarget().isTerminal();
    }

    @Predicate
    public Boolean wasAction(Action z) {
        return (wasTransition())
                ? transition.getActions().contains(z) || transition.getTarget().getActions().contains(z)
                : null;
    }

    @Predicate
    public Boolean wasFirstAction(Action z) {
        if (!wasTransition()) {
            return null;
        }
        if (transition.getActions().isEmpty()) {
            return false;
        } else {
            return transition.getActions().get(0).equals(z);
        }
    }
}

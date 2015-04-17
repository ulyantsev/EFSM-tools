/**
 * PredicateUtils.java, 12.03.2008
 */
package qbf.egorov.ltl.grammar.predicate;

import qbf.egorov.ltl.grammar.predicate.annotation.Predicate;
import qbf.egorov.statemachine.*;
import qbf.egorov.statemachine.impl.Event;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class PredicateFactory<S extends IState> extends AbstractPredicateFactory<S> {

    @Predicate
    public Boolean wasEvent(Event e) {
        return (wasTransition()) ? e.equals(transition.getEvent()) : null;
    }

    @Predicate
    public Boolean isInState(IStateMachine<? extends IState> a, IState s) {
        return (wasTransition()) ? transition.getTarget().equals(s) : null;
    }

    @Predicate
    public Boolean wasInState(IStateMachine<? extends IState> a, IState s) {
        return (wasTransition()) ? state.equals(s): null;
    }

    @Predicate
    public boolean cameToFinalState() {
        return transition.getTarget().isTerminal();
    }

    @Predicate
    public Boolean wasAction(IAction z) {
        return (wasTransition())
                ? transition.getActions().contains(z) || transition.getTarget().getActions().contains(z)
                : null;
    }

    @Predicate
    public Boolean wasFirstAction(IAction z) {
        if (!wasTransition()) {
            return null;
        }
        if (transition.getActions().isEmpty()) {
            return false;
        } else {
            return transition.getActions().get(0).equals(z);
        }
    }

    @Predicate
    public boolean wasTrue(ICondition cond) {
    	throw new AssertionError();
    }

    @Predicate
    public boolean wasFalse(ICondition cond) {
    	throw new AssertionError();
    }
}

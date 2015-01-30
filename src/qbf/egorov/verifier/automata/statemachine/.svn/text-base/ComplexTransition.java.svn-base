/**
 * ComplexTransition.java, 28.04.2008
 */
package ru.ifmo.verifier.automata.statemachine;

import ru.ifmo.automata.statemachine.*;

import java.util.List;

/**
 * The transition from ComplexState to ComplexState
 *
 * @author Kirill Egorov
 */
public class ComplexTransition implements IStateTransition {
    private IStateTransition transition;
    private ComplexState target;

    public ComplexTransition(IStateTransition transition, ComplexState target) {
        if (transition == null) {
            throw new IllegalArgumentException("transition can't be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("ComplexState target can't be null");
        }
        this.transition = transition;
        this.target = target;
    }

    public ComplexState getTarget() {
        target.setActiveState(transition.getTarget());
        return target;
    }

    public IEvent getEvent() {
        return transition.getEvent();
    }

    public List<IAction> getActions() {
        return transition.getActions();
    }

    public ICondition getCondition() {
        return transition.getCondition();
    }
}

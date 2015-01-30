/**
 * Transition.java, 02.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.*;
import ru.ifmo.automata.statemachine.ICondition;
import ru.ifmo.automata.statemachine.IEvent;

import java.util.List;
import java.util.ArrayList;

/**
 * The IStateTransition implementation
 *
 * @author Kirill Egorov
 */
public class Transition implements IStateTransition {
    private IEvent event;
    private List<IAction> actions = new ArrayList<IAction>();
    private ICondition condition;
    private IState target;

    public Transition(IEvent event, ICondition condition, IState target) {
        this.event = event;
        this.condition = condition;
        this.target = target;
    }

    public IEvent getEvent() {
        return event;
    }

    public List<IAction> getActions() {
        return actions;
    }

    public ICondition getCondition() {
        return condition;
    }

    public IState getTarget() {
        return target;
    }

    public void addAction(IAction a) {
        actions.add(a);
    }

    public void setActions(List<IAction> actions) {
        this.actions = actions;
    }
}

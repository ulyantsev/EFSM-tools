/**
 * Transition.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.*;

import java.util.List;
import java.util.ArrayList;

/**
 * The IStateTransition implementation
 *
 * @author Kirill Egorov
 */
public class Transition implements IStateTransition {
    private Event event;
    private List<IAction> actions = new ArrayList<IAction>();
    private ICondition condition;
    private IState target;

    public Transition(Event event, ICondition condition, IState target) {
        this.event = event;
        this.condition = condition;
        this.target = target;
    }

    public Event getEvent() {
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

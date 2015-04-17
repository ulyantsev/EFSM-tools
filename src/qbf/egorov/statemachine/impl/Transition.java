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
    private List<Action> actions = new ArrayList<>();
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

    public List<Action> getActions() {
        return actions;
    }

    public ICondition getCondition() {
        return condition;
    }

    public IState getTarget() {
        return target;
    }

    public void addAction(Action a) {
        actions.add(a);
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }
}

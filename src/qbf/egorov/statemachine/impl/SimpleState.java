/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.*;

import java.util.*;

/**
 * IState implementation whithout nested state machines
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class SimpleState implements IState {
    private String name;
    private StateType type;
    private List<Action> actions;
    private Collection<IStateTransition> outTransitions = new ArrayList<>();

    public SimpleState(String name, StateType type, List<Action> actions) {
        this.name = name;
        this.type = type;
        this.actions = actions;

        outTransitions.add(new Transition(null, null, this));
    }

    public String getName() {
        return name;
    }

    public StateType getType() {
        return type;
    }

    public List<Action> getActions() {
        return actions;
    }

    public boolean isTerminal() {
        return type == StateType.FINAL;
    }

    public Collection<IStateTransition> getOutcomingTransitions() {
        return outTransitions;
    }

    public String getUniqueName() {
        return name + '@' + Integer.toHexString(super.hashCode());
    }

    public void addOutcomingTransition(IStateTransition t) {
        outTransitions.add(t);
    }

    @Override
    public String toString() {
        return name;
    }
}

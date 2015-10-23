/* 
 * Developed by eVelopers Corporation, 2009
 */
package egorov.statemachine;

import java.util.ArrayList;
import java.util.List;

import egorov.automata.INode;

/**
 * IState implementation whithout nested state machines
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class SimpleState implements INode<StateTransition> {
    public final String name;
    public final StateType type;
    private final List<String> actions;
    private final List<StateTransition> outTransitions = new ArrayList<>();

    public SimpleState(String name, StateType type, List<String> actions) {
        this.name = name;
        this.type = type;
        this.actions = actions;

        outTransitions.add(new StateTransition(null, this));
    }

    public List<String> getActions() {
        return actions;
    }

    public boolean isTerminal() {
        return type == StateType.FINAL;
    }

    public List<StateTransition> getOutcomingTransitions() {
        return outTransitions;
    }

    public String getUniqueName() {
        return name + '@' + Integer.toHexString(super.hashCode());
    }

    public void addOutcomingTransition(StateTransition t) {
        outTransitions.add(t);
    }

    @Override
    public String toString() {
        return name;
    }
}

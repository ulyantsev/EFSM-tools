/* 
 * Developed by eVelopers Corporation, 2009
 */
package egorov.statemachine;

import java.util.ArrayList;
import java.util.List;

/**
 * IState implementation without nested state machines
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class SimpleState {
    public final String name;
    public final boolean isInitial;
    private final List<String> actions;
    private final List<StateTransition> outTransitions = new ArrayList<>();

    public SimpleState(String name, boolean isInitial, List<String> actions) {
        this.name = name;
        this.isInitial = isInitial;
        this.actions = actions;

        outTransitions.add(new StateTransition(null, this));
    }

    public List<String> getActions() {
        return actions;
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

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
    private final List<StateTransition> outTransitions = new ArrayList<>();

    public SimpleState(String name, boolean isInitial) {
        this.name = name;
        this.isInitial = isInitial;
        
        // TODO seems that this line can be removed:
        outTransitions.add(new StateTransition(null, this));
    }

    public List<StateTransition> outgoingTransitions() {
        return outTransitions;
    }

    public String getUniqueName() {
        return name + '@' + Integer.toHexString(super.hashCode());
    }

    public void addOutgoingTransition(StateTransition t) {
        outTransitions.add(t);
    }

    @Override
    public String toString() {
        return name;
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.verifier;

import qbf.egorov.statemachine.ControlledObject;
import qbf.egorov.statemachine.EventProvider;
import qbf.egorov.statemachine.StateMachine;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class AutomataContext {
    private final ControlledObject co;
    private final EventProvider ep;
    private StateMachine machine;

    public AutomataContext(ControlledObject co, EventProvider ep) {
        this.co = co;
        this.ep = ep;
    }

    public ControlledObject getControlledObject() {
        return co;
    }

    public EventProvider getEventProvider() {
        return ep;
    }

    public StateMachine getStateMachine() {
        return machine;
    }

    public void setStateMachine(StateMachine machine) {
        this.machine = machine;
    }
}

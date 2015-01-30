/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.verifier;

import qbf.egorov.statemachine.IAutomataContext;
import qbf.egorov.statemachine.IControlledObject;
import qbf.egorov.statemachine.IEventProvider;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class ModifiableAutomataContext implements IAutomataContext {
    private IControlledObject co;
    private IEventProvider ep;
    private IStateMachine<IState> machine;

    public ModifiableAutomataContext(IControlledObject co, IEventProvider ep) {
        this.co = co;
        this.ep = ep;
    }

    public IControlledObject getControlledObject(String name) {
        return co;
    }

    public IEventProvider getEventProvider(String name) {
        return ep;
    }

    public IStateMachine<? extends IState> getStateMachine(String name) {
        return machine;
    }

    public void setStateMachine(IStateMachine<IState> machine) {
        this.machine = machine;
    }
}

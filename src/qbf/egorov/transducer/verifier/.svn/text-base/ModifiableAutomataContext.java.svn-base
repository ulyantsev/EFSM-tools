/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.ctddev.genetic.transducer.verifier;

import ru.ifmo.automata.statemachine.*;

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

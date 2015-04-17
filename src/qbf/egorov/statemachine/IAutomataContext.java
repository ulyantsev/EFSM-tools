/**
 * IAutomataContext.java, 13.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.statemachine.impl.StateMachine;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IAutomataContext {
    IControlledObject getControlledObject(String name);
    IEventProvider getEventProvider(String name);
    StateMachine<? extends IState> getStateMachine(String name);
}

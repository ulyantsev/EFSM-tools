/**
 * IAutomataContext.java, 13.03.2008
 */
package qbf.egorov.statemachine;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IAutomataContext {
    IControlledObject getControlledObject(String name);
    IEventProvider getEventProvider(String name);
    IStateMachine<? extends IState> getStateMachine(String name);
}

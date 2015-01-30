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

    public IControlledObject getControlledObject(String name);

    public IEventProvider getEventProvider(String name);

    public IStateMachine<? extends IState> getStateMachine(String name);
}

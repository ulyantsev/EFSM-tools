/**
 * IEventProvider.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.statemachine.IEvent;

import java.util.Collection;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IEventProvider {

    public String getName();

    public IEvent getEvent(String eventName);

    public Collection<IEvent> getEvents();

//    public Collection<IStateMachine> getTargets();

    public Class getImplClass();
}

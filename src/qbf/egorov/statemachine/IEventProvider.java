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
    String getName();
    IEvent getEvent(String eventName);
    Collection<IEvent> getEvents();
    Class getImplClass();
}

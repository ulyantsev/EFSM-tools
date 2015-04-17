/**
 * IEventProvider.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.statemachine.impl.Event;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IEventProvider {
    Event getEvent(String eventName);
}

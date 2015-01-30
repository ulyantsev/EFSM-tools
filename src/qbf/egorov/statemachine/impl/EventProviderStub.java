/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.IEvent;
import qbf.egorov.statemachine.IEventProvider;

import java.util.*;

/**
 * @author kegorov
 *         Date: Jun 17, 2009
 */
public class EventProviderStub implements IEventProvider {
    private String name;
    private Map<String, IEvent> events = new HashMap<String, IEvent>();

    public EventProviderStub(String name, String ... events) {
        this(name, Arrays.asList(events));
    }

    public EventProviderStub(String name, Collection<String> events) {
        this.name = name;

        for (String e: events) {
            this.events.put(e, new Event(e, null));
        }
    }

    public String getName() {
        return name;
    }

    public IEvent getEvent(String eventName) {
        return events.get(eventName);
    }

    public Collection<IEvent> getEvents() {
        return Collections.unmodifiableCollection(events.values());
    }

    public Class getImplClass() {
        return null;
    }
}

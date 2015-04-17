/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import qbf.egorov.statemachine.IEventProvider;

/**
 * @author kegorov
 *         Date: Jun 17, 2009
 */
public class EventProviderStub implements IEventProvider {
    private String name;
    private Map<String, Event> events = new HashMap<>();

    public EventProviderStub(String name, String ... events) {
        this(name, Arrays.asList(events));
    }

    private EventProviderStub(String name, Collection<String> events) {
        this.name = name;

        for (String e: events) {
            this.events.put(e, new Event(e));
        }
    }

    public String getName() {
        return name;
    }

    public Event getEvent(String eventName) {
        return events.get(eventName);
    }

    public Collection<Event> getEvents() {
        return Collections.unmodifiableCollection(events.values());
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kegorov
 *         Date: Jun 17, 2009
 */
public class EventProvider {
    private final Map<String, Event> events = new HashMap<>();

    public EventProvider(String... events) {
        for (String e: events) {
            this.events.put(e, new Event(e));
        }
    }

    public Event getEvent(String eventName) {
        return events.get(eventName);
    }
}

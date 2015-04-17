/**
 * EventProvider.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import qbf.egorov.statemachine.IEventProvider;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class EventProvider implements IEventProvider {
    private Map<String, Event> events;

    protected void findEvents(Class<?> clazz) {
        events = new HashMap<>();
        for (Field f: clazz.getFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
                try {
                    Object o = f.get(null);
                    if (o instanceof String) {
                        String name = (String) o;
                        events.put(name, new Event(name));
                    }
                } catch (IllegalAccessException e) {
                    //
                }
            }
        }
    }

    public Event getEvent(String eventName) {
        return events.get(eventName);
    }
}

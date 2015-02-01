/**
 * EventProvider.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.IEvent;
import qbf.egorov.statemachine.IEventProvider;

import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class EventProvider implements IEventProvider {
    private String name;
    private Map<String, IEvent> events;
    private Class implClass;

    public EventProvider(String name, Class implClass) {
        this.name = name;
        this.implClass = implClass;
        findEvents(implClass);
    }

    protected void findEvents(Class clazz) {
        events = new HashMap<>();
        for (Field f: clazz.getFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) && Modifier.isPublic(mod) && Modifier.isFinal(mod)) {
                try {
                    Object o = f.get(null);
                    if (o instanceof String) {
                        String name = (String) o;
                        events.put(name, new Event(name, null));
                    }
                } catch (IllegalAccessException e) {
                    //
                }
            }
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
        return implClass;
    }
}

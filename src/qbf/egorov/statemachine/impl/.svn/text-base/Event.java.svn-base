/**
 * Event.java, 02.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.IEvent;
import org.apache.commons.lang.StringUtils;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Event implements IEvent {
    private String name;
    private String description;

    protected Event(String name, String description) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Event name can't be null or blank");
        }
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action)) return false;

        Event event = (Event) o;

        return name.equals(event.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return name;
    }
}

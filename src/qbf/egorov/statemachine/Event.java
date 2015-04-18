/**
 * Event.java, 02.03.2008
 */
package qbf.egorov.statemachine;

import org.apache.commons.lang3.StringUtils;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Event {
    private final String name;

    Event(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Event name can't be null or blank");
        }
        this.name = name;
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

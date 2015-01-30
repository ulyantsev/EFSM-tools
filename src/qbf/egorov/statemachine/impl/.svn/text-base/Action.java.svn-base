/**
 * Action.java, 02.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.IAction;
import org.apache.commons.lang.StringUtils;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Action implements IAction {
    private String name;
    private String description;

    protected Action(String name, String description) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Action name can't be null or blank");
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

        Action action = (Action) o;

        return name.equals(action.name);
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return name;
    }
}

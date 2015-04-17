/**
 * Action.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import org.apache.commons.lang3.StringUtils;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Action {
    private String name;
    
    protected Action(String name) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Action name can't be null or blank");
        }
        this.name = name;
    }

    public String getName() {
        return name;
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

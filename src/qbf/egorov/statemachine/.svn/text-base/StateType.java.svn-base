/**
 * StateType.java, 01.03.2008
 */
package ru.ifmo.automata.statemachine;

import org.apache.commons.lang.StringUtils;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public enum StateType {
    NORMAL("NORMAL"),
    INITIAL("INITIAL"),
    FINAL("FINAL");

    private String name;

    private StateType(String name) {
        this.name = name;
    }

    public static StateType getByName(String name) {
        for (StateType type: StateType.values()) {
            if (StringUtils.equalsIgnoreCase(type.name, name)) {
                return type;
            }
        }
        return null;
    }
}

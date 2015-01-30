/**
 * Node.java, 16.03.2008
 */
package ru.ifmo.ltl.buchi.impl;

import ru.ifmo.ltl.buchi.IBuchiNode;
import ru.ifmo.ltl.buchi.ITransitionCondition;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;

/**
 * A Buchi automata node implementation
 *
 * @author Kirill Egorov
 */
public class BuchiNode implements IBuchiNode {
    private int id;
    private Map<ITransitionCondition, IBuchiNode> transitions =
            new HashMap<ITransitionCondition, IBuchiNode>();

    public BuchiNode(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public IBuchiNode getNext() {
        for (ITransitionCondition cond: transitions.keySet()) {
            if (cond.getValue()) {
                return transitions.get(cond);
            }
        }
        return null;
    }

    public Map<ITransitionCondition, IBuchiNode> getTransitions() {
        return Collections.unmodifiableMap(transitions);
    }

    public void addTransition(ITransitionCondition condition, IBuchiNode next) {
        transitions.put(condition, next);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BuchiNode)) {
            return false;
        }

        return id == ((BuchiNode) o).id;

    }

    public int hashCode() {
        return id;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("BuchiNode %d\n", id));
        for (Map.Entry<ITransitionCondition, IBuchiNode> entry: transitions.entrySet()) {
            buf.append(String.format("\t-->[%s] %d\n", entry.getKey(), entry.getValue().getID()));
        }
        return buf.toString();
    }

}

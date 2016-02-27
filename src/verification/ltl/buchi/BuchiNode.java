/**
 * Node.java, 16.03.2008
 */
package verification.ltl.buchi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Buchi automata node implementation
 *
 * @author Kirill Egorov
 */
public class BuchiNode {
    private final int id;
    private final Map<TransitionCondition, BuchiNode> transitions = new LinkedHashMap<>();

    public BuchiNode(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public Map<TransitionCondition, BuchiNode> getTransitions() {
        return Collections.unmodifiableMap(transitions);
    }

    public void addTransition(TransitionCondition condition, BuchiNode next) {
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
        for (Map.Entry<TransitionCondition, BuchiNode> entry: transitions.entrySet()) {
            buf.append(String.format("\t-->[%s] %d\n", entry.getKey(), entry.getValue().getID()));
        }
        return buf.toString();
    }

}

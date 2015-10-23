/**
 * BuchiAutomata.java, 16.03.2008
 */
package egorov.ltl.buchi;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class BuchiAutomaton {
    private BuchiNode startNode;
    private final Set<BuchiNode> nodes = new LinkedHashSet<>();
    private Set<BuchiNode> acceptSet;

    public BuchiNode startNode() {
        return startNode;
    }

    public void setStartNode(BuchiNode startNode) {
        this.startNode = startNode;
    }

    public Set<BuchiNode> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public Set<BuchiNode> acceptSet() {
        return acceptSet;
    }

    public void setAcceptSet(Set<BuchiNode> acceptSet) {
        this.acceptSet = acceptSet;
    }

    public int size() {
        return nodes.size();
    }

    public void addNode(BuchiNode node) {
        nodes.add(node);
    }

    @Override
    public String toString() {
        return "Initial state: " + startNode.getID() + "\n"
        		+ String.join("", nodes.stream()
        				.map(Object::toString).collect(Collectors.toList()))
        		+ "Accepting states: ["
        		+ String.join(", ", acceptSet.stream()
        				.map(node -> String.valueOf(node.getID())).collect(Collectors.toList())
        		+ "]\n");
    }
}

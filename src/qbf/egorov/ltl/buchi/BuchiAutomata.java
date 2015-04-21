/**
 * BuchiAutomata.java, 16.03.2008
 */
package qbf.egorov.ltl.buchi;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class BuchiAutomata {
    private BuchiNode startNode;
    private final Set<BuchiNode> nodes = new LinkedHashSet<>();
    private final Map<Integer, Set<BuchiNode>> accept = new LinkedHashMap<>();
    private int acceptSetsCount = 0;

    public BuchiNode getStartNode() {
        return startNode;
    }

    public void setStartNode(BuchiNode startNode) {
        this.startNode = startNode;
    }

    public Set<BuchiNode> getNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public Set<BuchiNode> getAcceptSet(int i) {
        if (i < 0 || i >= acceptSetsCount) {
            throw new IndexOutOfBoundsException("Should be 0 <= i < acceptSetsCount");
        }
        return accept.get(i);
    }

    public int getAcceptSetsCount() {
        return acceptSetsCount;
    }

    public void addAcceptSet(Set<BuchiNode> acceptSet) {
        if (acceptSet == null || acceptSet.isEmpty()) {
            throw new IllegalArgumentException("acceptSet can't be null or emty");
        }
        accept.put(acceptSetsCount++, acceptSet);
    }

    public int size() {
        return nodes.size();
    }

    public void addNode(BuchiNode node) {
        nodes.add(node);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        buf.append("initial ");
        buf.append(startNode.getID());
        buf.append("\n");
        for (BuchiNode n: nodes) {
            buf.append(n);
        }
        for (Map.Entry<Integer, Set<BuchiNode>> entry: accept.entrySet()) {
            buf.append(String.format("Accept set %d [", entry.getKey()));
            for (BuchiNode node: entry.getValue()) {
                buf.append(node.getID()).append(", ");
            }
            buf.replace(buf.length() - 2, buf.length(), "]\n");
        }
        return buf.toString();
    }
}

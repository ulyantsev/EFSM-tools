/**
 * SecondDfs.java, 09.05.2008
 */
package qbf.egorov.verifier;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import qbf.egorov.util.DequeSet;
import qbf.egorov.verifier.automata.IntersectionNode;
import qbf.egorov.verifier.automata.IntersectionTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class SecondDfs {
    public boolean dfs(IntersectionNode<?> node, Deque<IntersectionNode<?>> mainDfsStack) {
    	Deque<IntersectionNode<?>> stack = new DequeSet<>();
        Set<IntersectionNode<?>> visited = new HashSet<>();
    	node.resetIterator();
        visited.add(node);
        stack.push(node);

        while (!stack.isEmpty()) {
            IntersectionNode<?> n = stack.getFirst();
            IntersectionTransition<?> trans = n.next();
            if (trans != null && trans.getTransition().getEvent() == null) {
            	continue;
            }
            
            IntersectionNode<?> child = (trans != null) ? trans.getTarget() : null;
            
            if (child != null) {
            	if (mainDfsStack.contains(child)) {
                    return true;
                } else if (!visited.contains(child)) {
                	visited.add(child);
                	stack.push(child);
                    child.resetIterator();
                }
            } else {
            	stack.pop();
            }
        }
        return false;
    }
}

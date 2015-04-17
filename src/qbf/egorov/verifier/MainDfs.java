/**
 * MainDfs.java, 09.05.2008
 */
package qbf.egorov.verifier;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import qbf.egorov.transducer.verifier.TransitionCounter;
import qbf.egorov.util.CollectionUtils;
import qbf.egorov.util.DequeSet;
import qbf.egorov.verifier.automata.IntersectionNode;
import qbf.egorov.verifier.automata.IntersectionTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class MainDfs {
    public Deque<IntersectionTransition<?>> dfs(IntersectionNode<?> node, TransitionCounter counter) {
        Deque<IntersectionTransition<?>> transitionStack = new LinkedList<>();
        Deque<IntersectionNode<?>> stack = new DequeSet<>();
        Set<IntersectionNode<?>> visited = new HashSet<>();
        visited.add(node);
        stack.push(node);
        transitionStack.push(new IntersectionTransition<>(null, node));

        while (!stack.isEmpty()) {
            IntersectionNode<?> n = stack.getFirst();
            IntersectionTransition<?> trans = n.next();
            if (trans != null && trans.getTransition().getEvent() == null) {
            	continue;
            }
            
            IntersectionNode<?> child = (trans != null) ? trans.getTarget() : null;
            
            if (child != null) {
                if (!visited.contains(child)) {
                	visited.add(child);
                	stack.push(child);
                    transitionStack.push(trans);
                }
            } else {
            	counter.leaveState(n.getState());
                assert n.next() == null;
                if (n.isTerminal() && new SecondDfs().dfs(n, stack)) {
                    return transitionStack;
                } else {
                	stack.pop();
                	transitionStack.pop();
                }
            }
        }
        return CollectionUtils.emptyDeque();
    }
}

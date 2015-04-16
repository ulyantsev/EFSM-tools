/**
 * AbstractDfs.java, 12.04.2008
 */
package qbf.egorov.verifier;

import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import qbf.egorov.util.DequeSet;
import qbf.egorov.verifier.automata.IntersectionNode;
import qbf.egorov.verifier.automata.IntersectionTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public abstract class AbstractDfs<R> extends NotifiableDfs<R> {
    private final Deque<IntersectionNode<?>> stack = new DequeSet<>();
    private final Deque<IntersectionTransition<?>> transStack = new LinkedList<>();
    
    private final Set<IntersectionNode<?>> visited;
    private R result;

    public AbstractDfs() {
        this.visited = new LinkedHashSet<>();
    }

    protected void enterNode(IntersectionNode<?> node) {
        notifyEnterState(node.getState());
    }

    protected boolean visitNode(IntersectionNode<?> node) {
        return false;
    }

    protected boolean leaveNode(IntersectionNode<?> node) {
        notifyLeaveState(node.getState());
        return false;
    }

    protected Deque<IntersectionNode<?>> getStack() {
        return stack;
    }

    protected Deque<IntersectionTransition<?>> getTransitionStack() {
        return transStack;
    }

    protected void setResult(R result) {
        this.result = result;
    }

    public R dfs(IntersectionNode<?> node) {
        stack.clear();
        transStack.clear();

        enterNode(node);
        visited.add(node);
        stack.push(node);
        transStack.push(new IntersectionTransition<>(null, node));

        while (!stack.isEmpty()) {
            IntersectionNode<?> n = stack.getFirst();
            IntersectionTransition<?> trans = n.next();
            IntersectionNode<?> child = (trans != null) ? trans.getTarget() : null;
            
            if (trans != null && trans.getTransition().getEvent() == null) {
            	continue;
            }
            
            if (child != null) {
                if (visitNode(child)) {
                    break;
                }
                if (!visited.contains(child)) {
                    if (visited.add(child)) {
                        stack.push(child);
                        transStack.push(trans);
                        enterNode(child);
                    }
                }
            } else {
                if (leaveNode(n)) {
                    break;
                }
                stack.pop();
                transStack.pop();
            }
        }
        return result;
    }
}

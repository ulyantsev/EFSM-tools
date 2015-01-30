/**
 * AbstractDfs.java, 12.04.2008
 */
package ru.ifmo.verifier;

import ru.ifmo.verifier.automata.IntersectionNode;
import ru.ifmo.verifier.automata.IntersectionTransition;
import ru.ifmo.verifier.automata.IIntersectionTransition;
import ru.ifmo.util.DequeSet;
import ru.ifmo.automata.statemachine.IState;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public abstract class AbstractDfs<R> extends NotifiableDfs<R> {
    private final Deque<IntersectionNode> stack = new DequeSet<IntersectionNode>();
    private final Deque<IIntersectionTransition> transStack = new LinkedList<IIntersectionTransition>();
    
    private final Set<IntersectionNode> visited;
    private R result;

    protected final ISharedData sharedData;
    protected final int threadId;

    public AbstractDfs(ISharedData sharedData, Set<IntersectionNode> visited, int threadId) {
        this.sharedData = sharedData;
        this.visited = visited;
        this.threadId = threadId;
    }

    protected void enterNode(IntersectionNode node) {
        notifyEnterState(node.getState());
    }

    protected boolean visitNode(IntersectionNode node) {
        return false;
    }

    protected boolean leaveNode(IntersectionNode node) {
        notifyLeaveState(node.getState());
        return false;
    }

    protected Deque<IntersectionNode> getStack() {
        return stack;
    }

    protected Deque<IIntersectionTransition> getTransitionStack() {
        return transStack;
    }

    protected void setResult(R result) {
        this.result = result;
    }

    public R dfs(IntersectionNode node) {
        stack.clear();
        transStack.clear();

        enterNode(node);
        visited.add(node);
        stack.push(node);
        transStack.push(new IntersectionTransition<IState>(null, node));

        while (!stack.isEmpty() && sharedData.getContraryInstance() == null) {
            IntersectionNode n = stack.getFirst();
            IIntersectionTransition trans = n.next(threadId);
            IntersectionNode child = (trans != null) ? trans.getTarget() : null;
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

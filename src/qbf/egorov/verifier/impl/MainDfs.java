/**
 * MainDfs.java, 09.05.2008
 */
package qbf.egorov.verifier.impl;

import qbf.egorov.util.CollectionUtils;
import qbf.egorov.verifier.AbstractDfs;
import qbf.egorov.verifier.ISharedData;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.automata.IntersectionNode;

import java.util.Deque;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class MainDfs extends AbstractDfs<Deque<IIntersectionTransition<?>>> {
    private final int curThreadId;
    
    public MainDfs(ISharedData sharedData, int curThreadId) {
        super(sharedData, sharedData.getVisited(), -1);
        setResult(CollectionUtils.<IIntersectionTransition<?>>emptyDeque());
        this.curThreadId = curThreadId;
    }

    protected boolean leaveNode(IntersectionNode<?> node) {
        super.leaveNode(node);
        assert node.next(threadId) == null;
        if (node.isTerminal()) {
            AbstractDfs<Boolean> dfs2 = new SecondDfs(sharedData, getStack(), curThreadId);
            if (dfs2.dfs(node)) {
                setResult(getTransitionStack());
                return true;
            }
        }
        return false;
    }
}

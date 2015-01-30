/**
 * MainDfs.java, 09.05.2008
 */
package ru.ifmo.verifier.impl;

import ru.ifmo.verifier.automata.IntersectionNode;
import ru.ifmo.verifier.automata.IIntersectionTransition;
import ru.ifmo.verifier.AbstractDfs;
import ru.ifmo.verifier.ISharedData;
import ru.ifmo.util.CollectionUtils;

import java.util.Deque;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class MainDfs extends AbstractDfs<Deque<IIntersectionTransition>> {
    private final int curThreadId;
    
    public MainDfs(ISharedData sharedData, int curThreadId) {
        super(sharedData, sharedData.getVisited(), -1);
        setResult(CollectionUtils.<IIntersectionTransition>emptyDeque());
        this.curThreadId = curThreadId;
    }

    protected boolean leaveNode(IntersectionNode node) {
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

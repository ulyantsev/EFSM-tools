/**
 * MainDfs.java, 09.05.2008
 */
package qbf.egorov.verifier.impl;

import java.util.Deque;

import qbf.egorov.util.CollectionUtils;
import qbf.egorov.verifier.AbstractDfs;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.automata.IntersectionNode;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class MainDfs extends AbstractDfs<Deque<IIntersectionTransition<?>>> {
    public MainDfs() {
        setResult(CollectionUtils.<IIntersectionTransition<?>>emptyDeque());
    }

    protected boolean leaveNode(IntersectionNode<?> node) {
        super.leaveNode(node);
        assert node.next() == null;
        if (node.isTerminal()) {
            if (new SecondDfs(getStack()).dfs(node)) {
                setResult(getTransitionStack());
                return true;
            }
        }
        return false;
    }
}

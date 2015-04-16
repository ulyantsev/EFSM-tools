/**
 * SecondDfs.java, 09.05.2008
 */
package qbf.egorov.verifier.impl;

import java.util.Deque;

import qbf.egorov.verifier.AbstractDfs;
import qbf.egorov.verifier.automata.IntersectionNode;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class SecondDfs extends AbstractDfs<Boolean> {
    private Deque<IntersectionNode<?>> mainDfsStack;

    public SecondDfs(Deque<IntersectionNode<?>> mainDfsStack) {
        this.mainDfsStack = mainDfsStack;
        setResult(false);
    }

    protected void enterNode(IntersectionNode<?> node) {
        node.resetIterator();
    }

    protected boolean visitNode(IntersectionNode<?> node) {
        if (mainDfsStack.contains(node)) {
            setResult(true);
            return true;
        }
        return false;
    }

    protected boolean leaveNode(IntersectionNode<?> node) {
        return false;
    }
}

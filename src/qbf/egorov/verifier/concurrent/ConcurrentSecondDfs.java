/*
 * Developed by eVelopers Corporation - 03.06.2008
 */
package qbf.egorov.verifier.concurrent;

import java.util.HashSet;

import qbf.egorov.util.concurrent.DfsStackTreeNode;
import qbf.egorov.verifier.AbstractDfs;
import qbf.egorov.verifier.ISharedData;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.automata.IntersectionNode;

public class ConcurrentSecondDfs extends AbstractDfs<Boolean> {
    private DfsStackTreeNode<IIntersectionTransition> mainDfsStack;

    public ConcurrentSecondDfs(ISharedData sharedData, DfsStackTreeNode<IIntersectionTransition> mainDfsStack,  int threadId) {
        super(sharedData, new HashSet<IntersectionNode>(), threadId);
        this.mainDfsStack = mainDfsStack;
        setResult(false);
    }

    protected void enterNode(IntersectionNode node) {
        node.resetIterator(threadId);
    }

    protected boolean visitNode(IntersectionNode node) {
        if (node.isOwner(threadId)) {
            if (sharedData.setContraryInstance(mainDfsStack)) {
                setResult(true);
                return true;
            }
        }
        return false;
    }

    protected boolean leaveNode(IntersectionNode node) {
        return false;
    }
}

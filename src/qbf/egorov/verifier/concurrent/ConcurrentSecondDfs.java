/*
 * Developed by eVelopers Corporation - 03.06.2008
 */
package qbf.egorov.verifier.concurrent;

import qbf.egorov.util.concurrent.DfsStackTreeNode;
import qbf.egorov.verifier.AbstractDfs;
import qbf.egorov.verifier.IInterNode;
import qbf.egorov.verifier.ISharedData;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.automata.IntersectionNode;

import java.util.HashSet;

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

                //TODO: delete stack print  ------------------------
                /*synchronized (System.out) {
                    if (getStack().isEmpty()) {
                        System.out.println("Stack is empty");
                    } else {
                        System.out.println("DFS 2 stack:");
                    }
                    final int MAX_LEN = 80;
                    int len = 0;
                    for (IInterNode n: getStack()) {
                        String tmp = n.toString();
                        len += tmp.length();
                        if (len > MAX_LEN) {
                            len = tmp.length();
                            System.out.println();
                        }
                        System.out.print("-->" + tmp);
                    }
                    System.out.println("-->" + node);
                }*/
                //--------------------------------------------------
                return true;
            }
        }
        return false;
    }

    protected boolean leaveNode(IntersectionNode node) {
        return false;
    }
}

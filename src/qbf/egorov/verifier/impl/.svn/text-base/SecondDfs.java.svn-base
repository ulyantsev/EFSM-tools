/**
 * SecondDfs.java, 09.05.2008
 */
package ru.ifmo.verifier.impl;

import ru.ifmo.verifier.AbstractDfs;
import ru.ifmo.verifier.IInterNode;
import ru.ifmo.verifier.ISharedData;
import ru.ifmo.verifier.automata.IntersectionNode;

import java.util.Deque;
import java.util.HashSet;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class SecondDfs extends AbstractDfs<Boolean> {
    private Deque<IntersectionNode> mainDfsStack;

    public SecondDfs(ISharedData sharedData, Deque<IntersectionNode> mainDfsStack,  int threadId) {
        super(sharedData, new HashSet<IntersectionNode>(), threadId);
        this.mainDfsStack = mainDfsStack;
        setResult(false);
    }

    protected void enterNode(IntersectionNode node) {
        node.resetIterator(threadId);
    }

    protected boolean visitNode(IntersectionNode node) {
        if (mainDfsStack.contains(node)) {
//            sharedData.setContraryInstance(mainDfsStack);
//            sharedData.notifyAllUnoccupiedThreads();
            
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
        return false;
    }

    protected boolean leaveNode(IntersectionNode node) {
        return false;
    }
}

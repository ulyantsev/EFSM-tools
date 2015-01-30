/*
 * Developed by eVelopers Corporation - 21.05.2008
 */
package ru.ifmo.verifier.concurrent;

import ru.ifmo.verifier.automata.IntersectionNode;
import ru.ifmo.verifier.automata.IIntersectionTransition;
import ru.ifmo.verifier.AbstractDfs;
import ru.ifmo.verifier.ISharedData;
import ru.ifmo.verifier.NotifiableDfs;
import ru.ifmo.util.concurrent.DfsStackTreeNode;
import ru.ifmo.util.concurrent.DfsStackTree;

import java.util.*;

public class ConcurrentMainDfs extends NotifiableDfs<Void> {
//    private long childVisit = 0;
//    private int childDepth = 0;
//    private int maxChildDepth = 0;

    private DfsStackTree<IIntersectionTransition> stackTree;
    //current dfs stack tree node
    private DfsStackTreeNode<IIntersectionTransition> stackTreeNode;

    private final Set<IntersectionNode> visited;

    private final ISharedData sharedData;
    private final int threadId;

    public ConcurrentMainDfs(ISharedData sharedData, DfsStackTree<IIntersectionTransition> stackTree, int threadId) {
        this.sharedData = sharedData;
        this.visited = sharedData.getVisited();
        this.threadId = threadId;
        this.stackTree = stackTree;
        this.stackTreeNode = stackTree.getRoot();
    }

    protected boolean leaveNode() {
        if (stackTreeNode.wasLeft.compareAndSet(false, true)) {
            IntersectionNode node = stackTreeNode.getItem().getTarget();

            notifyLeaveState(node.getState());
            stackTreeNode.remove();

            if (node.isTerminal()) {
                AbstractDfs<Boolean> dfs2 = new ConcurrentSecondDfs(sharedData, stackTreeNode, threadId);
                if (dfs2.dfs(node)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Void dfs(IntersectionNode node) {
        if (node != stackTreeNode.getItem().getTarget()) {
            throw new IllegalArgumentException();
        }

        visited.add(node);
        node.addOwner(threadId);
        while (stackTreeNode != null && sharedData.getContraryInstance() == null) {
            IIntersectionTransition trans = stackTreeNode.getItem().getTarget().next(-1);
            IntersectionNode child = (trans != null) ? trans.getTarget() : null;
            if (child != null) {
                if (!visited.contains(child)) {
                    if (visited.add(child)) {
                        stackTreeNode = stackTree.addChild(stackTreeNode, trans);

                        IntersectionNode n = stackTreeNode.getItem().getTarget();
                        n.addOwner(threadId);
                        notifyEnterState(n.getState());
                    }
                }
            } else {
                boolean flag = true;
                if (stackTreeNode.hasChildren()) {
                    for (DfsStackTreeNode<IIntersectionTransition> childNode: stackTreeNode.getChildren()) {
                        if (!childNode.wasLeft.get()) {
                            //TODO
//                            childVisit++;
//                            childDepth++;
                            //----------

                            flag = false;
                            stackTreeNode = childNode;
                            stackTreeNode.getItem().getTarget().addOwner(threadId);
                            break;
                        }
                    }
                }
                if (flag) {
                    if (leaveNode()) {
                        break;
                    }
//                    if (childDepth > 0) {
//                        maxChildDepth = Math.max(childDepth, maxChildDepth);
//                        childDepth--;
//                    }
                    stackTreeNode.getItem().getTarget().removeOwner(threadId);
                    stackTreeNode = stackTreeNode.getParent();
                }
            }
        }
//        System.out.println("Child visit: " + childVisit);
//        System.out.println("Child depth: " + maxChildDepth);
        return null;
    }
}

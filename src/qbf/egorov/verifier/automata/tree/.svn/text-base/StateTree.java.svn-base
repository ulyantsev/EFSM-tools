/**
 * StateTree.java, 27.04.2008
 */
package ru.ifmo.verifier.automata.tree;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.automata.statemachine.IStateTransition;
import ru.ifmo.automata.statemachine.IStateMachine;

import java.util.Iterator;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class StateTree<S extends IState> implements ITree<S> {

    private ITreeNode<S> root;
    private int hash = -1;

    public StateTree(ITreeNode<S> root) {
        this.root = root;
    }

    public StateTree(ITree<S> tree, ITreeNode<S> node, IStateTransition trans) {
        root = copy(tree.getRoot(), node, trans);
        assert root.isActive();
    }

    public ITreeNode<S> getRoot() {
        return root;
    }

    public ITreeNode<S> getNodeForStateMachine(IStateMachine<S> stateMachine) {
        IStateMachine<S> parentSM = stateMachine.getParentStateMachine();
        
        if (parentSM != null) {
            ITreeNode<S> parentNode = getNodeForStateMachine(parentSM);

            assert parentNode.getStateMachine().equals(parentSM);
            return parentNode.getChild(stateMachine);
        }
        assert root.getStateMachine().equals(stateMachine);
        return root;
    }

    protected ITreeNode<S> copy(ITreeNode<S> node, ITreeNode<S> fromNode, IStateTransition trans) {
        boolean isTransNode = (node == fromNode);
        S state = isTransNode ? (S) trans.getTarget() : node.getState();
        TreeNode<S> newNode = new TreeNode<S>(state, node.getStateMachine(), node.isActive());

        if (isTransNode) {
            assert node.isActive();
            
            for (ITreeNode<S> child: node.getChildren()) {
                newNode.addChildren(copyTransSubnode(newNode, child));
            }
        } else {
            for (ITreeNode<S> child: node.getChildren()) {
                newNode.addChildren(copy(child, fromNode, trans));
            }
        }
        return newNode;
    }

    protected ITreeNode<S> copyTransSubnode(ITreeNode<S> parent, ITreeNode<S> node) {
        boolean active = node.getStateMachine().getParentStates().containsKey(parent.getState());
        TreeNode<S> newNode = new TreeNode<S>(node.getState(), node.getStateMachine(), active);

        for (ITreeNode<S> child: node.getChildren()) {
            newNode.addChildren(copyTransSubnode(node, child));
        }
        return newNode;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StateTree)) {
            return false;
        }
        StateTree<S> tree = (StateTree) o;

        return equals(root, tree.getRoot());
    }

    public int hashCode() {
        if (hash == -1) {
            hash = hashCode(root);
        }
        return hash;
    }

    protected int hashCode(ITreeNode<S> node) {
        int hash = node.getState().hashCode();
        for (ITreeNode<S> child: node.getChildren()) {
            hash += hashCode(child);
        }
        return hash;
    }

    protected boolean equals(ITreeNode<S> node1, ITreeNode<S> node2) {
        if (!node1.getState().equals(node2.getState())) {
            return false;
        }
        assert node1.getChildren().size() == node2.getChildren().size();
        Iterator<ITreeNode<S>> iter = node2.getChildren().iterator();

        for (ITreeNode<S> child1: node1.getChildren()) {
            if (iter.hasNext()) {
                ITreeNode<S> child2 = iter.next();
                if (!equals(child1, child2)) {
                    return false;
                }
            } else {
                throw new RuntimeException(String.format("%s and %s has different number of childred", node1, node2));
            }
        }
        return true;
    }
}

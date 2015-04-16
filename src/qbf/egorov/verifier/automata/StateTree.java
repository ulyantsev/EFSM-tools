/**
 * StateTree.java, 27.04.2008
 */
package qbf.egorov.verifier.automata;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;
import qbf.egorov.statemachine.IStateTransition;

import java.util.Iterator;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class StateTree<S extends IState> {
    private TreeNode<S> root;
    private int hash = -1;

    public StateTree(TreeNode<S> root) {
        this.root = root;
    }

    public StateTree(StateTree<S> tree, TreeNode<S> node, IStateTransition trans) {
        root = copy(tree.getRoot(), node, trans);
        assert root.isActive();
    }

    public TreeNode<S> getRoot() {
        return root;
    }

    public TreeNode<S> getNodeForStateMachine(IStateMachine<S> stateMachine) {
        IStateMachine<S> parentSM = stateMachine.getParentStateMachine();
        
        if (parentSM != null) {
            TreeNode<S> parentNode = getNodeForStateMachine(parentSM);

            assert parentNode.getStateMachine().equals(parentSM);
            return parentNode.getChild(stateMachine);
        }
        assert root.getStateMachine().equals(stateMachine);
        return root;
    }

    protected TreeNode<S> copy(TreeNode<S> node, TreeNode<S> fromNode, IStateTransition trans) {
        boolean isTransNode = (node == fromNode);
        @SuppressWarnings("unchecked")
		S state = isTransNode ? (S) trans.getTarget() : node.getState();
        TreeNode<S> newNode = new TreeNode<S>(state, node.getStateMachine(), node.isActive());

        if (isTransNode) {
            assert node.isActive();
            
            for (TreeNode<S> child: node.getChildren()) {
                newNode.addChildren(copyTransSubnode(newNode, child));
            }
        } else {
            for (TreeNode<S> child: node.getChildren()) {
                newNode.addChildren(copy(child, fromNode, trans));
            }
        }
        return newNode;
    }

    protected TreeNode<S> copyTransSubnode(TreeNode<S> parent, TreeNode<S> node) {
        boolean active = node.getStateMachine().getParentStates().containsKey(parent.getState());
        TreeNode<S> newNode = new TreeNode<S>(node.getState(), node.getStateMachine(), active);

        for (TreeNode<S> child: node.getChildren()) {
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
        @SuppressWarnings("unchecked")
		StateTree<S> tree = (StateTree<S>) o;

        return equals(root, tree.getRoot());
    }

    public int hashCode() {
        if (hash == -1) {
            hash = hashCode(root);
        }
        return hash;
    }

    protected int hashCode(TreeNode<S> node) {
        int hash = node.getState().hashCode();
        for (TreeNode<S> child: node.getChildren()) {
            hash += hashCode(child);
        }
        return hash;
    }

    protected boolean equals(TreeNode<S> node1, TreeNode<S> node2) {
        if (!node1.getState().equals(node2.getState())) {
            return false;
        }
        assert node1.getChildren().size() == node2.getChildren().size();
        Iterator<TreeNode<S>> iter = node2.getChildren().iterator();

        for (TreeNode<S> child1: node1.getChildren()) {
            if (iter.hasNext()) {
                TreeNode<S> child2 = iter.next();
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

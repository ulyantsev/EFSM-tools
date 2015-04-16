/**
 * TreeNode.java, 27.04.2008
 */
package qbf.egorov.verifier.automata;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class TreeNode<S extends IState> {
    private S state;
    private IStateMachine<S> stateMachine;
    private boolean active;

    private Map<IStateMachine<S>, TreeNode<S>> children
            = new LinkedHashMap<IStateMachine<S>, TreeNode<S>>();

    public TreeNode(S state, IStateMachine<S> stateMachine, boolean active) {
        this.state = state;
        this.stateMachine = stateMachine;
        this.active = active;
    }

    public S getState() {
        return state;
    }

    public IStateMachine<S> getStateMachine() {
        return stateMachine;
    }

    public boolean isActive() {
        return active;
    }

    public void addChildren(TreeNode<S> node) {
        children.put(node.getStateMachine(), node);
    }

    /**
     * Get modifiable set of children
     * @return set of children nodes
     */
    public Collection<TreeNode<S>> getChildren() {
        return children.values();
    }

    public TreeNode<S> getChild(IStateMachine<S> stateMachine) {
        return children.get(stateMachine);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode)) return false;

        TreeNode<?> treeNode = (TreeNode<?>) o;

        if (state != null ? !state.equals(treeNode.state) : treeNode.state != null) return false;

        return true;
    }

    public String toString() {
        return stateMachine.getName() + '.' + state.getName();
    }
}

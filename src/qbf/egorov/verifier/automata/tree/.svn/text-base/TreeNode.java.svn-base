/**
 * TreeNode.java, 27.04.2008
 */
package ru.ifmo.verifier.automata.tree;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.automata.statemachine.IStateMachine;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class TreeNode<S extends IState> implements ITreeNode<S> {
    private S state;
    private IStateMachine<S> stateMachine;
    private boolean active;

    private Map<IStateMachine<S>, ITreeNode<S>> children
            = new LinkedHashMap<IStateMachine<S>, ITreeNode<S>>();

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

    public void addChildren(ITreeNode<S> node) {
        children.put(node.getStateMachine(), node);
    }

    /**
     * Get modifiable set of children
     * @return set of children nodes
     */
    public Collection<ITreeNode<S>> getChildren() {
        return children.values();
    }

    public ITreeNode<S> getChild(IStateMachine<S> stateMachine) {
        return children.get(stateMachine);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TreeNode)) return false;

        TreeNode treeNode = (TreeNode) o;

        if (state != null ? !state.equals(treeNode.state) : treeNode.state != null) return false;

        return true;
    }

    public String toString() {
        return stateMachine.getName() + '.' + state.getName();
    }
}

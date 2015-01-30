/**
 * ITreeNode.java, 27.04.2008
 */
package ru.ifmo.verifier.automata.tree;

import ru.ifmo.automata.statemachine.IStateMachine;
import ru.ifmo.automata.statemachine.IState;

import java.util.Collection;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface ITreeNode<S extends IState> {
    S getState();

    IStateMachine<S> getStateMachine();

    boolean isActive();

    /**
     * Get modifiable set of children
     * @return set of children nodes
     */
    Collection<ITreeNode<S>> getChildren();

    ITreeNode<S> getChild(IStateMachine<S> stateMachine);
}

/**
 * ITreeNode.java, 27.04.2008
 */
package qbf.egorov.verifier.automata.tree;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;

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

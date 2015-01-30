/**
 * ITree.java, 27.04.2008
 */
package qbf.egorov.verifier.automata.tree;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateMachine;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface ITree<S extends IState> {

    ITreeNode<S> getRoot();

    ITreeNode<S> getNodeForStateMachine(IStateMachine<S> stateMachine);
}

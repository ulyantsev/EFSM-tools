/**
 * IComplexStateFactory.java, 27.04.2008
 */
package qbf.egorov.verifier.automata.statemachine;

import qbf.egorov.statemachine.IState;
import qbf.egorov.verifier.automata.tree.ITree;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IComplexStateFactory<S extends IState> {

//    S getInitialState(IStateMachine<IState> stateMachine);

    ComplexState<S> getState(ITree<S> tree);
}

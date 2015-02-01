/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.verifier.automata;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateTransition;
import qbf.egorov.automata.ITransition;

/**
 * @author kegorov
 *         Date: Jul 22, 2009
 */
public interface IIntersectionTransition<S extends IState> extends ITransition<IntersectionNode<S>> {
    IStateTransition getTransition();
    IntersectionNode<S> getTarget();
}

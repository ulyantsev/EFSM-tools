/**
 * IState.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.automata.INode;

import java.util.List;
import java.util.Set;

/**
 * The state machine state
 *
 * @author Kirill Egorov
 */
public interface IState extends INode<IStateTransition> {

    String getName();

    StateType getType();

    List<IAction> getActions();

    Set<IStateMachine<? extends IState>> getNestedStateMachines();

    String getUniqueName();
}

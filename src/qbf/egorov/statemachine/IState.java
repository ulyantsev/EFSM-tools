/**
 * IState.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import java.util.List;

import qbf.egorov.automata.INode;

/**
 * The state machine state
 *
 * @author Kirill Egorov
 */
public interface IState extends INode<IStateTransition> {
    String getName();
    StateType getType();
    List<IAction> getActions();
    String getUniqueName();
}

/**
 * IState.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import java.util.List;

import qbf.egorov.automata.INode;
import qbf.egorov.statemachine.impl.Action;

/**
 * The state machine state
 *
 * @author Kirill Egorov
 */
public interface IState extends INode<IStateTransition> {
    String getName();
    StateType getType();
    List<Action> getActions();
    String getUniqueName();
}

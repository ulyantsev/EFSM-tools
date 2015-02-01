/**
 * ITransition.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.automata.ITransition;

import java.util.List;

/**
 * Transition between two states (between source and target)
 *
 * @author Kirill Egorov
 */
public interface IStateTransition extends ITransition<IState> {
    IEvent getEvent();
    List<IAction> getActions();
    ICondition getCondition();
}

/**
 * ITransition.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.automata.ITransition;
import qbf.egorov.statemachine.impl.Action;
import qbf.egorov.statemachine.impl.Event;

import java.util.List;

/**
 * Transition between two states (between source and target)
 *
 * @author Kirill Egorov
 */
public interface IStateTransition extends ITransition<IState> {
    Event getEvent();
    List<Action> getActions();
    ICondition getCondition();
}

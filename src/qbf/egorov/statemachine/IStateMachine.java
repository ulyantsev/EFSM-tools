/**
 * IStateMachine.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import java.util.Map;

/**
 * State machine
 *
 * @author Kirill Egorov
 */
public interface IStateMachine<S extends IState> {
    String getName();
    IStateMachine<S> getParentStateMachine();
    Map<S, IStateMachine<S>> getParentStates();
    S getInitialState();
    S getState(String stateName);
}

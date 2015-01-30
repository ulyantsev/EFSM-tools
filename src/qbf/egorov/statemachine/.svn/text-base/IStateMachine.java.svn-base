/**
 * IStateMachine.java, 01.03.2008
 */
package ru.ifmo.automata.statemachine;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * State machine
 *
 * @author Kirill Egorov
 */
public interface IStateMachine<S extends IState> {

    String getName();

    IStateMachine<S> getParentStateMachine();

    Set<IStateMachine<S>> getNestedStateMachines();

    Map<S, IStateMachine<S>> getParentStates();

    boolean isNested();

    S getInitialState();

    S getState(String stateName);

    Collection<S> getStates();

    Set<IEventProvider> getEventProviders();

    IControlledObject getControlledObject(String association);

    Collection<IControlledObject> getControlledObjects();

    /**
     * Get sources for condition evaluation.
     * @return
     */
    Map<String, Map<String, ?>> getSources();
}

/**
 * StateMachine.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import qbf.egorov.statemachine.IControlledObject;
import qbf.egorov.statemachine.IEventProvider;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.StateType;

/**
 * The IStateMachine implementation
 *
 * @author Kirill Egorov
 */
public class StateMachine<S extends IState> {
    private String name;
    private S initialState;
    private Map<String, S> states = new HashMap<>();
    private Set<IEventProvider> eventProviders = new HashSet<>();
    private Map<String, IControlledObject> ctrlObjects = new HashMap<>();

    private StateMachine<S> parentStateMachine;
    private Map<S, StateMachine<S>> parentStates = new HashMap<>();

    public StateMachine(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public StateMachine<S> getParentStateMachine() {
        return parentStateMachine;
    }

    public Map<S, StateMachine<S>> getParentStates() {
        return parentStates;
    }

    public S getInitialState() {
        if (initialState == null) {
            throw new RuntimeException("Automamta has not been initialized yet or has not initial state");
        }
        return initialState;
    }

    public S getState(String stateName) {
        return states.get(stateName);
    }

    public void addEventProvider(IEventProvider provider) {
        eventProviders.add(provider);
    }

    public void addState(S s) {
        checkInitial(s);
        states.put(s.getName(), s);
    }

    private void checkInitial(S s)  {
        if (StateType.INITIAL == s.getType()) {
            if (initialState != null) {
                throw new IllegalArgumentException("StateMachine can't contain more than one initial state");
            }
            initialState = s;
        }
    }

    public void addControlledObject(String association, IControlledObject ctrlObject) {
        ctrlObjects.put(association, ctrlObject);
    }

    @Override
    public String toString() {
        return name; 
    }
}

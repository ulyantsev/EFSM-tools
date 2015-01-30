/**
 * StateMachine.java, 02.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The IStateMachine implementation
 *
 * @author Kirill Egorov
 */
public class StateMachine<S extends IState> implements IStateMachine<S> {

    private String name;
    private S initialState;
    private Map<String, S> states = new HashMap<String, S>();
    private Set<IEventProvider> eventProviders = new HashSet<IEventProvider>();
    private Map<String, IControlledObject> ctrlObjects = new HashMap<String, IControlledObject>();

    private Map<String, Map<String, ?>> sources;
    private Map<S, List<IFunction>> functions;

    private IStateMachine<S> parentStateMachine;
    private Map<S, IStateMachine<S>> parentStates = new HashMap<S, IStateMachine<S>>();

    private Set<IStateMachine<S>> nestedStateMachines = new LinkedHashSet<IStateMachine<S>>();

    public StateMachine(String name) {
        this.name = name;
    }

    protected void createSource() {
        sources = new HashMap<String, Map<String, ?>>();
        for (Map.Entry<String, IControlledObject> e: ctrlObjects.entrySet()) {
            Map<String, Object> functions = new HashMap<String, Object>();
            for (IFunction f: e.getValue().getFunctions()) {
                functions.put(f.getName(), f.getCurValue());
            }
            sources.put(e.getKey(), functions);
        }
    }

    public String getName() {
        return name;
    }

    public IStateMachine<S> getParentStateMachine() {
        return parentStateMachine;
    }

    public Set<IStateMachine<S>> getNestedStateMachines() {
        return nestedStateMachines;
    }

    public Map<S, IStateMachine<S>> getParentStates() {
        return parentStates;
    }

    public <T extends IStateMachine<S>> void setParent(T parentStateMachine, S parentState) {
        if (parentStateMachine == null || parentState == null) {
            throw new IllegalArgumentException("parent parameters can't be null");
        }
        if (this.parentStateMachine != null && this.parentStateMachine != parentStateMachine) {
            throw new UnsupportedOperationException("State machine can't have more than one parent");
        }
//        if (!parentState.equals(parentStateMachine.getState(parentState.getName()))) {
//            throw new IllegalArgumentException("parentState isn't parentStateMachine state");
//        }
        if (!parentState.getNestedStateMachines().contains(this)) {
            throw new IllegalArgumentException("This stateMachine isn't nested stateMachine for parentState");
        }
        this.parentStateMachine = parentStateMachine;
        parentStates.put(parentState, parentStateMachine);
    }

    public void addNestedStateMachine(IStateMachine<S> stateMachine) {
        nestedStateMachines.add(stateMachine);
    }

    public boolean isNested() {
        return parentStateMachine != null;
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

    public Collection<S> getStates() {
        return states.values();
    }

    public Set<IEventProvider> getEventProviders() {
        return eventProviders;
    }

    public void addEventProvider(IEventProvider provider) {
        eventProviders.add(provider);
    }

    public IControlledObject getControlledObject(String association) {
        return ctrlObjects.get(association);
    }

    public Collection<IControlledObject> getControlledObjects() {
        return ctrlObjects.values();
    }

    public Map<String, Map<String, ?>> getSources() {
        if (sources == null) {
            createSource();
        }
        return sources;
    }

    public void addState(S s) {
        checkInitial(s);
        states.put(s.getName(), s);
    }

    public void addStates(Map<String, S> states) {
        for (Map.Entry<String, S> entry: states.entrySet()) {
            this.states.put(entry.getKey(), entry.getValue());
            checkInitial(entry.getValue());
        }
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

    public List<IFunction> getStateFunctions(S state) {
        if (!states.containsValue(state)) {
            throw new IllegalArgumentException("State machine doesn't contain this state: " + state);
        }
        synchronized (this) {
            if (functions == null) {
                functions = new HashMap<S, List<IFunction>>();
            }
            List<IFunction> stateFns = functions.get(state);
            if (stateFns == null) {
                stateFns = createStateFunctions(state);
                functions.put(state, stateFns);
            }
            return Collections.unmodifiableList(stateFns);
        }
    }

    private List<IFunction> createStateFunctions(IState state) {
        List<IFunction> list = new ArrayList<IFunction>();
        for (String fName: getInvocations(state)) {
            int i = fName.indexOf('.');
            String ctrlObj = fName.substring(0, i);
            String funName = fName.substring(i + 1);
            IFunction fun = ctrlObjects.get(ctrlObj).getFunction(funName);
            list.add(fun);
        }
        return list;
    }

    private Set<String> getInvocations(IState state) {
        Set<String> invocations = new HashSet<String>();
        for (IStateTransition t: state.getOutcomingTransitions()) {
            if (t.getCondition() != null) {
                invocations.addAll(getInvocations(t.getCondition().getExpression()));
            }
        }
        return invocations;
    }

    private Set<String> getInvocations(String expr) {
        if (expr == null) {
            return Collections.emptySet();
        }
        Set<String> invocations = new HashSet<String>();
        Pattern p = Pattern.compile(StateMachineUtils.METHOD_PATTERN);
        Matcher m = p.matcher(expr);
        while (m.find()) {
            invocations.add(expr.substring(m.start(), m.end()));
        }
        return invocations;
    }

    public String toString() {
        return name; 
    }
}

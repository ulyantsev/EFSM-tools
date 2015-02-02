/**
 * StateMachine.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.*;

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
    private Map<String, S> states = new HashMap<>();
    private Set<IEventProvider> eventProviders = new HashSet<>();
    private Map<String, IControlledObject> ctrlObjects = new HashMap<>();

    private Map<S, List<IFunction>> functions;

    private IStateMachine<S> parentStateMachine;
    private Map<S, IStateMachine<S>> parentStates = new HashMap<>();

    public StateMachine(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public IStateMachine<S> getParentStateMachine() {
        return parentStateMachine;
    }

    public Map<S, IStateMachine<S>> getParentStates() {
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

    @Override
    public String toString() {
        return name; 
    }
}

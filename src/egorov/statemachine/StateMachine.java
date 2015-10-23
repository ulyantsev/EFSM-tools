/**
 * StateMachine.java, 02.03.2008
 */
package egorov.statemachine;

import java.util.HashMap;
import java.util.Map;

/**
 * The IStateMachine implementation
 *
 * @author Kirill Egorov
 */
public class StateMachine {
    private SimpleState initialState;
    private final Map<String, SimpleState> states = new HashMap<>();

    public SimpleState getInitialState() {
        if (initialState == null) {
            throw new RuntimeException("Automata has not been initialized yet or has not initial state");
        }
        return initialState;
    }

    public SimpleState getState(String stateName) {
        return states.get(stateName);
    }

    public void addState(SimpleState s) {
        checkInitial(s);
        states.put(s.name, s);
    }

    private void checkInitial(SimpleState s)  {
        if (s.isInitial) {
            if (initialState != null) {
                throw new IllegalArgumentException("StateMachine can't contain more than one initial state");
            }
            initialState = s;
        }
    }

    @Override
    public String toString() {
        return "StateMachine"; 
    }
}

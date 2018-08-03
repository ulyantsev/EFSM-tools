package verification.statemachine;

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

    public SimpleState initialState() {
        if (initialState == null) {
            throw new RuntimeException("StateMachine has no initial state!");
        }
        return initialState;
    }

    public void addState(SimpleState s) {
        checkInitial(s);
        states.put(s.name, s);
    }

    private void checkInitial(SimpleState s)  {
        if (s.isInitial) {
            if (initialState != null) {
                throw new IllegalArgumentException("StateMachine can't contain more than one initial state!");
            }
            initialState = s;
        }
    }

    @Override
    public String toString() {
        return "StateMachine"; 
    }
}

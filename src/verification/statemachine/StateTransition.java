package verification.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The IStateTransition implementation
 *
 * @author Kirill Egorov
 */
public class StateTransition {
    public final String event;
    private final List<String> actions = new ArrayList<>();
    private final SimpleState target;

    public StateTransition(String event, SimpleState target) {
        this.event = event;
        this.target = target;
    }

    public List<String> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public SimpleState getTarget() {
        return target;
    }

    public void addAction(String a) {
        actions.add(a);
    }
}

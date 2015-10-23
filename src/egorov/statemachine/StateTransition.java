/**
 * Transition.java, 02.03.2008
 */
package egorov.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import egorov.automata.ITransition;

/**
 * The IStateTransition implementation
 *
 * @author Kirill Egorov
 */
public class StateTransition implements ITransition<SimpleState> {
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

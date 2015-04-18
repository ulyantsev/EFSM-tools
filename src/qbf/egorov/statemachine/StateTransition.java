/**
 * Transition.java, 02.03.2008
 */
package qbf.egorov.statemachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import qbf.egorov.automata.ITransition;

/**
 * The IStateTransition implementation
 *
 * @author Kirill Egorov
 */
public class StateTransition implements ITransition<SimpleState> {
    public final Event event;
    private final List<Action> actions = new ArrayList<>();
    private final SimpleState target;

    public StateTransition(Event event, SimpleState target) {
        this.event = event;
        this.target = target;
    }

    public List<Action> getActions() {
        return Collections.unmodifiableList(actions);
    }

    public SimpleState getTarget() {
        return target;
    }

    public void addAction(Action a) {
        actions.add(a);
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import qbf.egorov.statemachine.IControlledObject;

/**
 * @author kegorov
 *         Date: Jun 17, 2009
 */
public class ControlledObjectStub implements IControlledObject {
    private final String name;
    private final Map<String, Action> actions = new HashMap<>();

    public ControlledObjectStub(String name, String ... actions) {
        this(name, Arrays.asList(actions));
    }

    private ControlledObjectStub(String name, Collection<String> actions) {
        this.name = name;
        for (String a: actions) {
            this.actions.put(a, new Action(a));
        }
    }

    public String getName() {
        return name;
    }

    public Action getAction(String actionName) {
        return actions.get(actionName);
    }

    public Collection<Action> getActions() {
        return Collections.unmodifiableCollection(actions.values());
    }
}

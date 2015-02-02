/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.IAction;
import qbf.egorov.statemachine.IControlledObject;
import qbf.egorov.statemachine.IFunction;

import java.util.*;

/**
 * @author kegorov
 *         Date: Jun 17, 2009
 */
public class ControlledObjectStub implements IControlledObject {
    private String name;
    private Map<String, IAction> actions = new HashMap<String, IAction>();

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

    public IAction getAction(String actionName) {
        return actions.get(actionName);
    }

    public Collection<IAction> getActions() {
        return Collections.unmodifiableCollection(actions.values());
    }

    public IFunction getFunction(String funName) {
        return null;
    }

    public Collection<IFunction> getFunctions() {
        return Collections.emptyList();
    }
}

/**
 * ControlledObject.java, 02.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.IControlledObject;
import ru.ifmo.automata.statemachine.IAction;
import ru.ifmo.automata.statemachine.IFunction;

import java.util.*;
import java.lang.reflect.Method;

import com.evelopers.unimod.runtime.context.StateMachineContext;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class ControlledObject implements IControlledObject {
    private String name;
    private Class implClass;
    private Map<String, IAction> actions;
    private Map<String, IFunction> functions;

    public ControlledObject(String name, Class implClass) {
        this.name = name;
        this.implClass = implClass;
        findMethods(implClass);
    }

    protected void findMethods(Class clazz) {
        actions = new HashMap<String, IAction>();
        functions = new HashMap<String, IFunction>();
        for (Method m: clazz.getMethods()) {
            Class[] params = m.getParameterTypes();
            if (params.length == 1 && StateMachineContext.class.isAssignableFrom(params[0])) {
                if (!m.getReturnType().equals(void.class)) {
                    IFunction func = new Function(m.getName(), m.getReturnType());
                    actions.put(func.getName(), func);
                    functions.put(func.getName(), func);
                } else {
                    actions.put(m.getName(), new Action(m.getName(), null));
                }
            }
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
        return functions.get(funName);
    }

    public Collection<IFunction> getFunctions() {
        return Collections.unmodifiableCollection(functions.values());
    }

    public Class getImplClass() {
        return implClass;
    }
}

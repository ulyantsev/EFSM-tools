/**
 * ${NAME}.java, 13.03.2008
 */
package ru.ifmo.automata.statemachine.impl;

import ru.ifmo.automata.statemachine.*;
import ru.ifmo.automata.statemachine.io.IAutomataReader;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class AutomataContext implements IAutomataContext {

    private Map<String, IControlledObject> ctrlObjects = new HashMap<String, IControlledObject>();
    private Map<String, IEventProvider> eventProviders = new HashMap<String, IEventProvider>();
    private Map<String, IStateMachine<? extends IState>> stateMachines
            = new HashMap<String, IStateMachine<? extends IState>>();

    public AutomataContext() {
    }

    /**
     * Create new automata context instance and close <code>reader</code>.
     * @param reader
     * @throws AutomataFormatException
     * @throws IOException
     */
    public AutomataContext(IAutomataReader reader) throws AutomataFormatException, IOException {
        try {
            putAll(reader);
        } catch (AutomataFormatException e) {
            reader.close();
        }

    }

    public IControlledObject getControlledObject(String name) {
        return ctrlObjects.get(name);
    }

    public IEventProvider getEventProvider(String name) {
        return eventProviders.get(name);
    }

    public IStateMachine<? extends IState> getStateMachine(String name) {
        return stateMachines.get(name);
    }

    public void putControlledObject(String name, IControlledObject o) {
        ctrlObjects.put(name, o);
    }

    public void putEventProvider(String name, IEventProvider p) {
        eventProviders.put(name, p);
    }

    public void putStateMachine(String name, IStateMachine<? extends IState> m) {
        stateMachines.put(name, m);
    }

    public void putAll(IAutomataReader reader) throws AutomataFormatException {
        ctrlObjects.putAll(reader.readControlledObjects());
        stateMachines.putAll(reader.readStateMachines());
        eventProviders.putAll(reader.readEventProviders());
    }
}

/*
 * Developed by eVelopers Corporation - 25.06.2008
 */
package ru.ifmo.automata.statemachine.io;

import ru.ifmo.automata.statemachine.*;
import ru.ifmo.automata.statemachine.impl.*;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamConstants;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLClassLoader;
import java.net.URL;

import static ru.ifmo.automata.statemachine.StateMachineUtils.*;

public class StateMachineReader implements IAutomataReader {

    private StateMachine<State> rootStateMachine;
    private Map<String, StateMachine<State>> stateMachines;
    private Map<String, IEventProvider> eventProviders;
    private Map<String, IControlledObject> ctrlObjects;

    private XMLStreamReader reader;

    public StateMachineReader(String fileLocation) throws IOException {
        URLClassLoader urlLoader = (URLClassLoader) getClass().getClassLoader();
        URL fileLoc = urlLoader.findResource(fileLocation);

        if (fileLoc == null) {
            throw new FileNotFoundException("File not found: " + fileLocation);
        }

        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            reader = inputFactory.createXMLStreamReader(fileLoc.openStream());
        } catch (XMLStreamException e) {
            throw new IOException("Can't parse " + fileLocation, e);
        }
    }

    public StateMachineReader(File file) throws IOException {
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

            reader = inputFactory.createXMLStreamReader(new FileInputStream(file));
        } catch (XMLStreamException e) {
            throw new IOException("Cant parse " + file.getPath(), e);
        }
    }

    public void close() throws IOException {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void init() {
        if (rootStateMachine == null) {
            ctrlObjects = new HashMap<String, IControlledObject>();
            eventProviders = new HashMap<String, IEventProvider>();
            stateMachines = new HashMap<String, StateMachine<State>>();
        }
    }

    public IStateMachine<? extends IState> readRootStateMachine() throws AutomataFormatException {
        try {
            init();
            readAll();
            return rootStateMachine;
        } catch (XMLStreamException e) {
            throw new AutomataFormatException(e);
        } catch (ClassNotFoundException e) {
            throw new AutomataFormatException(e);
        }
    }

    public Map<String, IEventProvider> readEventProviders() throws AutomataFormatException {
        try {
            init();
            readAll();
            return eventProviders;
        } catch (XMLStreamException e) {
            throw new AutomataFormatException(e);
        } catch (ClassNotFoundException e) {
            throw new AutomataFormatException(e);
        }
    }

    public Map<String, IControlledObject> readControlledObjects() throws AutomataFormatException {
        try {
            init();
            readAll();
            return ctrlObjects;
        } catch (XMLStreamException e) {
            throw new AutomataFormatException(e);
        } catch (ClassNotFoundException e) {
            throw new AutomataFormatException(e);
        }
    }

    public Map<String, ? extends IStateMachine<? extends IState>> readStateMachines() throws AutomataFormatException {
        try {
            init();
            readAll();
            return stateMachines;
        } catch (XMLStreamException e) {
            throw new AutomataFormatException(e);
        } catch (ClassNotFoundException e) {
            throw new AutomataFormatException(e);
        }
    }

    protected void readAll() throws XMLStreamException, ClassNotFoundException, AutomataFormatException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    String elementName = reader.getName().toString();
                    if (CTRL_OBJECT.equals(elementName)) {
                        parseControlledObject();
                    } else if (EVENT_PROVIDER.equals(elementName)) {
                        parseEventProvider();
                    } else if (STATE_MACHINE_REF.equals(elementName)) {
                        parseRootStateMachine();
                    } else if (STATE_MACHINE.equals(elementName)) {
                        parseStateMachine();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void parseRootStateMachine() {
        assert STATE_MACHINE_REF.equals(reader.getName().toString());

        String name = reader.getAttributeValue(null, ATTR_NAME);
        rootStateMachine = getStateMachine(name);
    }

    private void parseStateMachine() throws XMLStreamException, AutomataFormatException {
        assert STATE_MACHINE.equals(reader.getName().toString());

        String name = reader.getAttributeValue(null, ATTR_NAME);
        StateMachine<State> sm = getStateMachine(name);

        int startCount = 1;
        while (startCount > 0) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    startCount++;
                    String elementName = reader.getName().toString();
                    if (TRANSITION.equals(elementName)) {
                        parseTransition(sm);
                        startCount--;
                    } else if (ASSOCIATION.equals(elementName)) {
                        parseCtrlObjAssociation(sm);
                    } else if (STATE.equals(elementName)) {
                        parseStates(sm);
                        startCount--;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    startCount--;
                    break;
                default:
                    break;
            }
        }

        if (!STATE_MACHINE.equals(reader.getName().toString())) {
            throw new AutomataFormatException("Unexpected end tag: " + reader.getName());
        }
    }

    private void parseControlledObject() throws ClassNotFoundException {
        assert CTRL_OBJECT.equals(reader.getName().toString());

        String name = reader.getAttributeValue(null, ATTR_NAME);
        String className = reader.getAttributeValue(null, ATTR_CLASS);
        Class aClass = Class.forName(className);
        ctrlObjects.put(name, new ControlledObject(name, aClass));
    }

    private void parseEventProvider() throws ClassNotFoundException, XMLStreamException, AutomataFormatException {
        assert EVENT_PROVIDER.equals(reader.getName().toString());

        String name = reader.getAttributeValue(null, ATTR_NAME);
        String className = reader.getAttributeValue(null, ATTR_CLASS);
        Class aClass = Class.forName(className);
        EventProvider eProvider = new EventProvider(name, aClass);
        eventProviders.put(name, eProvider);

        int startCount = 1;
        while (startCount > 0) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    startCount++;
                    if (ASSOCIATION.equals(reader.getName().toString())) {
                        String smName = reader.getAttributeValue(null, ATTR_TARGET);

                        getStateMachine(smName).addEventProvider(eProvider);
                    } else {
                        throw new AutomataFormatException("Unexpected element name: " + reader.getName());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    startCount--;
                    break;
                default:
                    break;
            }
        }
        if (!EVENT_PROVIDER.equals(reader.getName().toString())) {
            throw new AutomataFormatException("Unexpected end tag: " + reader.getName());
        }
    }

    private void parseStates(StateMachine<State> sm) throws XMLStreamException, AutomataFormatException {
        assert STATE.equals(reader.getName().toString());
        assert "Top".equals(reader.getAttributeValue(null, ATTR_NAME));

        int startCount = 1;
        while (startCount > 0) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
//                    startCount++;
                    if (STATE.equals(reader.getName().toString())) {
                        parseSingleState(sm);
                    } else {
                        throw new AutomataFormatException("Unexpected tag: " + reader.getName());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    startCount--;
                    break;
                default:
                    break;
            }
        }
        if (!STATE.equals(reader.getName().toString())) {
            throw new AutomataFormatException("Unexpected end tag: " + reader.getName());
        }
    }

    private void parseSingleState(StateMachine<State> sm) throws XMLStreamException, AutomataFormatException {
        assert STATE.equals(reader.getName().toString());

        String name = reader.getAttributeValue(null, ATTR_NAME);
        String type = reader.getAttributeValue(null, ATTR_TYPE);
        List<IAction> actions = new ArrayList<IAction>();
        State state = new State(name, StateType.getByName(type), actions);

        int startCount = 1;
        while (startCount > 0) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    startCount++;
                    String elementName = reader.getName().toString();
                    if (OUT_ACTION.equals(elementName)) {
                        String actionFullName = reader.getAttributeValue(null, ATTR_ACTION);
                        actions.add(StateMachineUtils.extractAction(actionFullName, sm));
                    } else if (STATE_MACHINE_REF.equals(elementName)) {
                        String smName = reader.getAttributeValue(null, ATTR_NAME);
                        StateMachine<State> nested = getStateMachine(smName);
                        state.addNestedStateMachine(nested);
                        nested.setParent(sm, state);
                        sm.addNestedStateMachine(nested);
                    } else {
                        throw new AutomataFormatException("Unexpected tag: " + reader.getName());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    startCount--;
                    break;
                default:
                    break;
            }
        }

        sm.addState(state);

        if (!STATE.equals(reader.getName().toString())) {
            throw new AutomataFormatException("Unexpected end tag: " + reader.getName());
        }
    }

    private void parseTransition(StateMachine<State> sm) throws XMLStreamException, AutomataFormatException {
        assert TRANSITION.equals(reader.getName().toString());

        String eventFullName = reader.getAttributeValue(null, ATTR_EVENT);
        String condExpr      = reader.getAttributeValue(null, ATTR_COND);
        String source        = reader.getAttributeValue(null, ATTR_SOURCE);
        String target        = reader.getAttributeValue(null, ATTR_TARGET);

        State stateSource = sm.getState(source);
        State stateTarget = sm.getState(target);

        IEvent event = StateMachineUtils.parseEvent(sm, eventProviders, eventFullName);
        Transition t = new Transition(event, new Condition(condExpr), stateTarget);

        int startCount = 1;
        while (startCount > 0) {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    startCount++;
                    if (OUT_ACTION.equals(reader.getName().toString())) {
                        String actionFullName = reader.getAttributeValue(null, ATTR_ACTION);
                        t.addAction(StateMachineUtils.extractAction(actionFullName, sm));
                    } else {
                        throw new AutomataFormatException("Unexpected tag: " + reader.getName());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    startCount--;
                    break;
                default:
                    break;
            }
        }
        stateSource.addOutcomingTransition(t);

        if (!TRANSITION.equals(reader.getName().toString())) {
            throw new AutomataFormatException("Unexpected end tag: " + reader.getName());
        }
    }

    private void parseCtrlObjAssociation(StateMachine<State> sm) {
        String role = reader.getAttributeValue(null, ATTR_SUPPLIER_ROLE);
        String target = reader.getAttributeValue(null, ATTR_TARGET);

        IControlledObject ctrl = ctrlObjects.get(target);
        if (ctrl != null) {
            sm.addControlledObject(role, ctrl);
        }
    }

    protected StateMachine<State> getStateMachine(String name) {
        StateMachine<State> sm = stateMachines.get(name);
        if (sm == null) {
            sm = new StateMachine<State>(name);
            stateMachines.put(name, sm);
        }
        return sm;
    }
}
/*
 * Developed by eVelopers Corporation - 26.06.2008
 */
package ru.ifmo.automata.statemachine;

import ru.ifmo.automata.statemachine.impl.AutomataFormatException;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class StateMachineUtils {
    private static final String ANY_EVENT = "*";

    public static final String STATE_MACHINE = "stateMachine";
    public static final String ROOT_STATE_MACHINE = "rootStateMachine";
    public static final String CTRL_OBJECT = "controlledObject";
    public static final String EVENT_PROVIDER = "eventProvider";
    public static final String ASSOCIATION = "association";
    public static final String STATE = "state";
    public static final String OUT_ACTION = "outputAction";
    public static final String TRANSITION = "transition";
    public static final String STATE_MACHINE_REF = "stateMachineRef";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_CLASS = "class";
    public static final String ATTR_TARGET = "targetRef";
    public static final String ATTR_SOURCE = "sourceRef";
    public static final String ATTR_ACTION = "ident";
    public static final String ATTR_EVENT = "event";
    public static final String ATTR_COND = "guard";
    public static final String ATTR_SUPPLIER_ROLE = "supplierRole";

    public static final String METHOD_PATTERN = "\\p{Alpha}\\w*\\.\\p{Alpha}\\w*";

    private StateMachineUtils() {}

    public static IAction extractAction(String actionFullName, IStateMachine<? extends IState> sm)
            throws AutomataFormatException {
        if (!actionFullName.matches(METHOD_PATTERN)) {
            throw new AutomataFormatException("Wrong output action format: " + actionFullName);
        }
        int pointIndx = actionFullName.indexOf('.');
        String ctrlName = actionFullName.substring(0, pointIndx);
        String actionName = actionFullName.substring(pointIndx + 1);
        IControlledObject ctrlObj = sm.getControlledObject(ctrlName);
        if (ctrlObj == null) {
            throw new AutomataFormatException("Unknown controlled object: "+ ctrlName);
        }
        IAction action = ctrlObj.getAction(actionName);
        if (action == null) {
            throw new AutomataFormatException("Unknown action name: "+ actionName);
        }
        return action;
    }

    /**
     * Extract event provider name and event name from eventAttr.
     * @param m state machine
     * @param eventProviders event providers map
     * @param eventAttr event full qualifier
     * @return IEvent instance or null if event Attr is blank
     * @throws AutomataFormatException
     */
    public static IEvent parseEvent(IStateMachine<? extends IState> m,
                                    Map<String, IEventProvider> eventProviders,
                                    String eventAttr) throws AutomataFormatException {
        if (eventAttr == null) {
            return null;
        }
        String[] a = eventAttr.split("\\.");
        if (a.length > 2) {
            throw new AutomataFormatException("Wrong event format: " + eventAttr);
        }
        IEvent event;
        if (a.length == 2) {
            IEventProvider provider = eventProviders.get(a[0]);
            if (provider == null) {
                throw new AutomataFormatException("Unknown event provider: " + a[0]);
            }
            event = provider.getEvent(a[1]);
            if (event == null) {
                throw new AutomataFormatException("Unknown event name: " + a[1]);
            }
        } else if (StringUtils.isNotBlank(eventAttr)) {
            //try to find event in state machine's event providers set
            event = findEventByName(m, eventAttr);
            if (event != null) {
                return event;
            }
            if (ANY_EVENT.equals(eventAttr)) {
                return null;
            }
        } else {
            return null;
        }

        throw new AutomataFormatException(String.format("Unknown event %s in state machine %s", eventAttr, m.getName()));
    }

    private static <M extends IStateMachine<? extends IState>> IEvent findEventByName(M m, String eventName) {
        for (IEventProvider p: m.getEventProviders()) {
            IEvent event = p.getEvent(eventName);
            if (event != null) {
                return event;
            }
        }
        IStateMachine<? extends IState> parent = m.getParentStateMachine();
        return (parent != null) ? findEventByName(parent, eventName) : null;
    }
}

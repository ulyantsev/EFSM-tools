/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.verifier;

import ru.ifmo.automata.statemachine.IState;

import java.util.List;
import java.util.ArrayList;

/**
 * @author kegorov
 *         Date: Nov 3, 2009
 */
public abstract class NotifiableDfs<R> implements IDfs<R> {

    private List<IDfsListener> listeners;

    protected void notifyEnterState(IState s) {
        if (listeners != null) {
            for (IDfsListener l : listeners) {
                l.enterState(s);
            }
        }
    }

    protected void notifyLeaveState(IState s) {
        if (listeners != null) {
            for (IDfsListener l : listeners) {
                l.leaveState(s);
            }
        }
    }

    public void add(IDfsListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<IDfsListener>();
        }
        listeners.add(listener);
    }

    public void remove(IDfsListener listener) {
        listeners.remove(listener);
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.ctddev.genetic.transducer.verifier;

import ru.ifmo.verifier.IDfsListener;
import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.automata.statemachine.IStateTransition;
import ru.ifmo.ctddev.genetic.transducer.algorithm.Transition;

import java.util.Collection;

/**
 * @author kegorov
 *         Date: Dec 10, 2009
 */
public class TransitionMarker implements IDfsListener {
    public void enterState(IState state) {
        setTransitionsVerified(state, false);
    }

    public void leaveState(IState state) {
        setTransitionsVerified(state, true);
    }

    private void setTransitionsVerified(IState state, boolean verified) {
        for (IStateTransition t : state.getOutcomingTransitions()) {
            if (t instanceof AutomataTransition) {
                Transition algTransition = ((AutomataTransition) t).getAlgTransition();
                if (algTransition != null) {
                    algTransition.setVerified(verified);
                }
            }
        }
    }
}

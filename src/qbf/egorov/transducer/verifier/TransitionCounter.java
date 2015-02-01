/* 
 * Developed by eVelopers Corporation, 2010
 */
package qbf.egorov.transducer.verifier;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateTransition;
import qbf.egorov.transducer.Transition;
import qbf.egorov.verifier.IDfsListener;

import java.util.Map;
import java.util.HashMap;

/**
 * @author kegorov
 *         Date: Feb 19, 2010
 */
public class TransitionCounter implements IDfsListener {
    private Map<Transition, Boolean> transitions = new HashMap<>();

    public void enterState(IState state) {
    }

    public void leaveState(IState state) {
        markTransitions(state, true);
    }

    public void resetCounter() {
        transitions.clear();
    }

    /**
     * Count verified transitions;
     * @return
     */
    public int countVerified() {
        return transitions.size();
    }

    private void markTransitions(IState state, boolean leave) {
        for (IStateTransition t : state.getOutcomingTransitions()) {
            if (t instanceof AutomataTransition) {
                Transition algTransition = ((AutomataTransition) t).getAlgTransition();
                if (algTransition != null) {
                    transitions.put(algTransition, leave);
                }
            }
        }
    }
}

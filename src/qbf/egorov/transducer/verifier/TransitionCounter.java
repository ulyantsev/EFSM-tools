/* 
 * Developed by eVelopers Corporation, 2010
 */
package qbf.egorov.transducer.verifier;

import java.util.LinkedHashSet;
import java.util.Set;

import qbf.egorov.statemachine.IState;
import qbf.egorov.transducer.Transition;

/**
 * @author kegorov
 *         Date: Feb 19, 2010
 */
public class TransitionCounter {
    private final Set<Transition> transitions = new LinkedHashSet<>();

    public void leaveState(IState state) {
    	state.getOutcomingTransitions().stream()
    		.filter(t -> t instanceof AutomataTransition)
    		.map(t -> ((AutomataTransition) t).getAlgTransition())
    		.filter(t -> t != null)
    		.forEach(transitions::add);
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
}

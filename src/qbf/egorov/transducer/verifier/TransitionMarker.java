/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.verifier;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateTransition;
import qbf.egorov.transducer.Transition;
import qbf.egorov.verifier.IDfsListener;

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

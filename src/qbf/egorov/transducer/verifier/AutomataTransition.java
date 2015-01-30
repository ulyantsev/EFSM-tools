/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.verifier;

import qbf.egorov.statemachine.ICondition;
import qbf.egorov.statemachine.IEvent;
import qbf.egorov.statemachine.IState;
import qbf.egorov.transducer.algorithm.Transition;

/**
 * @author kegorov
 *         Date: Jul 23, 2009
 */
public class AutomataTransition extends qbf.egorov.statemachine.impl.Transition {
    private Transition algTransition;

    public AutomataTransition(IEvent iEvent, ICondition iCondition, IState iState) {
        super(iEvent, iCondition, iState);
    }

    public Transition getAlgTransition() {
        return algTransition;
    }

    public void setAlgTransition(Transition algTransition) {
        this.algTransition = algTransition;
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.ctddev.genetic.transducer.verifier;

import ru.ifmo.automata.statemachine.IEvent;
import ru.ifmo.automata.statemachine.ICondition;
import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.ctddev.genetic.transducer.algorithm.Transition;

/**
 * @author kegorov
 *         Date: Jul 23, 2009
 */
public class AutomataTransition extends ru.ifmo.automata.statemachine.impl.Transition {
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

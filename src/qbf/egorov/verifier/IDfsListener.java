/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.verifier;

import qbf.egorov.statemachine.IState;

/**
 * @author kegorov
 *         Date: Nov 2, 2009
 */
public interface IDfsListener {
    void enterState(IState state);
    void leaveState(IState state);
}

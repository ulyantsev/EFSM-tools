/* 
 * Developed by eVelopers Corporation, 2010
 */
package qbf.egorov.transducer.algorithm.util;

import qbf.egorov.transducer.algorithm.Transition;

/**
 * @author kegorov
 *         Date: May 6, 2010
 */
public interface ITransitionChecker {

    boolean isMarked(Transition t);
}

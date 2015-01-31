/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.verifier;

import java.util.List;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.transducer.algorithm.FST;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public interface IVerifierFactory {

    void prepareFormulas(List<String> formulas) throws LtlParseException;

    void configureStateMachine(FST fst);

    /**
     * Verify prepared formulas.
     * @return number of marked transitions per group
     */
    int[] verify();
}

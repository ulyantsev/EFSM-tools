/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.io;

import qbf.egorov.transducer.algorithm.Parameters;
import qbf.egorov.transducer.scenario.TestGroup;

import java.util.List;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public interface ITestsReader {
    List<TestGroup> getGroups();

    String[] getSetOfInputs();

    String[] getSetOfOutputs();

    Parameters getAlgorithmParameters();
}

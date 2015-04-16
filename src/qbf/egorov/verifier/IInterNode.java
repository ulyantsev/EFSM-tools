/**
 * IInterNode.java, 12.04.2008
 */
package qbf.egorov.verifier;

import qbf.egorov.ltl.buchi.IBuchiNode;
import qbf.egorov.statemachine.IState;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IInterNode {
    IState getState();
    IBuchiNode getNode();
    int getAcceptSet();
}

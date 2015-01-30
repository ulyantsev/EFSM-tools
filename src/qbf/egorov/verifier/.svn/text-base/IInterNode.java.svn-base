/**
 * IInterNode.java, 12.04.2008
 */
package ru.ifmo.verifier;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.ltl.buchi.IBuchiNode;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IInterNode {

    IState getState();

    IBuchiNode getNode();

    int getAcceptSet();

    void addOwner(int threadId);

    void removeOwner(int threadId);

    boolean isOwner(int threadId);
}

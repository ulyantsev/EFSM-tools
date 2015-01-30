/**
 * INode.java, 12.04.2008
 */
package qbf.egorov.automata;

import java.util.Collection;

/**
 * The automata node
 *
 * @author Kirill Egorov
 */
public interface INode<T extends ITransition> {

    boolean isTerminal();
    Collection<T> getOutcomingTransitions();
}

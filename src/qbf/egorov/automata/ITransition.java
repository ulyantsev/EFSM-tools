/**
 * ITransition.java, 12.04.2008
 */
package qbf.egorov.automata;

/**
 * Transition between sourse node and target
 *
 * @author Kirill Egorov
 */
public interface ITransition<N extends INode<?>> {
    N getTarget();
}

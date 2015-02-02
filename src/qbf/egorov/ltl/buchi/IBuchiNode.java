/**
 * INode.java, 16.03.2008
 */
package qbf.egorov.ltl.buchi;

import java.util.Map;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IBuchiNode {
    int getID();
    Map<ITransitionCondition, IBuchiNode> getTransitions();
}

/**
 * IControlledObject.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import java.util.Collection;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IControlledObject {
    String getName();
    IAction getAction(String actionName);
    IFunction getFunction(String funName);
    Collection<IFunction> getFunctions();
}

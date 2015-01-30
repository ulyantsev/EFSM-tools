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

    public String getName();

    public IAction getAction(String actionName);

    public Collection<IAction> getActions();

    public IFunction getFunction(String funName);

    public Collection<IFunction> getFunctions();

    public Class getImplClass();
}

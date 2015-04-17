/**
 * IControlledObject.java, 01.03.2008
 */
package qbf.egorov.statemachine;

import qbf.egorov.statemachine.impl.Action;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IControlledObject {
    String getName();
    Action getAction(String actionName);
}

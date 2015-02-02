/**
 * Condition.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.statemachine.ICondition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Condition implements ICondition {
    private String expr;
    
    public String getExpression() {
        return expr;
    }
}

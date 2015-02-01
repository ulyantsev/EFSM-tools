/**
 * Condition.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import qbf.egorov.ognl.Ognl;
import qbf.egorov.ognl.OgnlException;
import qbf.egorov.statemachine.ICondition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Condition implements ICondition {
    private String expr;
    private Object tree;

    public String getExpression() {
        return expr;
    }

    public boolean evaluate(Object source) {
        try {
            if (expr == null) {
                return (Boolean) Ognl.getValue(tree, source, Boolean.class);
            } else {
                return true;
            }
        } catch (OgnlException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }

    //TODO: override equals() and hashCode()
}

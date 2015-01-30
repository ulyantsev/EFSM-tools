/**
 * Condition.java, 02.03.2008
 */
package qbf.egorov.statemachine.impl;

import org.apache.commons.lang3.StringUtils;

import qbf.egorov.statemachine.ICondition;
import ognl.Ognl;
import ognl.OgnlException;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Condition implements ICondition {
    private String expr;
    private Object tree;

    public Condition(String expr) {
        this.expr = expr;
        try {
            if (StringUtils.isNotBlank(expr)) {
                tree = Ognl.parseExpression(expr);
            } else {
                this.expr = null;
            }
        } catch (OgnlException e) {
            throw new ParseException(e);
        }
    }

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

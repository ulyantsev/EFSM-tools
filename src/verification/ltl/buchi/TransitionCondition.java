/**
 * TransitionCondition.java, 16.03.2008
 */
package verification.ltl.buchi;

import verification.ltl.grammar.IExpression;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class TransitionCondition {
    private final Set<IExpression<Boolean>> exprs;
    private final Set<IExpression<Boolean>> negExprs;

    public TransitionCondition() {
        exprs = new LinkedHashSet<>();
        negExprs = new LinkedHashSet<>();
    }
    
    public Set<IExpression<Boolean>> expressions() {
        return Collections.unmodifiableSet(exprs);
    }
    
    public Set<IExpression<Boolean>> negativeExpressions() {
        return Collections.unmodifiableSet(negExprs);
    }

    public boolean getValue() {
        for (IExpression<Boolean> expr: exprs) {
            if (expr.getValue() == null || !expr.getValue()) {
                return false;
            }
        }
        for (IExpression<Boolean> expr: negExprs) {
            if (expr.getValue() == null || expr.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void addExpression(IExpression<Boolean> expr) {
        exprs.add(expr);
    }

    public void addNegExpression(IExpression<Boolean> expr) {
        negExprs.add(expr);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (IExpression<Boolean> expr: exprs) {
            buf.append(expr).append(" && ");
        }
        for (IExpression<Boolean> expr: negExprs) {
            buf.append("!").append(expr).append(" && ");
        }
        if (exprs.isEmpty() && negExprs.isEmpty()) {
            return "true";
        } else {
            return buf.substring(0, buf.length() - 4);
        }
    }
}

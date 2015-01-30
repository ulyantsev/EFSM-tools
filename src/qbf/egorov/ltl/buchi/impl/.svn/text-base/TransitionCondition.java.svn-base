/**
 * TransitionCondition.java, 16.03.2008
 */
package ru.ifmo.ltl.buchi.impl;

import ru.ifmo.ltl.buchi.ITransitionCondition;
import ru.ifmo.ltl.grammar.IExpression;

import java.util.Set;
import java.util.HashSet;

import org.apache.commons.lang.BooleanUtils;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class TransitionCondition implements ITransitionCondition {
    private Set<IExpression<Boolean>> exprs;
    private Set<IExpression<Boolean>> negExprs;

    public TransitionCondition() {
        exprs = new HashSet<IExpression<Boolean>>();
        negExprs = new HashSet<IExpression<Boolean>>();
    }

    public TransitionCondition(Set<IExpression<Boolean>> exprs, Set<IExpression<Boolean>> negExprs) {
        this.exprs = exprs;
        this.negExprs = negExprs;
    }

    public Set<IExpression<Boolean>> getExpressions() {
        return exprs;
    }

    public Set<IExpression<Boolean>> getNegExpressions() {
        return negExprs;
    }

    public boolean getValue() {
        for (IExpression<Boolean> expr: exprs) {
            if (BooleanUtils.isNotTrue(expr.getValue())) {
                return false;
            }
        }
        for (IExpression<Boolean> expr: negExprs) {
            if (BooleanUtils.isNotFalse(expr.getValue())) {
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
        for (IExpression expr: exprs) {
            buf.append(expr).append(" && ");
        }
        for (IExpression expr: negExprs) {
            buf.append("!").append(expr).append(" && ");
        }
        if (exprs.isEmpty() && negExprs.isEmpty()) {
            return "true";
        } else {
            return buf.substring(0, buf.length() - 4);
        }
    }
}

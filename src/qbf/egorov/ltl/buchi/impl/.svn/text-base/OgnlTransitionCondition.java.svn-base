/*
 * Developed by eVelopers Corporation - 27.05.2008
 */
package ru.ifmo.ltl.buchi.impl;

import ru.ifmo.ltl.buchi.ITransitionCondition;
import ru.ifmo.ltl.grammar.IExpression;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import ognl.*;
import org.apache.commons.lang.BooleanUtils;

public class OgnlTransitionCondition implements ITransitionCondition {

    static {
        OgnlRuntime.setNullHandler(ExpressionMap.class, new NullHandler() {
            public Object nullMethodResult(Map context, Object target, String methodName, Object[] args) {
                return null;
            }

            public Object nullPropertyValue(Map context, Object target, Object property) {
                throw new NullExpressionException();
            }
        });
        OgnlRuntime.setPropertyAccessor(ExpressionMap.class, new MapPropertyAccessor() {
            public Object getProperty(Map context, Object target, Object name) throws OgnlException {
                Object res = super.getProperty(context, target, name);
                if (res instanceof IExpression) {
                    return ((IExpression) res).getValue();
                }
                return res;
            }

            public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
                throw new UnsupportedOperationException();
            }
        });
    }

    private String cond;
    private Object tree;
    private Map root = new HashMap();

    public OgnlTransitionCondition(String cond, ExpressionMap exprs) throws OgnlException {
        this.cond = cond.trim();
        tree = Ognl.parseExpression(this.cond);
        this.root = exprs;
    }

    @Deprecated
    public Set<IExpression<Boolean>> getExpressions() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public Set<IExpression<Boolean>> getNegExpressions() {
        throw new UnsupportedOperationException();
    }

    public boolean getValue() {
        try {
            Boolean res = (Boolean) Ognl.getValue(tree, (Object) root, Boolean.class);
            return BooleanUtils.isTrue(res);
        } catch (OgnlException e) {
            throw new RuntimeException(e);
        } catch (NullExpressionException e) {
            return false;
        }
    }

    public String toString() {
        return cond;
    }
}

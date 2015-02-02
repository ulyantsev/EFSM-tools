/*
 * Developed by eVelopers Corporation - 27.05.2008
 */
package qbf.egorov.ltl.buchi.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;

import qbf.egorov.ltl.buchi.ITransitionCondition;
import qbf.egorov.ltl.grammar.IExpression;
import qbf.egorov.ognl.MapPropertyAccessor;
import qbf.egorov.ognl.NullHandler;
import qbf.egorov.ognl.Ognl;
import qbf.egorov.ognl.OgnlException;
import qbf.egorov.ognl.OgnlRuntime;

public class OgnlTransitionCondition implements ITransitionCondition {
    static {
        OgnlRuntime.setNullHandler(ExpressionMap.class, new NullHandler() {
            public Object nullMethodResult(@SuppressWarnings("rawtypes") Map context, Object target, String methodName, Object[] args) {
                return null;
            }

            public Object nullPropertyValue(@SuppressWarnings("rawtypes") Map context, Object target, Object property) {
                throw new NullExpressionException();
            }
        });
        OgnlRuntime.setPropertyAccessor(ExpressionMap.class, new MapPropertyAccessor() {
            public Object getProperty(@SuppressWarnings("rawtypes") Map context, Object target, Object name) throws OgnlException {
                Object res = super.getProperty(context, target, name);
                if (res instanceof IExpression) {
                    return ((IExpression<?>) res).getValue();
                }
                return res;
            }

            public void setProperty(@SuppressWarnings("rawtypes") Map context, Object target, Object name, Object value) throws OgnlException {
                throw new UnsupportedOperationException();
            }
        });
    }

    private String cond;
    private Object tree;
    private Map<?, ?> root = new HashMap<>();

    public OgnlTransitionCondition(String cond, ExpressionMap exprs) throws OgnlException {
        this.cond = cond.trim();
        tree = Ognl.parseExpression(this.cond);
        this.root = exprs;
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

    @Override
    public String toString() {
        return cond;
    }
}

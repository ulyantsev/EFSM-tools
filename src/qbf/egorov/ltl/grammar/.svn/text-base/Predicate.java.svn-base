/**
 * Predicate.java, 12.03.2008
 */
package ru.ifmo.ltl.grammar;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Predicate extends LtlNode implements IExpression<Boolean> {
    protected Object[] args;
    protected Method method;
    protected Object target;

    public Predicate(Object target, Method m, Object ... args) {
        super(m.getName());
        if (!boolean.class.equals(m.getReturnType()) && !Boolean.class.equals(m.getReturnType())) {
            throw new IllegalArgumentException("Method should return boolean type");
        }
        Class<?>[] params = m.getParameterTypes();
        try {
            if (target != null && !target.getClass().getMethod(m.getName(), params).equals(m)) {
                throw new IllegalArgumentException("Method isn't from this object");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Object hasn't such method");
        }
        if (params.length != args.length) {
            throw new IllegalArgumentException("Wrong arguments count");
        }
        int i = 0;
        for (Object a: args) {
            if ((a != null) && !params[i].isAssignableFrom(a.getClass())) {
                throw new IllegalArgumentException("Wrong argument's type: " + a.getClass());
            }
            i++;
        }
        this.args = args;
        this.method = m;
        this.target = target;

    }

    public Boolean getValue() {
        try {
            return (Boolean) method.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Predicate) {
            Predicate p = (Predicate) obj;
            if (method.equals(p.method) && target.equals(p.target) && (args.length == p.args.length)) {
                for (int i = 0; i < args.length; i++) {
                    if (!args[i].equals(p.args[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return method.hashCode();
    }

    public String toString() {
        StringBuilder buf = new StringBuilder(method.getName());
        buf.append("(");
        for (Object obj: args) {
            buf.append(obj).append(", ");
        }
        if (args.length > 0) {
            buf.replace(buf.length() - 2, buf.length(), ")");
        } else {
            buf.append(")");
        }
        return buf.toString();
    }

    public String getUniqueName() {
        StringBuilder buf = new StringBuilder();
        buf.append(method.getName());
        for (Object o: args) {
            buf.append(o);
        }
        return buf.toString().replace(' ', '_');
    }

    public <R, D> R accept(INodeVisitor<R, D> visitor, D data) {
        return visitor.visitPredicate(this, data);
    }
}

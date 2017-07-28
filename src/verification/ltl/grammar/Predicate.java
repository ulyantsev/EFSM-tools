/**
 * Predicate.java, 12.03.2008
 */
package verification.ltl.grammar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class Predicate extends LtlNode implements IExpression<Boolean> {
    private final String arg;
    private final Method method;
    private final Object target;

    /*
     * For simplified usage.
     */
    public String arg() {
        return arg;
    }
    
    public Predicate(Object target, Method m, String arg) {
        super(m.getName());
        
        if (arg.startsWith("ep.") || arg.startsWith("co.")) {
            arg = arg.substring(3);
        }
        
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
        this.arg = arg;
        this.method = m;
        this.target = target;
    }

    public Boolean getValue() {
        try {
            return (Boolean) method.invoke(target, arg);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Predicate) {
            final Predicate p = (Predicate) obj;
            return method.equals(p.method) && target.equals(p.target) && arg.equals(p.arg);
        }
        return false;
    }

    public int hashCode() {
        return method.hashCode();
    }

    @Override
    public String toString() {
        return (method == null ? getName() : method.getName()) + "(" + arg + ")";
    }

    public String getUniqueName() {
        return (method.getName() + arg).replace(' ', '_');
    }

    public <R, D> R accept(INodeVisitor<R, D> visitor, D data) {
        return visitor.visitPredicate(this, data);
    }
}

/**
 * Operator.java, 12.03.2008
 */
package verification.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public abstract class Operator<E extends IOperatorType> extends LtlNode {
    private E type;

    Operator(E type) {
        super(type.getName());
        this.type = type;
    }

    public E getType() {
        return type;
    }
}

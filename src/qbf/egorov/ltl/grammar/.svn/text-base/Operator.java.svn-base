/**
 * Operator.java, 12.03.2008
 */
package ru.ifmo.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public abstract class Operator<E extends IOperatorType> extends LtlNode {

    private E type;

    public Operator(E type) {
        super(type.getName());
        this.type = type;
    }

    public E getType() {
        return type;
    }
}

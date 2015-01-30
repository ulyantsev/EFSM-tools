/**
 * ConstNode.java, 22.03.2008
 */
package ru.ifmo.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class BooleanNode extends LtlNode implements IExpression<Boolean> {
    public static final BooleanNode TRUE = new BooleanNode("true");
    public static final BooleanNode FALSE = new BooleanNode("false");

    public static BooleanNode getByName(String name) {
        if (TRUE.getName().equalsIgnoreCase(name)) {
            return TRUE;
        } else if (FALSE.getName().equalsIgnoreCase(name)) {
            return FALSE;
        }
        return null;
    }

    private BooleanNode(String name) {
        super(name);
    }

    public Boolean getValue() {
        return TRUE.equals(this);
    }

    public <R, D> R accept(INodeVisitor<R, D> visitor, D data) {
        return visitor.visitBoolean(this, data);
    }
}

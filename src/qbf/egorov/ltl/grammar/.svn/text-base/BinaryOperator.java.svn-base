/**
 * BinaryOperator.java, 11.03.2008
 */
package ru.ifmo.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class BinaryOperator extends Operator<BinaryOperatorType> {

    private LtlNode leftOperand;
    private LtlNode rightOperand;

    public BinaryOperator(BinaryOperatorType type) {
        super(type);
    }

    public LtlNode getLeftOperand() {
        return leftOperand;
    }

    public void setLeftOperand(LtlNode leftOperand) {
        this.leftOperand = leftOperand;
    }

    public LtlNode getRightOperand() {
        return rightOperand;
    }

    public void setRightOperand(LtlNode rightOperand) {
        this.rightOperand = rightOperand;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof BinaryOperator) {
            BinaryOperator op = (BinaryOperator) obj;
            if (getType() == op.getType()) {
                return equals(leftOperand.equals(op.leftOperand) && rightOperand.equals(op.rightOperand));
            }
        }
        return false;
    }

    public int hashCode() {
        return getName().hashCode() ^ leftOperand.getName().hashCode() ^ rightOperand.getName().hashCode();
    }

    public <R, D> R accept(INodeVisitor<R, D> visitor, D data) {
        return getType().accept(this, visitor, data);
    }
}

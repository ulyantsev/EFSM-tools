/**
 * BinaryOperator.java, 11.03.2008
 */
package qbf.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public class BinaryOperator extends Operator<BinaryOperatorType> {
    private LtlNode leftOperand;
    private LtlNode rightOperand;

    public BinaryOperator(BinaryOperatorType type) {
        super(type);
    }
    
    public BinaryOperator(BinaryOperatorType type, LtlNode leftOperand, LtlNode rightOperand) {
        super(type);
        setLeftOperand(leftOperand);
        setRightOperand(rightOperand);
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

	@Override
	public String toFullString() {
		return "(" + leftOperand.toFullString() + " " + toString() + " " + rightOperand.toFullString() + ")"; 
	}
}

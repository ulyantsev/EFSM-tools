/**
 * BinaryOperator.java, 11.03.2008
 */
package qbf.ltl;

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

	@Override
	public String toString() {
		return "(" + leftOperand + " " + getName() + " " + rightOperand + ")"; 
	}
}

/**
 * UnaryOperator.java, 11.03.2008
 */
package qbf.ltl;

/**
 * TODO: add comment
 *
 * @author: Kirill Egorov
 */
public class UnaryOperator extends Operator<UnaryOperatorType> {

    private LtlNode operand;

    public UnaryOperator(UnaryOperatorType type) {
        this(type, null);
    }

    public UnaryOperator(UnaryOperatorType type, LtlNode operand) {
        super(type);
        this.operand = operand;
    }

    public LtlNode getOperand() {
        return operand;
    }

    public void setOperand(LtlNode operand) {
        this.operand = operand;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof UnaryOperator) {
            UnaryOperator op = (UnaryOperator) obj;
            if (getType() == op.getType()) {
                return operand.equals(op.getOperand());
            }
        }
        return false;
    }

    public int hashCode() {
        return getName().hashCode() ^ operand.getName().hashCode();
    }

	@Override
	public String toFullString() {
		return toString() + "(" + operand.toFullString() + ")";
	}
}

/**
 * UnaryOperatorType.java, 11.03.2008
 */
package verification.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public enum UnaryOperatorType implements IOperatorType {
    NEG("!") {
        public <R, D> R accept(UnaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitNeg(op, data);
        }
    },
    FUTURE("F") {
        public <R, D> R accept(UnaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitFuture(op, data);
        }
    },
    NEXT("X") {
        public <R, D> R accept(UnaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitNext(op, data);
        }
    },
    GLOBAL("G") {
        public <R, D> R accept(UnaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitGlobal(op, data);
        }
    };

    private String name;

    @Override
    public String toString() {
        return name;
    }
    
    UnaryOperatorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract <R, D> R accept(UnaryOperator op, INodeVisitor<R, D> visitor, D data);
}

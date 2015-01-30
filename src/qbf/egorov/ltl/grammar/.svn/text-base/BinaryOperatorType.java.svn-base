/**
 * BinaryOperatorType.java, 11.03.2008
 */
package ru.ifmo.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public enum BinaryOperatorType implements IOperatorType {
    AND("&&") {
        public <R, D> R accept(BinaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitAnd(op, data);
        }
    },
    OR("||") {
        public <R, D> R accept(BinaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitOr(op, data);
        }
    },
    RELEASE("R") {
        public <R, D> R accept(BinaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitRelease(op, data);
        }},
    UNTIL("U") {
        public <R, D> R accept(BinaryOperator op, INodeVisitor<R, D> visitor, D data) {
            return visitor.visitUntil(op, data);
        }
    };

    private String name;

    private BinaryOperatorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract <R, D> R accept(BinaryOperator op, INodeVisitor<R, D> visitor, D data);
}

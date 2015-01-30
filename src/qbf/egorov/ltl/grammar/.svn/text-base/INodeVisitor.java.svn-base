/**
 * INodeVisitor.java, 19.03.2008
 */
package ru.ifmo.ltl.grammar;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface INodeVisitor<R, D> {
    R visitPredicate(Predicate p, D d);
    R visitNeg(UnaryOperator op, D d);
    R visitFuture(UnaryOperator op, D d);
    R visitNext(UnaryOperator op, D d);
    R visitAnd(BinaryOperator op, D d);
    R visitOr(BinaryOperator op, D d);
    R visitRelease(BinaryOperator op, D d);
    R visitUntil(BinaryOperator op, D d);
    R visitGlobal(UnaryOperator op, D d);
    R visitBoolean(BooleanNode b, D d);
}

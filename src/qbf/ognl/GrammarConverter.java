/**
 * GrammarConverter.java, 12.03.2008
 */
package qbf.ognl;

import java.util.Collections;

import qbf.ltl.grammar.BinaryOperator;
import qbf.ltl.grammar.BinaryOperatorType;
import qbf.ltl.grammar.BooleanNode;
import qbf.ltl.grammar.LtlNode;
import qbf.ltl.grammar.UnaryOperator;
import qbf.ltl.grammar.UnaryOperatorType;
import qbf.ltl.grammar.exception.UnexpectedOperatorException;
import qbf.ltl.grammar.exception.UnexpectedParameterException;

//import org.apache.commons.lang3.NotImplementedException;

/**
 * Convert from Ognl tree to LtlNode tree
 *
 * @author: Kirill Egorov
 */
public class GrammarConverter {

    public static LtlNode convert(Node root) {
        if (root == null) {
            throw new IllegalArgumentException("BuchiNode shouldn't be null");
        }
        if (root instanceof ASTMethod) {
            ASTMethod node = (ASTMethod) root;
            String name = node.getMethodName();

            //is unary operator?
            for (UnaryOperatorType type: UnaryOperatorType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return createUnaryOperator(node, type);
                }
            }

            //is binary operator?
            for (BinaryOperatorType type: BinaryOperatorType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return createBinaryOperator(node, type);
                }
            }

            return new qbf.ltl.grammar.Predicate(name, Collections.singletonList(node._children[0].toString().replaceAll(".*\\.", "")));

        } else if (root instanceof ASTAnd) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.AND);
        } else if (root instanceof ASTOr) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.OR);
        } else if (root instanceof ASTShiftRight) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.IMPLIES);
        } else if (root instanceof ASTNot) {
            return createUnaryOperator((SimpleNode) root, UnaryOperatorType.NEG);
        } else if (root instanceof ASTConst) {
            Object o = ((ASTConst) root).getValue();
            if (o instanceof Boolean) {
                return BooleanNode.getByName(o.toString());
            } else {
                throw new UnexpectedParameterException(o.getClass());
            }
        }
        throw new UnexpectedOperatorException(root.getClass().toString());
    }

    private static UnaryOperator createUnaryOperator(SimpleNode node, UnaryOperatorType type) {
        if (node._children.length != 1) {
            throw new UnexpectedOperatorException(node + " isn't unary operator");
        }
        UnaryOperator op = new UnaryOperator(type);

        op.setOperand(convert(node._children[0]));
        return op;
    }

    private static BinaryOperator createBinaryOperator(SimpleNode node, BinaryOperatorType type) {
        if (node._children.length < 2) {
            throw new UnexpectedOperatorException(node + " isn't binary operation");
        }
        return createBinaryOperator(node, 0, type);
    }

    private static BinaryOperator createBinaryOperator(SimpleNode node, int i, BinaryOperatorType type) {
        BinaryOperator op = new BinaryOperator(type);
        op.setLeftOperand(convert(node._children[i]));
        LtlNode right = (++i == node._children.length - 1) ? convert(node._children[i])
                                                        : createBinaryOperator(node, i, type);
        op.setRightOperand(right);
        return op;
    }

}

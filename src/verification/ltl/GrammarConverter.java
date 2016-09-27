/**
 * GrammarConverter.java, 12.03.2008
 */
package verification.ltl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ognl.ASTAnd;
import ognl.ASTConst;
import ognl.ASTMethod;
import ognl.ASTOr;
import ognl.Node;
import ognl.SimpleNode;
import verification.ltl.grammar.BinaryOperator;
import verification.ltl.grammar.BinaryOperatorType;
import verification.ltl.grammar.BooleanNode;
import verification.ltl.grammar.LtlNode;
import verification.ltl.grammar.PredicateFactory;
import verification.ltl.grammar.UnaryOperator;
import verification.ltl.grammar.UnaryOperatorType;
import verification.ltl.grammar.annotation.Predicate;
import verification.ltl.grammar.exception.NotPredicateException;
import verification.ltl.grammar.exception.UnexpectedMethodException;
import verification.ltl.grammar.exception.UnexpectedOperatorException;
import verification.ltl.grammar.exception.UnexpectedParameterException;

/**
 * Convert from Ognl tree to LtlNode tree
 *
 * @author Kirill Egorov
 */
public class GrammarConverter {
    private final PredicateFactory predicatesObj;
    private final Map<String, Method> predicates = new HashMap<>();

    public GrammarConverter(PredicateFactory predicatesObj) {
        this.predicatesObj = predicatesObj;
        for (Method m : predicatesObj.getClass().getMethods()) {
            if (m.isAnnotationPresent(Predicate.class)) {
                if (!m.getReturnType().equals(boolean.class) && !m.getReturnType().equals(Boolean.class)) {
                    throw new NotPredicateException("Predicate method must return boolean type (" + m + ")");
                }
                predicates.put(m.getName(), m);
            }
        }
    }

    public LtlNode convert(Node root) {
        if (root == null) {
            throw new IllegalArgumentException("BuchiNode can't be null");
        }
        if (root instanceof ASTMethod) {
            final ASTMethod node = (ASTMethod) root;
            final String name = node.getMethodName();

            //is unary operator?
            for (UnaryOperatorType type : UnaryOperatorType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return createUnaryOperator(node, type);
                }
            }

            //is binary operator?
            for (BinaryOperatorType type : BinaryOperatorType.values()) {
                if (type.getName().equalsIgnoreCase(name)) {
                    return createBinaryOperator(node, type);
                }
            }

            //is predicate?
            final Method predMethod = predicates.get(name);
            if (predMethod != null) {
                return new verification.ltl.grammar.Predicate(predicatesObj, predMethod, node.jjtGetChild(0).toString());
            }
            throw new UnexpectedMethodException(node.getMethodName());
        } else if (root instanceof ASTAnd) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.AND);
        } else if (root instanceof ASTOr) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.OR);
        } else if (root.getClass().getSimpleName().equals("ASTNot")) {
            return createUnaryOperator((SimpleNode) root, UnaryOperatorType.NEG);
        } else if (root instanceof ASTConst) {
            final Object o = ((ASTConst) root).getValue();
            if (o instanceof Boolean) {
                return BooleanNode.getByName(o.toString());
            } else {
                throw new UnexpectedParameterException(o.getClass());
            }
        }
        throw new UnexpectedOperatorException(root.getClass().toString());
    }

    private UnaryOperator createUnaryOperator(SimpleNode node, UnaryOperatorType type) {
        if (node.jjtGetNumChildren() != 1) {
            throw new UnexpectedOperatorException(node + " isn't unary operator");
        }
        final UnaryOperator op = new UnaryOperator(type);
        op.setOperand(convert(node.jjtGetChild(0)));
        return op;
    }

    private BinaryOperator createBinaryOperator(SimpleNode node, BinaryOperatorType type) {
        if (node.jjtGetNumChildren() < 2) {
            throw new UnexpectedOperatorException(node + " isn't binary operation");
        }
        return createBinaryOperator(node, 0, type);
    }

    private BinaryOperator createBinaryOperator(SimpleNode node, int i, BinaryOperatorType type) {
        final BinaryOperator op = new BinaryOperator(type);
        op.setLeftOperand(convert(node.jjtGetChild(i)));
        final LtlNode right = (++i == node.jjtGetNumChildren() - 1) ? convert(node.jjtGetChild(i))
                                                        : createBinaryOperator(node, i, type);
        op.setRightOperand(right);
        return op;
    }
}

/**
 * GrammarConverter.java, 12.03.2008
 */
package egorov.ognl;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import egorov.ltl.grammar.BinaryOperator;
import egorov.ltl.grammar.BinaryOperatorType;
import egorov.ltl.grammar.BooleanNode;
import egorov.ltl.grammar.LtlNode;
import egorov.ltl.grammar.PredicateFactory;
import egorov.ltl.grammar.UnaryOperator;
import egorov.ltl.grammar.UnaryOperatorType;
import egorov.ltl.grammar.annotation.Predicate;
import egorov.ltl.grammar.exception.NotPredicateException;
import egorov.ltl.grammar.exception.UnexpectedMethodException;
import egorov.ltl.grammar.exception.UnexpectedOperatorException;
import egorov.ltl.grammar.exception.UnexpectedParameterException;

/**
 * Convert from Ognl tree to LtlNode tree
 *
 * @author Kirill Egorov
 */
public class EgorovGrammarConverter {
    private final Object predicatesObj;
    private final Map<String, Method> predicates = new HashMap<>();

    public EgorovGrammarConverter(PredicateFactory predicatesObj) {
        if (predicatesObj == null) {
            throw new IllegalArgumentException("Predicates object shouldn't be null");
        }
        this.predicatesObj = predicatesObj;
        for (Method m : predicatesObj.getClass().getMethods()) {
            if (m.isAnnotationPresent(Predicate.class)) {
                if (!m.getReturnType().equals(boolean.class) && !m.getReturnType().equals(Boolean.class)) {
                    throw new NotPredicateException("Predicate method should return boolean type (" + m + ")");
                }
                predicates.put(m.getName(), m);
            }
        }
    }

    public LtlNode convert(Node root) {
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

            //is predicate?
            Method predMethod = predicates.get(name);
            if (predMethod != null) {
                return new egorov.ltl.grammar.Predicate(predicatesObj, predMethod, node._children[0].toString());
            }
            throw new UnexpectedMethodException(node.getMethodName());
        } else if (root instanceof ASTAnd) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.AND);
        } else if (root instanceof ASTOr) {
            return createBinaryOperator((SimpleNode) root, BinaryOperatorType.OR);
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

    private UnaryOperator createUnaryOperator(SimpleNode node, UnaryOperatorType type) {
        if (node._children.length != 1) {
            throw new UnexpectedOperatorException(node + " isn't unary operator");
        }
        UnaryOperator op = new UnaryOperator(type);
        op.setOperand(convert(node._children[0]));
        return op;
    }

    private BinaryOperator createBinaryOperator(SimpleNode node, BinaryOperatorType type) {
        if (node._children.length < 2) {
            throw new UnexpectedOperatorException(node + " isn't binary operation");
        }
        return createBinaryOperator(node, 0, type);
    }

    private BinaryOperator createBinaryOperator(SimpleNode node, int i, BinaryOperatorType type) {
        BinaryOperator op = new BinaryOperator(type);
        op.setLeftOperand(convert(node._children[i]));
        LtlNode right = (++i == node._children.length - 1) ? convert(node._children[i])
                                                        : createBinaryOperator(node, i, type);
        op.setRightOperand(right);
        return op;
    }
}

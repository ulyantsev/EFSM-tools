package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import qbf.egorov.ltl.grammar.BinaryOperator;
import qbf.egorov.ltl.grammar.BinaryOperatorType;
import qbf.egorov.ltl.grammar.BooleanNode;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.Predicate;
import qbf.egorov.ltl.grammar.UnaryOperator;
import qbf.egorov.ltl.grammar.UnaryOperatorType;

public class LtlNormalizer {
	public static UnaryOperator future(LtlNode node) {
		return unary(node, UnaryOperatorType.FUTURE);
	}
	
	public static UnaryOperator global(LtlNode node) {
		return unary(node, UnaryOperatorType.GLOBAL);
	}
	
	public static UnaryOperator next(LtlNode node) {
		return unary(node, UnaryOperatorType.NEXT);
	}
	
	public static UnaryOperator not(LtlNode node) {
		return unary(node, UnaryOperatorType.NEG);
	}
	
	public static UnaryOperator unary(LtlNode node, UnaryOperatorType type) {
		return new UnaryOperator(type, node);
	}
	
	public static BinaryOperator and(LtlNode left, LtlNode right) {
		return binary(left, right, BinaryOperatorType.AND);
	}
	
	public static BinaryOperator or(LtlNode left, LtlNode right) {
		return binary(left, right, BinaryOperatorType.OR);
	}
	
	public static BinaryOperator release(LtlNode left, LtlNode right) {
		return binary(left, right, BinaryOperatorType.RELEASE);
	}
	
	public static BinaryOperator until(LtlNode left, LtlNode right) {
		return binary(left, right, BinaryOperatorType.UNTIL);
	}
	
	public static BinaryOperator binary(LtlNode left, LtlNode right, BinaryOperatorType type) {
		return new BinaryOperator(type, left, right);
	}
	
	public static LtlNode toNegationNormalForm(LtlNode node) {
		if (node instanceof BooleanNode || node instanceof Predicate) {
			return node;
		} else if (node instanceof BinaryOperator) {
			BinaryOperator op = (BinaryOperator) node;
			return binary(toNegationNormalForm(op.getLeftOperand()),
				toNegationNormalForm(op.getRightOperand()), op.getType());
		} else if (node instanceof UnaryOperator && ((UnaryOperator) node).getType() != UnaryOperatorType.NEG) {
			UnaryOperator op = (UnaryOperator) node;
			return unary(toNegationNormalForm(op.getOperand()), op.getType());
		} else if (node instanceof UnaryOperator) {
			final LtlNode nestedNode = ((UnaryOperator) node).getOperand();
			if (nestedNode == BooleanNode.TRUE) {
				return BooleanNode.FALSE;
			} else if (nestedNode == BooleanNode.FALSE) {
				return BooleanNode.TRUE;
			} else if (nestedNode instanceof Predicate) {
				return not(nestedNode);
			} else if (nestedNode instanceof UnaryOperator) {
				UnaryOperator uo = (UnaryOperator) nestedNode;
				switch (uo.getType()) {
				case GLOBAL:
					return future(toNegationNormalForm(not(uo.getOperand())));
				case FUTURE:
					return global(toNegationNormalForm(not(uo.getOperand())));
				case NEXT:
					return next(toNegationNormalForm(not(uo.getOperand())));
				case NEG:
					return toNegationNormalForm(uo.getOperand());
				default:
					throw new RuntimeException("Unknown unary operator " + nestedNode.toString());
				}
			} else if (nestedNode instanceof BinaryOperator) {
				BinaryOperator bo = (BinaryOperator) nestedNode;
				switch (bo.getType()) {
				case OR:
					return and(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				case AND:
					return or(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				case UNTIL:
					return release(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				case RELEASE:
					return until(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				default:
					throw new RuntimeException("Unknown binary operator " + nestedNode.toString());
				}
			} else {
				throw new AssertionError();
			}
		} else {
			throw new AssertionError();
		}
	}
}

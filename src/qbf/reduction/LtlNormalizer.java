package qbf.reduction;

import qbf.ltl.BinaryOperator;
import qbf.ltl.BinaryOperatorType;
import qbf.ltl.BooleanNode;
import qbf.ltl.LtlNode;
import qbf.ltl.Predicate;
import qbf.ltl.UnaryOperator;
import qbf.ltl.UnaryOperatorType;

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

	public static LtlNode removeImplications(LtlNode node) {
		if (node instanceof BinaryOperator) {
			BinaryOperator bo = (BinaryOperator) node;
			if (bo.getType() == BinaryOperatorType.IMPLIES) {
				return or(not(removeImplications(bo.getLeftOperand())), removeImplications(bo.getRightOperand()));
			} else {
				return binary(removeImplications(bo.getLeftOperand()), removeImplications(bo.getRightOperand()), bo.getType());
			}
		} else if (node instanceof UnaryOperator) {
			UnaryOperator uo = (UnaryOperator) node;
			return unary(removeImplications(uo.getOperand()), uo.getType());
		} else {
			return node;
		}
	}
	
	public static LtlNode toNegationNormalForm(LtlNode node) {
		if (node instanceof BooleanNode || node instanceof Predicate) {
			return node;
		} else if (node instanceof BinaryOperator) {
			BinaryOperator op = (BinaryOperator) node;
			return binary(toNegationNormalForm(op.getLeftOperand()),
				toNegationNormalForm(op.getRightOperand()), op.getType());
		} else if (node instanceof UnaryOperator && node.toString() != "!") {
			UnaryOperator op = (UnaryOperator) node;
			return unary(toNegationNormalForm(op.getOperand()), op.getType());
		} else if (node instanceof UnaryOperator && node.toString() == "!") {
			final LtlNode nestedNode = ((UnaryOperator) node).getOperand();
			if (nestedNode == BooleanNode.TRUE) {
				return BooleanNode.FALSE;
			} else if (nestedNode == BooleanNode.FALSE) {
				return BooleanNode.TRUE;
			} else if (nestedNode instanceof Predicate) {
				return not(nestedNode);
			} else if (nestedNode instanceof UnaryOperator) {
				UnaryOperator uo = (UnaryOperator) nestedNode;
				switch (uo.toString()) {
				case "G":
					return future(toNegationNormalForm(not(uo.getOperand())));
				case "F":
					return global(toNegationNormalForm(not(uo.getOperand())));
				case "X":
					return next(toNegationNormalForm(not(uo.getOperand())));
				case "!":
					return toNegationNormalForm(uo.getOperand());
				default:
					throw new RuntimeException("Unknown unary operator " + nestedNode.toString());
				}
			} else if (nestedNode instanceof BinaryOperator) {
				BinaryOperator bo = (BinaryOperator) nestedNode;
				switch (bo.toString()) {
				case "||":
					return and(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				case "&&":
					return or(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				case "U":
					return release(toNegationNormalForm(not(bo.getLeftOperand())),
						toNegationNormalForm(not(bo.getRightOperand())));
				case "R":
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

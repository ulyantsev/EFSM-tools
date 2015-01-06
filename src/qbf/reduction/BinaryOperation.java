package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BinaryOperation extends BooleanFormula {
	private final List<BooleanFormula> children;
	private final BinaryOperations type;
	private final String comment;
	
	public BinaryOperation(BooleanFormula left, BooleanFormula right, BinaryOperations type) {
		this(Arrays.asList(left, right), type, null);
	}
	
	public BinaryOperation(List<BooleanFormula> children, BinaryOperations type) {
		this(children, type, null);
	}
	
	public BinaryOperation(List<BooleanFormula> children, BinaryOperations type, String comment) {
		for (BooleanFormula f : children) {
			assert f != null;
		}
		this.children = new ArrayList<>(children);
		this.type = type;
		this.comment = comment;
	}
	
	@Override
	public String toLimbooleString() {
		if (children.size() < 2 && type != BinaryOperations.AND && type != BinaryOperations.OR) {
			throw new RuntimeException("Only AND and OR [0 or 1]-length formulae are possible.");
		}
		if (children.isEmpty()) {
			switch (type) {
			case AND:
				return TrueFormula.INSTANCE.toLimbooleString();
			case OR:
				return FalseFormula.INSTANCE.toLimbooleString();
			default:
				assert false;
			}
		} else if (children.size() == 1) {
			return children.get(0).toLimbooleString();
		}
		
		List<String> strChildren = new ArrayList<>();
		for (BooleanFormula f : children) {
			strChildren.add(f.toLimbooleString());
		}
		return "(" + String.join(" " + type + " ", strChildren) + ")";
	}
	
	private String commentStringStart() {
		return comment == null ? "" : "\n/* " + comment + " */\n";
	}
	
	private String commentStringEnd() {
		return comment == null ? "" : "\n/* " + "end of block" + " */\n";
	}
	
	private String comment(String content) {
		return commentStringStart() + content + commentStringEnd();
	}
	
	@Override
	public String toString() {
		if (children.size() < 2 && type != BinaryOperations.AND && type != BinaryOperations.OR) {
			throw new RuntimeException("Only AND and OR [0 or 1]-length formulae are possible.");
		}
		if (children.isEmpty()) {
			switch (type) {
			case AND:
				return comment(TrueFormula.INSTANCE.toString());
			case OR:
				return comment(FalseFormula.INSTANCE.toString());
			default:
				assert false;
			}
		} else if (children.size() == 1) {
			return comment(children.get(0).toString());
		}
		
		List<String> strChildren = children.stream().map(f -> f.toString()).collect(Collectors.toList());
		return "(" + comment(String.join(" " + type + " ", strChildren)) + ")";
	}
	
	public static BooleanFormula and(List<BooleanFormula> elements) {
		return and(elements, null);
	}
	
	public static BooleanFormula and(List<BooleanFormula> elements, String comment) {
		return new BinaryOperation(elements, BinaryOperations.AND, comment);
	}
	
	public static BooleanFormula and(BooleanFormula... elements) {
		return new BinaryOperation(Arrays.asList(elements), BinaryOperations.AND);
	}
	
	public static BooleanFormula or(List<BooleanFormula> elements) {
		return or(elements, null);
	}
	
	public static BooleanFormula or(List<BooleanFormula> elements, String comment) {
		return new BinaryOperation(elements, BinaryOperations.OR, comment);
	}
	
	public static BooleanFormula or(BooleanFormula... elements) {
		return new BinaryOperation(Arrays.asList(elements), BinaryOperations.OR);
	}

	@Override
	public BooleanFormula substitute(BooleanVariable v, BooleanFormula replacement) {
		List<BooleanFormula> newChildren = children.stream().map(c ->
			c.substitute(v, replacement)).collect(Collectors.toList()
		);
		return new BinaryOperation(newChildren, type);
	}
	
	@Override
	public BooleanFormula simplify() {
		List<BooleanFormula> childrenSimpl = children.stream()
				.map(BooleanFormula::simplify).collect(Collectors.toList());
		switch (type) {
		case AND:
			if (childrenSimpl.contains(FalseFormula.INSTANCE)) {
				return FalseFormula.INSTANCE;
			}
			childrenSimpl.removeIf(elem -> elem == TrueFormula.INSTANCE);
			return and(childrenSimpl);
		case OR:
			if (childrenSimpl.contains(TrueFormula.INSTANCE)) {
				return TrueFormula.INSTANCE;
			}
			childrenSimpl.removeIf(elem -> elem == FalseFormula.INSTANCE);
			return or(childrenSimpl);
		case EQ:
			boolean hasTrue = childrenSimpl.stream().anyMatch(elem -> elem == TrueFormula.INSTANCE);
			boolean hasFalse = childrenSimpl.stream().anyMatch(elem -> elem == FalseFormula.INSTANCE);
			if (hasTrue && hasFalse) {
				return FalseFormula.INSTANCE;
			} else if (hasTrue) {
				childrenSimpl.removeIf(elem -> elem == TrueFormula.INSTANCE);
				childrenSimpl.add(TrueFormula.INSTANCE);
			} else if (hasFalse) {
				childrenSimpl.removeIf(elem -> elem == FalseFormula.INSTANCE);
				childrenSimpl.add(FalseFormula.INSTANCE);
			}
			return new BinaryOperation(childrenSimpl, BinaryOperations.EQ);
		case IMPLIES:
			if (children.size() == 2) {
				BooleanFormula left = childrenSimpl.get(0);
				BooleanFormula right = childrenSimpl.get(1);
				if (left == FalseFormula.INSTANCE || right == TrueFormula.INSTANCE) {
					return TrueFormula.INSTANCE;
				} else if (left == TrueFormula.INSTANCE && right == FalseFormula.INSTANCE) {
					return FalseFormula.INSTANCE;
				} else if (left == TrueFormula.INSTANCE) {
					return right;
				} else if (right == FalseFormula.INSTANCE) {
					return left.not();
				} else {
					return new BinaryOperation(childrenSimpl, BinaryOperations.IMPLIES);
				}
			} else {
				return new BinaryOperation(childrenSimpl, BinaryOperations.IMPLIES);
			}
		}
		assert false;
		return null;
	}
}

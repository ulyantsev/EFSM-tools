package qbf.reduction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		if (children.isEmpty()) {
			switch (type) {
			case AND:
				return TrueFormula.INSTANCE.toLimbooleString();
			case OR:
				return FalseFormula.INSTANCE.toLimbooleString();
			default:
				throw new RuntimeException("Only AND and OR zero-length formulae are possible.");
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
		if (children.isEmpty()) {
			switch (type) {
			case AND:
				return comment(TrueFormula.INSTANCE.toString());
			case OR:
				return comment(FalseFormula.INSTANCE.toString());
			default:
				throw new RuntimeException("Only AND and OR zero-length formulae are possible.");
			}
		} else if (children.size() == 1) {
			return comment(children.get(0).toString());
		}
		
		List<String> strChildren = new ArrayList<>();
		for (BooleanFormula f : children) {
			strChildren.add(f.toString());
		}
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
}

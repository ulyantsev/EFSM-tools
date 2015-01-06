package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FormulaList {
	private final List<BooleanFormula> list = new ArrayList<>();
	private final BinaryOperations op;
	
	public FormulaList(BinaryOperations op) {
		this.op = op;
		assert op == BinaryOperations.OR || op == BinaryOperations.AND;
	}
	
	public void add(BooleanFormula f) {
		assert f != null;
		list.add(f);
	}
	
	public void addVarIfExists(BooleanVariable var) {
		if (var != null) {
			list.add(var);
		}
	}
	
	public void addAll(Collection<BooleanFormula> f) {
		assert !f.contains(null);
		list.addAll(f);
	}
	
	public void removeLast() {
		list.remove(list.size() - 1);
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}
	
	public void clear() {
		list.clear();
	}
	
	public BooleanFormula assemble(String comment) {
		if (op == BinaryOperations.OR) {
			return BinaryOperation.or(list, comment);
		} else if (op == BinaryOperations.AND) {
			return BinaryOperation.and(list, comment);
		} else {
			throw new AssertionError();
		}
	}
	
	public BooleanFormula assemble() {
		return assemble(null);
	}
	
	@Override
	public String toString() {
		return assemble().toString();
	}
}
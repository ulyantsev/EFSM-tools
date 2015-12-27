package bnf_formulae;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * (c) Igor Buzhinsky
 */

public class NotOperation extends BooleanFormula {
	public final BooleanFormula inside;

	public NotOperation(BooleanFormula inside) {
		this.inside = inside;
	}
	
	@Override
	public String toLimbooleString() {
		return "!" + inside.toLimbooleString();
	}
	
	@Override
	public String toString() {
		return "!" + inside;
	}
	
	@Override
	public BooleanFormula simplify() {
		final BooleanFormula insideSimpl = inside.simplify();
		return insideSimpl == TrueFormula.INSTANCE ? FalseFormula.INSTANCE
			 : insideSimpl == FalseFormula.INSTANCE ? TrueFormula.INSTANCE : insideSimpl.not();
	}

	@Override
	public BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement) {
		return inside.multipleSubstitute(replacement).not();
	}

	@Override
	public BooleanFormula removeEqImpl() {
		return inside.removeEqImpl().not();
	}

	@Override
	public BooleanFormula propagateNot() {
		if (inside instanceof BooleanVariable) {
			return this;
		} else if (inside instanceof NotOperation) {
			return ((NotOperation) inside).inside;
		} else if (inside instanceof BinaryOperation) {
			final BinaryOperation binInside = (BinaryOperation) inside;
			final List<BooleanFormula> children = binInside.copyChildren();
			final List<BooleanFormula> processed = children.stream()
					.map(f -> f.not().propagateNot())
					.collect(Collectors.toList());
			switch (binInside.type) {
			case AND:
				return BinaryOperation.or(processed);
			case OR:
				return BinaryOperation.and(processed);
			default:
				throw new AssertionError("Not supported; get rid of other operations first!");
			}
		} else if (inside instanceof TrueFormula) {
			return FalseFormula.INSTANCE;
		} else if (inside instanceof FalseFormula) {
			return TrueFormula.INSTANCE;
		} else {
			throw new AssertionError("Unknown formula type");
		}
	}

	@Override
	public boolean separateOr(List<BooleanFormula> terms) {
		terms.add(this);
		return true;
	}
}

package bnf_formulae;

import java.util.Map;

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
}

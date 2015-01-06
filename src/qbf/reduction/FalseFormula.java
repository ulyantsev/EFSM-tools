package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

public class FalseFormula extends BooleanFormula {
	public static final FalseFormula INSTANCE = new FalseFormula();

	private FalseFormula() {
	}
	
	@Override
	public String toLimbooleString() {
		return BooleanVariable.byName("x", 0, 0).get().not().toLimbooleString();
	}
	
	@Override
	public String toString() {
		return "FALSE";
	}

	@Override
	public BooleanFormula substitute(BooleanVariable v, BooleanFormula replacement) {
		return this;
	}

	@Override
	public BooleanFormula simplify() {
		return this;
	}
}

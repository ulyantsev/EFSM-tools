package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

public class TrueFormula extends BooleanFormula {	
	public static final TrueFormula INSTANCE = new TrueFormula();

	private TrueFormula() {
	}

	@Override
	public String toLimbooleString() {
		return BooleanVariable.byName("x", 0, 0).get().toLimbooleString();
	}
	
	@Override
	public String toString() {
		return "TRUE";
	}
}

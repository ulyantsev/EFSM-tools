package bnf_formulae;

import java.util.Map;

/**
 * (c) Igor Buzhinsky
 */

public class TrueFormula extends BooleanFormula {	
	public static final TrueFormula INSTANCE = new TrueFormula();

	private TrueFormula() {
	}

	@Override
	public String toLimbooleString() {
		throw new AssertionError();
	}
	
	@Override
	public String toString() {
		return "TRUE";
	}
	
	@Override
	public BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement) {
		return this;
	}
	
	@Override
	public BooleanFormula simplify() {
		return this;
	}
}

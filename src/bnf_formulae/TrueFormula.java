package bnf_formulae;

import java.util.List;
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

	@Override
	public BooleanFormula removeEqImplConst() {
		final BooleanVariable v = BooleanVariable.getVarByNumber(1);
		return v.or(v.not());
	}

	@Override
	public BooleanFormula propagateNot() {
		return this;
	}
	
	@Override
	public boolean separateOr(List<BooleanFormula> terms) {
		throw new AssertionError("Get rid of this formula type before applying!");
	}
}

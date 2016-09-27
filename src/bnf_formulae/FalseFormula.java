package bnf_formulae;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Map;

public class FalseFormula extends BooleanFormula {
    public static final FalseFormula INSTANCE = new FalseFormula();

    private FalseFormula() {
    }
    
    @Override
    public String toLimbooleString() {
        throw new AssertionError();
    }
    
    @Override
    public String toString() {
        return "FALSE";
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

package bnf_formulae;

/**
 * (c) Igor Buzhinsky
 */

public enum BinaryOperations {
	EQ("<->"), IMPLIES("->"), AND("&"), OR("|");
	
	private final String symbol;
	
	private BinaryOperations(String symbol) {
		this.symbol = symbol;
	}
	
	@Override
	public String toString() {
		return symbol;
	}
}
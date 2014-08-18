package qbf.reduction;

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
}

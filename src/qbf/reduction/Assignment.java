package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

public class Assignment implements Comparable<Assignment> {
	public final BooleanVariable var;
	public final boolean value;
	
	public Assignment(BooleanVariable var, boolean value) {
		this.var = var;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return var + " = " + (value ? 1 : 0);
	}

	@Override
	public int compareTo(Assignment o) {
		return var.compareTo(o.var);
	}
}

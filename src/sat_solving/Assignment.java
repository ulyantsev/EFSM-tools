package sat_solving;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;
import java.util.stream.Collectors;

import bnf_formulae.BooleanVariable;
import bnf_formulae.BooleanFormula.DimacsConversionInfo;

public class Assignment implements Comparable<Assignment> {
	public final BooleanVariable var;
	public final boolean value;
	
	public Assignment(BooleanVariable var, boolean value) {
		this.var = var;
		this.value = value;
	}
	
	public Assignment negate() {
		return new Assignment(var, !value);
	}
	
	@Override
	public String toString() {
		return var + " = " + (value ? 1 : 0);
	}
	
	public String toDimacsString(DimacsConversionInfo info) {
		return (value ? "" : "-") + info.toDimacsNumber(var.number).get();
	}
	
	public static String toDimacsString(List<Assignment> list, DimacsConversionInfo info) {
		return String.join(" ", list.stream()
				.map(ass -> ass.toDimacsString(info))
				.collect(Collectors.toList())) + " 0";
	}

	@Override
	public int compareTo(Assignment o) {
		return var.compareTo(o.var);
	}
}

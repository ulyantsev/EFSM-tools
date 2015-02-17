package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SolverResult {
	private final SolverResults result;
	private final List<Assignment> assignments;
	
	public enum SolverResults {
		SAT, UNSAT, UNKNOWN
	}
	
	public SolverResult(List<Assignment> assignments) {
		result = SolverResults.SAT;
		this.assignments = assignments.stream().sorted().collect(Collectors.toList());
	}
	
	public SolverResult(SolverResults result) {
		this.result = result;
		assignments = Collections.emptyList();
	}
	
	public SolverResults type() {
		return result;
	}
	
	public List<Assignment> list() {
		return Collections.unmodifiableList(assignments);
	}
	
	@Override
	public String toString() {
		return result + (result == SolverResults.SAT ?
			assignments.toString().replaceAll(", ", "\n").replace("[", "\n").replace("]", "") : "");
	}
}

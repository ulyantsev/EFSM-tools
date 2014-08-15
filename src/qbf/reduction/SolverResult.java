package qbf.reduction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SolverResult {
	private final SolverResults result;
	private final List<Assignment> assignments;
	private final int timeMillis;
	
	public enum SolverResults {
		SAT, UNSAT, UNKNOWN
	}
	
	public SolverResult(List<Assignment> assignments, int timeMillis) {
		result = SolverResults.SAT;
		this.assignments = new ArrayList<>(assignments);
		Collections.sort(this.assignments);
		this.timeMillis = timeMillis;
	}
	
	public SolverResult(SolverResults result, int timeMillis) {
		this.result = result;
		assignments = Collections.emptyList();
		this.timeMillis = timeMillis;
	}
	
	public int timeMillis() {
		return timeMillis;
	}
	
	public SolverResults type() {
		return result;
	}
	
	public List<Assignment> list() {
		return Collections.unmodifiableList(assignments);
	}
	
	@Override
	public String toString() {
		return result + " [finished in " + timeMillis + "ms]" + (result == SolverResults.SAT ?
			assignments.toString().replaceAll(", ", "\n").replace("[", "\n").replace("]", "") : "");
	}
}

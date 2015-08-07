package sat_solving;

/**
 * (c) Igor Buzhinsky
 */

public enum SatSolver {
	CRYPTOMINISAT("cryptominisat --maxtime="),
	LINGELING("lingeling -t ");
	
	public final String command;

	private SatSolver(String command) {
		this.command = command;
	}
}

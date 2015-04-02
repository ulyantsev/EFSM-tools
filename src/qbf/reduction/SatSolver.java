package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

public enum SatSolver {
	CRYPTOMINISAT("cryptominisat --maxtime="),
	LINGELING("lingeling -t "),
	ITERATIVE_CRYPTOMINISAT("iterative-cryptominisat");
	
	public final String command;

	private SatSolver(String command) {
		this.command = command;
	}
}

package qbf.reduction;

/**
 * (c) Igor Buzhinsky
 */

public enum SatSolver {
	CRYPTOMINISAT("cryptominisat --maxtime="),
	LINGELING("lingeling -t "),
	INCREMENTAL_CRYPTOMINISAT("incremental-cryptominisat"),
	INCREMENTAL_LINGELING("incremental-lingeling");
	
	public final String command;

	private SatSolver(String command) {
		this.command = command;
	}
	
	public boolean isIncremental() {
		return this == INCREMENTAL_CRYPTOMINISAT || this == INCREMENTAL_LINGELING;
	}
}

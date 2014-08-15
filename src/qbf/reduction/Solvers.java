package qbf.reduction;

public enum Solvers {
	DEPQBF("depqbf"), SKIZZO("sKizzo");
	
	public final String command;

	private Solvers(String command) {
		this.command = command;
	}
}

package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import structures.Automaton;

public class AutomatonFound extends Exception {
	public final Automaton automaton;

	public AutomatonFound(Automaton automaton) {
		this.automaton = automaton;
	}
	
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
package algorithms.exception;

/**
 * (c) Igor Buzhinsky
 */

import structures.Automaton;

public class AutomatonFoundException extends Exception {
	public final Automaton automaton;

	public AutomatonFoundException(Automaton automaton) {
		this.automaton = automaton;
	}
	
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}
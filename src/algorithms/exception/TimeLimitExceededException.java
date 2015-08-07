package algorithms.exception;

/**
 * (c) Igor Buzhinsky
 */

public class TimeLimitExceededException extends Exception {
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}

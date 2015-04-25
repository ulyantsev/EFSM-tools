package algorithms;

/**
 * (c) Igor Buzhinsky
 */

public class TimeLimitExceeded extends Exception {
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}

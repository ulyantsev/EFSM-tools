package algorithms;

public class TimeLimitExceeded extends Exception {
	@Override
	public Throwable fillInStackTrace() {
		return this;
	}
}

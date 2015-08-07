package egorov.verifier;

import java.util.Collections;
import java.util.List;

public class Counterexample {
	private final List<String> events;
	public final int loopLength;
	
	public List<String> events() {
		return Collections.unmodifiableList(events);
	}
	
	public Counterexample(List<String> events, int loopLength) {
		this.events = events;
		this.loopLength = loopLength;
	}
	
	public boolean isEmpty() {
		return events.isEmpty();
	}
	
	@Override
	public String toString() {
		return "[" + events + ", loop " + loopLength + "]";
	}
}

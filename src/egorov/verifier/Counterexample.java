package egorov.verifier;

import java.util.Collections;
import java.util.List;

public class Counterexample {
	private final List<String> events;
	private final List<List<String>> actions;
	public final int loopLength;
	
	public List<String> events() {
		return Collections.unmodifiableList(events);
	}
	
	public List<List<String>> actions() {
		return Collections.unmodifiableList(actions);
	}
	
	public Counterexample(List<String> events, List<List<String>> actions, int loopLength) {
		this.events = events;
		this.actions = actions;
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

package scenario;

import java.util.Arrays;

public class StringActions {
	private final String[] actions;

	public StringActions(String str) {
		str = str.trim();
		actions = str.isEmpty() ? new String[0] : str.split(",");
		for (int i = 0; i < actions.length; i++) {
			actions[i] = actions[i].trim();
		}
	}

	public String[] getActions() {
		return actions;
	}

	public int size() {
		return actions.length;
	}
	
	public boolean equals(StringActions other) {
		return Arrays.deepEquals(actions, other.actions);
	}

	public String toString() {
		return String.join(", ", actions);
	}
}

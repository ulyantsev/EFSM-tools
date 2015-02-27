package actions;

import java.util.Arrays;

public class StringActions {
	private final String[] actions;

	public StringActions(String str) {
		str = str.trim();
		if (str.equals("")) {
			actions = new String[0];
		} else {
			actions = str.split(",");
		}
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
		return Arrays.deepEquals(this.actions, other.actions);
	}

	public String toString() {
		return String.join(", ", actions);
	}
}

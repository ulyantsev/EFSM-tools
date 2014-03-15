package actions;

import java.util.Arrays;

public class StringActions {
	private String[] actions;

	public StringActions(String str) {
		str = str.trim();
		if (str.equals("")) {
			this.actions = new String[0];
		} else {
			this.actions = str.split(",");
		}
		for (int i = 0; i < actions.length; i++) {
			this.actions[i] = this.actions[i].trim();
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
		String res = "";
		for (int i = 0; i < actions.length; i++) {
			if (i > 0) {
				res += ", ";
			}
			res += actions[i];
		}
		return res;
	}
}

/**
 * Predicate.java, 12.03.2008
 */
package qbf.ltl.grammar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Predicate extends LtlNode {
	private final List<String> args;

	public final List<String> args() {
		return args;
	}
	
    public Predicate(String name, List<String> args) {
        super(name);
        this.args = Collections.unmodifiableList(new ArrayList<>(args));
    }

    public String toString() {
    	return getName() + "(" + String.join(", ", args) + ")";
    }

	@Override
	public String toFullString() {
		return toString();
	}
}

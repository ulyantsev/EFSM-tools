package verification.verifier;

import java.util.ArrayList;
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
        final List<String> elements = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            elements.add(events.get(i) + "/" + actions.get(i));
        }
        return "[" + String.join(", ", elements) + ", loop " + loopLength + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((actions == null) ? 0 : actions.hashCode());
        result = prime * result + ((events == null) ? 0 : events.hashCode());
        result = prime * result + loopLength;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Counterexample other = (Counterexample) obj;
        if (actions == null) {
            if (other.actions != null)
                return false;
        } else if (!actions.equals(other.actions))
            return false;
        if (events == null) {
            if (other.events != null)
                return false;
        } else if (!events.equals(other.events))
            return false;
        if (loopLength != other.loopLength)
            return false;
        return true;
    }
}

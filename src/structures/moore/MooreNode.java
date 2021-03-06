package structures.moore;

import scenario.StringActions;

import java.util.*;
import java.util.stream.Collectors;

public class MooreNode {
    private int number;
    private final StringActions actions;
    private final List<MooreTransition> transitions = new ArrayList<>(1);
    
    public MooreNode(int number, StringActions actions) {
        this.number = number;
        this.actions = actions;
    }

    public int number() {
        return number;
    }
    
    public StringActions actions() {
        return actions;
    }
    
    public boolean hasTransition(String event) {
        return transitions.stream().anyMatch(t -> t.event().equals(event));
    }
    
    public void addTransition(String event, MooreNode dst) {
        transitions.add(new MooreTransition(this, dst, event));
    }
    
    public void addTransition(MooreTransition transition) {
        transitions.add(transition);
    }
    
    public void removeTransition(MooreTransition transition) {
        for (int i = 0; i < transitions.size(); i++) {
            if (transitions.get(i) == transition) {
                transitions.remove(i);
                break;
            }
        }
    }
    
    public Collection<MooreTransition> transitions() {
        return Collections.unmodifiableList(transitions);
    }
    
    public int transitionCount() {
        return transitions.size();
    }
    
    @Override
    public String toString() {
        return toString(Collections.emptyMap());
    }
    
    public String toString(Map<String, String> actionDescriptions) {
        return String.valueOf(number + "\\n" + String.join("\\n",
                Arrays.stream(actions.getActions())
                .map(Object::toString)
                .map(s -> actionDescriptions.getOrDefault(s, s))
                .collect(Collectors.toList())));
    }
    
    public MooreNode scenarioDst(String event, StringActions actions) {
        for (MooreTransition t : transitions) {
            if (t.event().equals(event) && t.dst().actions().equals(actions)) {
                return t.dst();
            }
        }
        return null;
    }
    
    public List<MooreNode> allDst(String event) {
        return transitions.stream().filter(t -> t.event().equals(event)).map(MooreTransition::dst)
                .collect(Collectors.toList());
    }
}

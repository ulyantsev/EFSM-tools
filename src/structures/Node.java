package structures;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import actions.StringActions;
import bool.MyBooleanExpression;

public class Node {
    private final int number;
    private final Map<String, Transition> transitions;
    
    public Node(int number) {
        this.number = number;
        transitions = new TreeMap<>();
    }
    
    public int getNumber() {
        return number;
    }
    
    public boolean hasTransition(String event, MyBooleanExpression expr) {
        return transitions.containsKey(event + "[" + expr.toString() + "]");
    }
    
    public void addTransition(String event, MyBooleanExpression expr, StringActions actions, Node dst) {
        Transition transition = new Transition(this, dst, event, expr, actions);
        transitions.put(event + "[" + expr.toString() + "]", transition);
    }
    
    public Transition getTransition(String event, MyBooleanExpression expr) {
        return transitions.get(event + "[" + expr.toString() + "]");
    }
    
    public Collection<Transition> getTransitions() {
        return transitions.values();
    }
    
    public Node getDst(String event, MyBooleanExpression expr) {
        return transitions.get(event + "[" + expr.toString() + "]").getDst();
    }
    
    public int transitionsCount() {
        return transitions.size();
    }
}

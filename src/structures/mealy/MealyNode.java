package structures.mealy;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import scenario.StringActions;
import bool.MyBooleanExpression;

public class MealyNode {
    private int number;
    private final Map<String, MealyTransition> transitions;
    
    public MealyNode(int number) {
        this.number = number;
        transitions = new TreeMap<>();
    }

    public int number() {
        return number;
    }
    
    public void setNumber(int number) {
        this.number = number;
    }
    
    public boolean hasTransition(String event, MyBooleanExpression expr) {
        return transitions.containsKey(event + "[" + expr.toString() + "]");
    }
    
    public void addTransition(String event, MyBooleanExpression expr, StringActions actions, MealyNode dst) {
        MealyTransition transition = new MealyTransition(this, dst, event, expr, actions);
        transitions.put(event + "[" + expr.toString() + "]", transition);
    }
    
    public void removeTransition(MealyTransition transition) {
        transitions.remove(transition.event() + "[" + transition.expr().toString() + "]");
    }
    
    public MealyTransition transition(String event, MyBooleanExpression expr) {
        return transitions.get(event + "[" + expr.toString() + "]");
    }
    
    public Collection<MealyTransition> transitions() {
        return transitions.values();
    }
    
    public MealyNode dst(String event, MyBooleanExpression expr) {
        return transitions.get(event + "[" + expr.toString() + "]").dst();
    }
    
    public int transitionCount() {
        return transitions.size();
    }
    
    @Override
    public String toString() {
        return String.valueOf(number);
    }
}

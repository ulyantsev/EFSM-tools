package structures.plant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import scenario.StringActions;

public class MooreNode {
    private int number;
    private final StringActions actions;
    private final List<MooreTransition> transitions = new ArrayList<>();
    
    public MooreNode(int number, StringActions actions) {
        this.number = number;
        this.actions = actions;
    }
    
    public int getNumber() {
        return number;
    }
    
    public StringActions getActions() {
        return actions;
    }
    
    public void setNumber(int number) {
    	this.number = number;
    }
    
    public boolean hasTransition(String event) {
    	return transitions.stream().anyMatch(t -> t.getEvent().equals(event));
    }
    
    public void addTransition(String event, MooreNode dst) {
        final MooreTransition transition = new MooreTransition(this, dst, event);
        transitions.add(transition);
    }
    
    public void removeTransition(MooreTransition transition) {
        transitions.remove(transition.getEvent());
    }
    
    public Collection<MooreTransition> getTransitions() {
        return Collections.unmodifiableList(transitions);
    }
    
    public int transitionsCount() {
        return transitions.size();
    }
    
    @Override
    public String toString() {
    	return String.valueOf(number + " : " + actions);
    }
    
    public MooreNode getScenarioDst(String event, StringActions actions) {
    	for (MooreTransition t : transitions) {
    		if (t.getEvent().equals(event) && t.getDst().getActions().equals(actions)) {
    			return t.getDst();
    		}
    	}
    	return null;
    }
    
    public List<MooreNode> getAllDst(String event) {
    	final List<MooreNode> ans = new ArrayList<>();
    	for (MooreTransition t : transitions) {
    		if (t.getEvent().equals(event)) {
    			ans.add(t.getDst());
    		}
    	}
    	return ans;
    }
}

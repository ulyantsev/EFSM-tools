package structures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import actions.StringActions;
import bool.MyBooleanExpression;

public class NegativeNode extends Node {
    private final List<Transition> transitionList;
    private Set<NegativeNode> loops = null;
    
    public NegativeNode(int number) {
    	super(number);
    	transitionList = new ArrayList<>();
    }
    
    public void addLoop(NegativeNode node) {
    	if (loops == null) {
    		loops = new LinkedHashSet<>();
    	}
    	loops.add(node);
    }
    
    public boolean weakInvalid() {
    	return !loops().isEmpty();
    }
    
    public boolean strongInvalid() {
    	return loops().contains(this);
    }
    
    public Collection<NegativeNode> loops() {
    	return loops != null ? Collections.unmodifiableSet(loops) : Collections.emptySet();
    }
    
    @Override
    public int getNumber() {
        return super.getNumber();
    }
    
    @Override
    public boolean hasTransition(String event, MyBooleanExpression expr) {
        throw new AssertionError();
    }
    
    @Override
    public void addTransition(String event, MyBooleanExpression expr, StringActions actions, Node dst) {
    	throw new AssertionError();
    }
    
    public void addTransition(String event, MyBooleanExpression expr, StringActions actions, NegativeNode dst) {
        transitionList.add(new Transition(this, dst, event, expr, actions));
    }
    
    @Override
    public void removeTransition(Transition transition) {
    	throw new AssertionError();
    }
    
    @Override
    public Transition getTransition(String event, MyBooleanExpression expr) {
    	throw new AssertionError();
    }
    
    @Override
    public Collection<Transition> getTransitions() {
        return transitionList;
    }
    
    @Override
    public NegativeNode getDst(String event, MyBooleanExpression expr) {
    	throw new AssertionError();
    }
    
    public NegativeNode getDst(String event, MyBooleanExpression expr, StringActions actions) {
    	for (Transition t : transitionList) {
    		if (t.getEvent().equals(event) && t.getExpr().equals(expr) && t.getActions().equals(actions)) {
    			return (NegativeNode) t.getDst();
    		}
    	}
    	return null;
    }
    
    @Override
    public int transitionsCount() {
        return transitionList.size();
    }
}

package structures;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import scenario.StringActions;
import bool.MyBooleanExpression;

public class NegativeNode extends MealyNode {
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
    public boolean hasTransition(String event, MyBooleanExpression expr) {
        throw new AssertionError();
    }
    
    @Override
    public void addTransition(String event, MyBooleanExpression expr, StringActions actions, MealyNode dst) {
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
    public Transition transition(String event, MyBooleanExpression expr) {
    	throw new AssertionError();
    }
    
    @Override
    public Collection<Transition> transitions() {
        return transitionList;
    }
    
    @Override
    public NegativeNode dst(String event, MyBooleanExpression expr) {
    	throw new AssertionError();
    }
    
    public NegativeNode dst(String event, MyBooleanExpression expr, StringActions actions) {
    	for (Transition t : transitionList) {
    		if (t.event().equals(event) && t.expr().equals(expr) && t.actions().equals(actions)) {
    			return (NegativeNode) t.dst();
    		}
    	}
    	return null;
    }
    
    @Override
    public int transitionCount() {
        return transitionList.size();
    }
}

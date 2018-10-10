package structures.mealy;

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

public class NegativeMealyNode extends MealyNode {
    private final List<MealyTransition> transitionList;
    private Set<NegativeMealyNode> loops = null;
    
    NegativeMealyNode(int number) {
        super(number);
        transitionList = new ArrayList<>();
    }
    
    void addLoop(NegativeMealyNode node) {
        if (loops == null) {
            loops = new LinkedHashSet<>();
        }
        loops.add(node);
    }
    
    boolean weakInvalid() {
        return !loops().isEmpty();
    }
    
    public boolean strongInvalid() {
        return loops().contains(this);
    }
    
    public Collection<NegativeMealyNode> loops() {
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
    
    public void addTransition(String event, MyBooleanExpression expr, StringActions actions, NegativeMealyNode dst) {
        transitionList.add(new MealyTransition(this, dst, event, expr, actions));
    }
    
    @Override
    public void removeTransition(MealyTransition transition) {
        throw new AssertionError();
    }
    
    @Override
    public MealyTransition transition(String event, MyBooleanExpression expr) {
        throw new AssertionError();
    }
    
    @Override
    public Collection<MealyTransition> transitions() {
        return transitionList;
    }
    
    @Override
    public NegativeMealyNode dst(String event, MyBooleanExpression expr) {
        throw new AssertionError();
    }
    
    public NegativeMealyNode dst(String event, MyBooleanExpression expr, StringActions actions) {
        for (MealyTransition t : transitionList) {
            if (t.event().equals(event) && t.expr().equals(expr) && t.actions().equals(actions)) {
                return (NegativeMealyNode) t.dst();
            }
        }
        return null;
    }
    
    @Override
    public int transitionCount() {
        return transitionList.size();
    }
}

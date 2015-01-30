/**
 * MultiThreadPredicateFactory.java, 10.05.2008
 */
package ru.ifmo.ltl.grammar.predicate;

import ru.ifmo.automata.statemachine.*;
import ru.ifmo.ltl.grammar.predicate.annotation.Predicate;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The IPredicateFactory implementation that delegate to AbstractPredicateFactory instance.
 * For each thread different instance of AbstractPredicateFactory is used.
 * Call {@link #init(java.util.Collection)} before predicates invocations.
 *
 * @author Kirill Egorov
 */
public class MultiThreadPredicateFactory<S extends IState> implements IPredicateFactory<S> {
    private AbstractPredicateFactory<S> predicates;
    Map<Thread, IPredicateFactory<S>> predicateMap;

    public MultiThreadPredicateFactory(AbstractPredicateFactory<S> predicates) {
        this.predicates = predicates;
    }

    public void init(Collection<? extends Thread> threads) {
        predicateMap = new HashMap<Thread, IPredicateFactory<S>>();
        for (Thread t: threads) {
            try {
                predicateMap.put(t, predicates.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    protected IPredicateFactory<S> getPredicate() {
        return predicateMap.get(Thread.currentThread());
    }

    public void setAutomataState(S state, IStateTransition transition) {
        getPredicate().setAutomataState(state, transition);
    }

    @Predicate
    public Boolean wasEvent(IEvent e) {
        return getPredicate().wasEvent(e);
    }

    @Predicate
    public Boolean isInState(IStateMachine<? extends IState> a, IState s) {
        return getPredicate().isInState(a, s);
    }

    @Predicate
    public Boolean wasInState(IStateMachine<? extends IState> a, IState s) {
        return getPredicate().wasInState(a, s);
    }

    @Predicate
    public boolean cameToFinalState() {
        return getPredicate().cameToFinalState();
    }

    @Predicate
    public Boolean wasAction(IAction z) {
        return getPredicate().wasAction(z);
    }

    @Predicate
    public Boolean wasFirstAction(IAction z) {
        return getPredicate().wasFirstAction(z);
    }

    @Predicate
    public boolean wasTrue(ICondition cond) {
        return getPredicate().wasTrue(cond);
    }

    @Predicate
    public boolean wasFalse(ICondition cond) {
        return getPredicate().wasFalse(cond);
    }
}

/*
 * ConcurrentIntersectionAutomata.java, 08.05.2008
 */
package qbf.egorov.verifier.concurrent;

import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.IBuchiNode;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;
import qbf.egorov.util.CollectionUtils;
import qbf.egorov.verifier.automata.IIntersectionAutomata;
import qbf.egorov.verifier.automata.IntersectionNode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentIntersectionAutomata<S extends IState> implements IIntersectionAutomata<S> {
    private int threadNumber;
    private IPredicateFactory<S> predicates;
    private IBuchiAutomata buchiAutomata;
    private ConcurrentMap<String, IntersectionNode<S>> nodeMap;
    private Collection<? extends DfsThread> threads;

    public ConcurrentIntersectionAutomata(IPredicateFactory<S> predicates, IBuchiAutomata buchi,
                                          int entryNumber, int threadNumber) {
        if (buchi == null || predicates == null || entryNumber < 0 || threadNumber <= 0) {
            throw new IllegalArgumentException();
        }
        this.predicates = predicates;
        this.buchiAutomata = buchi;
        this.threadNumber = threadNumber;

        nodeMap = new ConcurrentHashMap<String, IntersectionNode<S>>(
                CollectionUtils.defaultInitialCapacity(entryNumber),
                CollectionUtils.DEFAULT_LOAD_FACTOR, threadNumber);
    }

    public void setThreads(Collection<? extends DfsThread> threads) {
        if (threads == null || threads.size() != threadNumber) {
            throw new IllegalArgumentException();
        }
        this.threads = threads;
    }

    public IBuchiAutomata getBuchiAutomata() {
        return buchiAutomata;
    }

    public IntersectionNode<S> getNode(S state, IBuchiNode node, int acceptSet) {
        String key = getUniqueKey(state, node, acceptSet);

        IntersectionNode<S> res = nodeMap.get(key);
        if (res == null) {
            res = new IntersectionNode<S>(this, state, node, acceptSet, threads);
            nodeMap.putIfAbsent(key, res);
        }
        return nodeMap.get(key);
    }

    public IPredicateFactory<S> getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(IState state, IBuchiNode node, int acceptSet) {
        return state.getUniqueName() + "_" + node.getID() + "_" + acceptSet;
    }
}

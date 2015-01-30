/*
 * ConcurrentIntersectionAutomata.java, 08.05.2008
 */
package ru.ifmo.verifier.concurrent;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.ltl.grammar.predicate.IPredicateFactory;
import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.ltl.buchi.IBuchiNode;
import ru.ifmo.verifier.automata.IntersectionNode;
import ru.ifmo.verifier.automata.IIntersectionAutomata;
import ru.ifmo.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConcurrentIntersectionAutomata<S extends IState> implements IIntersectionAutomata<S> {
    private int threadNumber;
    private IPredicateFactory<S> predicates;
    private IBuchiAutomata buchiAutomata;

    private ConcurrentMap<String, IntersectionNode<S>> nodeMap;
//    private ConcurrentMap<String, Object> lockMap;

//    private Map<S, Map<IBuchiNode, Map<Integer, IntersectionNode<S>>>> nodeMap;
//    private Map<S, ConcurrentMap<IBuchiNode, Lock>> lockMap;
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
//        lockMap = new ConcurrentHashMap<String, Object>(
//                CollectionUtils.defaultInitialCapacity(entryNumber),
//                CollectionUtils.DEFAULT_LOAD_FACTOR, threadNumber);
//        nodeMap = new ConcurrentHashMap<S, Map<IBuchiNode, Map<Integer, IntersectionNode<S>>>>(
//                initialCapacity, DEFAULT_LOAD_FACTOR, threadNumber);
//        lockMap = new ConcurrentHashMap<S, ConcurrentMap<IBuchiNode, Lock>>(
//                initialCapacity, DEFAULT_LOAD_FACTOR, threadNumber);
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

//        Object lockObj = lockMap.get(key);
//        if (lockObj == null) {
//            lockMap.putIfAbsent(key, new Object());
//        }
//        lockObj = lockMap.get(key);

        IntersectionNode<S> res = nodeMap.get(key);
        if (res == null) {
//            synchronized(lockObj) {
//                res = nodeMap.get(key);
//                if (res == null) {
                    res = new IntersectionNode<S>(this, state, node, acceptSet, threads);
                    nodeMap.putIfAbsent(key, res);
//                }
//            }
        }
//        return res;
        return nodeMap.get(key);

        /*Map<IBuchiNode, Map<Integer, IntersectionNode<S>>> buchiMap = nodeMap.get(state);
        if (buchiMap == null) {
            synchronized (state) {
                buchiMap = nodeMap.get(state);
                if (buchiMap == null) {
                    buchiMap = new ConcurrentHashMap<IBuchiNode, Map<Integer, IntersectionNode<S>>>(
                            buchiAutomata.size() * buchiAutomata.getAcceptSetsCount(),
                            DEFAULT_LOAD_FACTOR, threadNumber);
                    nodeMap.put(state, buchiMap);
                }
            }
        }

        Map<Integer, IntersectionNode<S>> acceptMap = buchiMap.get(node);
        IntersectionNode<S> res;
        if (acceptMap == null) {
            Lock lock = getLock(state, node);
            lock.lock();
            try {
                acceptMap = buchiMap.get(node);
                if (acceptMap == null) {
                    acceptMap = new ConcurrentHashMap<Integer, IntersectionNode<S>>(
                            buchiAutomata.getAcceptSetsCount(), DEFAULT_LOAD_FACTOR, threadNumber);
                    buchiMap.put(node, acceptMap);

                    res = new IntersectionNode<S>(this, state, node, acceptSet, threads);
                    acceptMap.put(acceptSet, res);
                    return res;
                }
            } finally {
                lock.unlock();
            }
        }

        res = acceptMap.get(acceptSet);
        if (res == null) {
            Lock lock = getLock(state, node);
            lock.lock();
            try {
                res = acceptMap.get(acceptSet);
                if (res == null) {
                    res = new IntersectionNode<S>(this, state, node, acceptSet, threads);
                    acceptMap.put(acceptSet, res);
                }
            } finally {
                lock.unlock();
            }
        }
        return res;*/
    }

    /*protected Lock getLock(S state, IBuchiNode node) {
        ConcurrentMap<IBuchiNode, Lock> buchiMap = lockMap.get(state);
        if (buchiMap == null) {
            synchronized (state) {
                buchiMap = lockMap.get(state);
                if (buchiMap == null) {
                    buchiMap = new ConcurrentHashMap<IBuchiNode, Lock>(buchiAutomata.size(),
                            DEFAULT_LOAD_FACTOR, threadNumber);
                    lockMap.put(state, buchiMap);

                    buchiMap.put(node, new ReentrantLock());
                }
            }
        }

        Lock lock = buchiMap.get(node);
        if (lock == null) {
            buchiMap.putIfAbsent(node, new ReentrantLock());
        }

        return buchiMap.get(node);
    }*/

    public IPredicateFactory<S> getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(IState state, IBuchiNode node, int acceptSet) {
        return state.getUniqueName() + "_" + node.getID() + "_" + acceptSet;
    }
}

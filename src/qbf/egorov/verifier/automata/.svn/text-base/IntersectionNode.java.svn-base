/**
 * DfsNode.java, 12.04.2008
 */
package ru.ifmo.verifier.automata;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.automata.statemachine.IStateTransition;
import ru.ifmo.automata.INode;
import ru.ifmo.ltl.buchi.IBuchiNode;
import ru.ifmo.ltl.buchi.ITransitionCondition;
import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.verifier.IInterNode;
import ru.ifmo.verifier.concurrent.DfsThread;

import java.util.*;

/**
 * Node received during state machine and buchi automata intersection
 *
 * @author Kirill Egorov
 */
public class IntersectionNode<S extends IState>
        implements INode<IIntersectionTransition>, IInterNode {
    private final IIntersectionAutomata<S> automata;
    private final S state;
    private final IBuchiNode node;
    private final int acceptSet;
    private final int nextAcceptSet;
    private final boolean terminal;

    private final TransitionIterator iterator;
    private ArrayList<TransitionIterator> threadIterators;

    private boolean[] owners;

    public IntersectionNode(IIntersectionAutomata<S> automata, S state, IBuchiNode node, int acceptSet) {
        this(automata, state, node, acceptSet, Collections.<DfsThread>emptyList());
    }

    public IntersectionNode(IIntersectionAutomata<S> automata, S state, IBuchiNode node,
                            int acceptSet, Collection<? extends DfsThread> threads) {
        if (state == null || node == null) {
            throw new IllegalArgumentException();
        }
        if (threads == null) {
            throw new NullPointerException("Collection of threads hasn't been initialized yet");
        }

        this.automata = automata;
        this.state = state;
        this.node = node;
        this.acceptSet = acceptSet;

        IBuchiAutomata buchi = automata.getBuchiAutomata();
        terminal = buchi.getAcceptSet(acceptSet).contains(node);
        nextAcceptSet = (terminal) ? (acceptSet + 1) % buchi.getAcceptSetsCount()
                                   : acceptSet;

        iterator = new TransitionIterator();

        threadIterators = new ArrayList<TransitionIterator>(threads.size());
        for (DfsThread t: threads) {
            threadIterators.add(new TransitionIterator());
        }
        owners = new boolean[threads.size()];
    }

    public IState getState() {
        return state;
    }

    public IBuchiNode getNode() {
        return node;
    }

    public int getAcceptSet() {
        return acceptSet;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public Collection<IIntersectionTransition> getOutcomingTransitions() {
        throw new UnsupportedOperationException("use next() method instead");
    }

    public void addOwner(int threadId) {
        owners[threadId] = true;
    }

    public void removeOwner(int threadId) {
        owners[threadId] = false;
    }

    public boolean isOwner(int threadId) {
        return owners[threadId];
    }

    public IIntersectionTransition<S> next(int threadId) {
        if (threadId < 0) {
            synchronized (iterator) {
                return (iterator.hasNext()) ? iterator.next() : null;
            }
        } else {
            Iterator<IIntersectionTransition<S>> iter = threadIterators.get(threadId);
            return(iter.hasNext()) ? iter.next() : null;
        }
    }

    public void resetIterator(int threadId) {
        if (threadId < 0) {
            synchronized (iterator) {
                iterator.reset();
            }
        } else {
            threadIterators.get(threadId).reset();
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntersectionNode)) {
            return false;
        }

        IntersectionNode intersectionNode = (IntersectionNode) o;

        return node.equals(intersectionNode.node) && state.equals(intersectionNode.state)
                && (acceptSet == intersectionNode.acceptSet);
    }

    public int hashCode() {
        int result;
        result = state.hashCode();
        result = 31 * result + node.hashCode();
        return result;
    }

    public String toString() {
        return String.format("[\"%s\", %d, %d]", state.getName(), node.getID(), acceptSet);
    }

    private class TransitionIterator implements Iterator<IIntersectionTransition<S>> {

        private Iterator<IStateTransition> stateIter;
        private Iterator<Map.Entry<ITransitionCondition, IBuchiNode>> nodeIter;

        private IStateTransition nextStateTransition;
        private IIntersectionTransition<S> next;

        private TransitionIterator() {
            reset();
        }

        public void reset() {
            stateIter = state.getOutcomingTransitions().iterator();
            nodeIter = node.getTransitions().entrySet().iterator();
            if (stateIter.hasNext()) {
                nextStateTransition = stateIter.next();
            }
        }

        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            Map.Entry<ITransitionCondition, IBuchiNode> nextBuchiTransition;

            automata.getPredicates().setAutomataState(state, nextStateTransition);
            while (true) {
                if (nodeIter.hasNext()) {
                    nextBuchiTransition = nodeIter.next();
                    if (nextBuchiTransition.getKey().getValue()) {
                        IntersectionNode<S> node = automata.getNode((S) nextStateTransition.getTarget(),
                                nextBuchiTransition.getValue(), nextAcceptSet);
                        next = new IntersectionTransition<S>(nextStateTransition, node);
                        return true;
                    }
                } else if (stateIter.hasNext()) {
                    nextStateTransition = stateIter.next();
                    nodeIter = node.getTransitions().entrySet().iterator();
                    automata.getPredicates().setAutomataState(state, nextStateTransition);
                } else {
                    next = null;
                    return false;
                }
            }
        }

        public IIntersectionTransition<S> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            IIntersectionTransition<S> res = next;
            next = null;
            return res;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}

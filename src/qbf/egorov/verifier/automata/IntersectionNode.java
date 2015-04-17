/**
 * DfsNode.java, 12.04.2008
 */
package qbf.egorov.verifier.automata;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import qbf.egorov.ltl.buchi.BuchiAutomata;
import qbf.egorov.ltl.buchi.BuchiNode;
import qbf.egorov.ltl.buchi.TransitionCondition;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateTransition;

/**
 * Node received during state machine and buchi automata intersection
 *
 * @author Kirill Egorov
 */
public class IntersectionNode<S extends IState> {
    private final IntersectionAutomata<S> automata;
    public final S state;
    public final BuchiNode node;
    public final int acceptSet;
    private final int nextAcceptSet;
    public final boolean terminal;
    private final TransitionIterator iterator;

    public IntersectionNode(IntersectionAutomata<S> automata, S state, BuchiNode node, int acceptSet) {
        this.automata = automata;
        this.state = state;
        this.node = node;
        this.acceptSet = acceptSet;

        BuchiAutomata buchi = automata.getBuchiAutomata();
        terminal = buchi.getAcceptSet(acceptSet).contains(node);
        nextAcceptSet = terminal ? (acceptSet + 1) % buchi.getAcceptSetsCount()
                                 : acceptSet;

        iterator = new TransitionIterator();
    }

    public IntersectionTransition<S> next() {
    	return iterator.hasNext() ? iterator.next() : null;
    }

    public void resetIterator() {
    	iterator.reset();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntersectionNode)) {
            return false;
        }

        @SuppressWarnings("unchecked")
		IntersectionNode<S> intersectionNode = (IntersectionNode<S>) o;

        return node.equals(intersectionNode.node) && state.equals(intersectionNode.state)
                && (acceptSet == intersectionNode.acceptSet);
    }

    @Override
    public int hashCode() {
        int result;
        result = state.hashCode();
        result = 31 * result + node.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("[\"%s\", %d, %d]", state.getName(), node.getID(), acceptSet);
    }

    private class TransitionIterator implements Iterator<IntersectionTransition<S>> {
        private Iterator<IStateTransition> stateIter;
        private Iterator<Map.Entry<TransitionCondition, BuchiNode>> nodeIter;

        private IStateTransition nextStateTransition;
        private IntersectionTransition<S> next;

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

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            Map.Entry<TransitionCondition, BuchiNode> nextBuchiTransition;

            automata.getPredicates().setAutomataState(state, nextStateTransition);
            while (true) {
                if (nodeIter.hasNext()) {
                    nextBuchiTransition = nodeIter.next();
                    if (nextBuchiTransition.getKey().getValue()) {
                        @SuppressWarnings("unchecked")
						IntersectionNode<S> node = automata.getNode((S) nextStateTransition.getTarget(),
                                nextBuchiTransition.getValue(), nextAcceptSet);
                        next = new IntersectionTransition<>(nextStateTransition, node);
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

        @Override
        public IntersectionTransition<S> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            IntersectionTransition<S> res = next;
            next = null;
            return res;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

		@Override
		public void forEachRemaining(Consumer<? super IntersectionTransition<S>> action) {
			throw new UnsupportedOperationException();
		}
    }
}

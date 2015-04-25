/**
 * DfsNode.java, 12.04.2008
 */
package qbf.egorov.verifier;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import qbf.egorov.ltl.buchi.BuchiAutomaton;
import qbf.egorov.ltl.buchi.BuchiNode;
import qbf.egorov.ltl.buchi.TransitionCondition;
import qbf.egorov.statemachine.SimpleState;
import qbf.egorov.statemachine.StateTransition;

/**
 * Node received during state machine and buchi automata intersection
 *
 * @author Kirill Egorov
 */
public class IntersectionNode {
    private final IntersectionAutomata automata;
    public final SimpleState state;
    public final BuchiNode node;
    public final boolean terminal;
    private final TransitionIterator iterator;

    public IntersectionNode(IntersectionAutomata automata, SimpleState state, BuchiNode node) {
        this.automata = automata;
        this.state = state;
        this.node = node;

        BuchiAutomaton buchi = automata.getBuchiAutomata();
        terminal = buchi.getAcceptSet().contains(node);
        iterator = new TransitionIterator();
    }

    public IntersectionTransition next() {
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

		IntersectionNode intersectionNode = (IntersectionNode) o;

        return node.equals(intersectionNode.node) && state.equals(intersectionNode.state);
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
        return String.format("[\"%s\", %d]", state.name, node.getID());
    }

    private class TransitionIterator implements Iterator<IntersectionTransition> {
        private Iterator<StateTransition> stateIter;
        private Iterator<Map.Entry<TransitionCondition, BuchiNode>> nodeIter;

        private StateTransition nextStateTransition;
        private IntersectionTransition next;

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
						IntersectionNode node = automata.getNode(nextStateTransition.getTarget(),
                                nextBuchiTransition.getValue());
                        next = new IntersectionTransition(nextStateTransition, node);
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
        public IntersectionTransition next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            IntersectionTransition res = next;
            next = null;
            return res;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

		@Override
		public void forEachRemaining(Consumer<? super IntersectionTransition> action) {
			throw new UnsupportedOperationException();
		}
    }
}

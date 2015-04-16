/**
 * DfsNode.java, 12.04.2008
 */
package qbf.egorov.verifier.automata;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import qbf.egorov.automata.INode;
import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.IBuchiNode;
import qbf.egorov.ltl.buchi.ITransitionCondition;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateTransition;
import qbf.egorov.verifier.IInterNode;

/**
 * Node received during state machine and buchi automata intersection
 *
 * @author Kirill Egorov
 */
public class IntersectionNode<S extends IState>
        implements INode<IIntersectionTransition<S>>, IInterNode {
    private final IIntersectionAutomata<S> automata;
    private final S state;
    private final IBuchiNode node;
    private final int acceptSet;
    private final int nextAcceptSet;
    private final boolean terminal;

    private final TransitionIterator iterator;

    public IntersectionNode(IIntersectionAutomata<S> automata, S state, IBuchiNode node,
                            int acceptSet) {
        if (state == null || node == null) {
            throw new IllegalArgumentException();
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

    public Collection<IIntersectionTransition<S>> getOutcomingTransitions() {
        throw new UnsupportedOperationException("use next() method instead");
    }

    public IIntersectionTransition<S> next() {
    	return iterator.hasNext() ? iterator.next() : null;
    }

    public void resetIterator() {
    	iterator.reset();
    }

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
                        @SuppressWarnings("unchecked")
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

		@Override
		public void forEachRemaining(
				Consumer<? super IIntersectionTransition<S>> action) {
			// TODO Auto-generated method stub
			throw new AssertionError();
		}
    }
}

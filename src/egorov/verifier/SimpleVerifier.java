/**
 * SimpleVerifier.java, 06.04.2008
 */
package egorov.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import egorov.ltl.buchi.BuchiAutomaton;
import egorov.ltl.buchi.BuchiNode;
import egorov.ltl.grammar.PredicateFactory;
import egorov.statemachine.SimpleState;

/**
 * Simple IVerifier implementation. Use one thread and can't be used concurrently.
 *
 * @author Kirill Egorov
 */
public class SimpleVerifier {
    private final SimpleState initState;
    
    public SimpleVerifier(SimpleState initState) {
        this.initState = initState;
    }

    public Pair<List<IntersectionTransition>, Integer> verify(BuchiAutomaton buchi,
    		PredicateFactory predicates, Set<BuchiNode> finiteCounterexampleNodes) {
        final IntersectionAutomata automata = new IntersectionAutomata(predicates, buchi);
        return bfs(automata.getNode(initState, buchi.startNode()), finiteCounterexampleNodes);
    }

    private class QueueElement {
    	private final IntersectionTransition trans;
    	private final IntersectionNode dest;
    	private final QueueElement predecessor;
    	
		public QueueElement(IntersectionTransition trans, QueueElement predecessor) {
			this.trans = trans;
			this.dest = trans.target;
			this.predecessor = predecessor;
		}
		
		@Override
		public String toString() {
			return "[" + trans + " -> " + dest + "]";
		}
    }
    
    public enum Criterion { MIN_LENGTH, MIN_LOOP };
    private static Criterion CRITERION = Criterion.MIN_LENGTH;
    
    public static void setCriterion(Criterion criterion) {
    	CRITERION = criterion;
    }
    
    private Pair<List<IntersectionTransition>, Integer> bfs(IntersectionNode initialNode,
    		Set<BuchiNode> finiteCounterexampleNodes) {
    	final Deque<QueueElement> queue = new LinkedList<>();
    	final Set<IntersectionNode> visited = new HashSet<>();
        queue.addLast(new QueueElement(new IntersectionTransition(null, initialNode), null));
        final List<Pair<List<IntersectionTransition>, Integer>> counterexamples = new ArrayList<>();
        
        Pair<List<IntersectionTransition>, Integer> finiteCounterexample = null;
        
        while (!queue.isEmpty()) {
        	final QueueElement element = queue.pollFirst();
        	if (!visited.contains(element.dest)) {
	        	visited.add(element.dest);
	
	        	if (finiteCounterexample == null && finiteCounterexampleNodes.contains(element.dest.node)) {
	        		// finite counterexample exists
	        		final List<IntersectionTransition> path = new ArrayList<>();
	        		QueueElement elem = element;
            		do {
            			path.add(elem.trans);
            			elem = elem.predecessor;
            		} while (elem != null);
            		path.remove(path.size() - 1);
            		Collections.reverse(path);
            		finiteCounterexample = Pair.of(path, 0);
	        	}
	        	if (element.dest.terminal) {
	        		secondBfs(element.dest, element, counterexamples);
	        	}
	        	
	        	element.dest.resetIterator();
	        	IntersectionTransition trans;
	        	while ((trans = element.dest.next()) != null) {
	        		if (trans.transition.event != null) {
	        			queue.addLast(new QueueElement(trans, element));
	        		}
	        	}
        	}
        }
    	if (finiteCounterexample != null) {
    		counterexamples.add(finiteCounterexample);
    	}
        if (counterexamples.isEmpty()) {
        	return Pair.of(new ArrayList<>(), 0);
        } else {
        	int minIndex = 0;
        	// the counterexample of the minimum length OR minimum loop length
        	// if the lengths are equal, then with the minimum loop size
        	for (int i = 1; i < counterexamples.size(); i++) {
        		final int currentSize = counterexamples.get(i).getLeft().size();
        		final int bestSize = counterexamples.get(minIndex).getLeft().size();
        		final int currentLoopSize = counterexamples.get(i).getRight();
        		final int bestLoopSize = counterexamples.get(minIndex).getRight();
        		
        		boolean condition = CRITERION == Criterion.MIN_LENGTH
        				? (currentSize < bestSize
        					|| currentSize == bestSize && currentLoopSize < bestLoopSize)
        				: (currentLoopSize < bestLoopSize
        					|| currentLoopSize == bestLoopSize && currentSize < bestSize);
        		
        		if (condition) {
        			minIndex = i;
        		}
        	}
        	return counterexamples.get(minIndex);
        }
    }
    
    private void secondBfs(IntersectionNode initialNode, QueueElement parentQueueElement,
    		List<Pair<List<IntersectionTransition>, Integer>> counterexamples) {
    	final Deque<QueueElement> queue = new LinkedList<>();
    	final Set<IntersectionNode> visited = new HashSet<>();
        queue.addLast(new QueueElement(new IntersectionTransition(null, initialNode), null));
        
        while (!queue.isEmpty()) {
        	final QueueElement element = queue.pollFirst();
        	if (visited.contains(element.dest)) {
        		if (element.dest == initialNode && element.predecessor != null) {
	        		// found a loop
        			final List<IntersectionTransition> path = new ArrayList<>();
            		QueueElement elem = element;
            		do {
            			path.add(elem.trans);
            			elem = elem.predecessor;
            		} while (elem != null);
            		path.remove(path.size() - 1);
            		int loopLength = path.size();
            		elem = parentQueueElement;
            		do {
            			path.add(elem.trans);
            			elem = elem.predecessor;
            		} while (elem != null);
            		path.remove(path.size() - 1);
            		Collections.reverse(path);
	        		counterexamples.add(Pair.of(path, loopLength));
	        		return;
	        	}
        	} else {
	        	visited.add(element.dest);
	        	IntersectionTransition trans;
	        	element.dest.resetIterator();
	        	
	        	while ((trans = element.dest.next()) != null) {
	        		if (trans.transition.event != null) {
	        			queue.addLast(new QueueElement(trans, element));
	        		}
	        	}
        	}
        }
    }
}

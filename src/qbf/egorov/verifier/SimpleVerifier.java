/**
 * SimpleVerifier.java, 06.04.2008
 */
package qbf.egorov.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.buchi.BuchiAutomata;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.SimpleState;

/**
 * Simple IVerifier implementation. Use one thread and can't be used cuncurrently.
 *
 * @author Kirill Egorov
 */
public class SimpleVerifier {
    private final SimpleState initState;
    
    public SimpleVerifier(SimpleState initState) {
        this.initState = initState;
    }

    public Pair<List<IntersectionTransition>, Integer> verify(BuchiAutomata buchi, IPredicateFactory predicates) {
        IntersectionAutomata automata = new IntersectionAutomata(predicates, buchi);
        //System.out.println("\n" + buchi);
        return bfs(automata.getNode(initState, buchi.getStartNode(), 0));
    }

    private class QueueElement {
    	private final IntersectionTransition trans;
    	private final IntersectionNode dest;
    	private final QueueElement predecessor;
    	
		public QueueElement(IntersectionTransition trans,
				QueueElement predecessor) {
			this.trans = trans;
			this.dest = trans.target;
			this.predecessor = predecessor;
		}
		
		@Override
		public String toString() {
			return "[" + trans + " -> " + dest + "]";
		}
    }
    
    private Pair<List<IntersectionTransition>, Integer> bfs(IntersectionNode initialNode) {
    	final Deque<QueueElement> queue = new LinkedList<>();
    	final Set<IntersectionNode> visited = new HashSet<>();
        queue.addLast(new QueueElement(new IntersectionTransition(null, initialNode), null));
        final List<Pair<List<IntersectionTransition>, Integer>> counterexamples = new ArrayList<>();
        
        while (!queue.isEmpty()) {
        	final QueueElement element = queue.pollFirst();
        	if (!visited.contains(element.dest)) {
	        	visited.add(element.dest);
	
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
        if (counterexamples.isEmpty()) {
        	return Pair.of(new ArrayList<>(), 0);
        } else {
        	//System.out.println(counterexamples);
        	int minIndex = 0;
        	for (int i = 1; i < counterexamples.size(); i++) {
        		if (counterexamples.get(i).getLeft().size() < counterexamples.get(minIndex).getLeft().size()) {
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
        			List<IntersectionTransition> path = new ArrayList<>();
            		QueueElement elem = element;
            		do  {
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

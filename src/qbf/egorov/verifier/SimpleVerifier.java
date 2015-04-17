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

import qbf.egorov.ltl.buchi.BuchiAutomata;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;
import qbf.egorov.transducer.verifier.TransitionCounter;
import qbf.egorov.verifier.automata.IntersectionAutomata;
import qbf.egorov.verifier.automata.IntersectionNode;
import qbf.egorov.verifier.automata.IntersectionTransition;

/**
 * Simple IVerifier implementation. Use one thread and can't be used cuncurrently.
 *
 * @author Kirill Egorov
 */
public class SimpleVerifier {
    private final IState initState;
    
    public SimpleVerifier(IState initState) {
        this.initState = initState;
    }

    public List<IntersectionTransition<?>> verify(BuchiAutomata buchi, IPredicateFactory<IState> predicates,
                                                TransitionCounter counter) {
        IntersectionAutomata<IState> automata = new IntersectionAutomata<>(predicates, buchi);
        IntersectionNode<IState> initial = automata.getNode(initState, buchi.getStartNode(), 0);
        List<IntersectionTransition<?>> res = mainDfs(initial, counter);
        Collections.reverse(res);
        return res;
    }
    
    private List<IntersectionTransition<?>> mainDfs(IntersectionNode<IState> initialNode, TransitionCounter counter) {
        Deque<IntersectionTransition<?>> transitionStack = new LinkedList<>();
        Deque<IntersectionNode<?>> stack = new LinkedList<>();
        Set<IntersectionNode<?>> visited = new HashSet<>();
        visited.add(initialNode);
        stack.push(initialNode);
        transitionStack.push(new IntersectionTransition<>(null, initialNode));

        while (!stack.isEmpty()) {
            IntersectionNode<?> node = stack.getFirst();
            IntersectionTransition<?> trans = node.next();
            
            if (trans != null) {
            	if (trans.getTransition().getEvent() == null) {
                	continue;
                }
            	IntersectionNode<?> child = trans.getTarget();
                if (!visited.contains(child)) {
                	visited.add(child);
                	stack.push(child);
                    transitionStack.push(trans);
                }
            } else {
            	counter.process(node.state);
                if (node.terminal && secondDfs(node, stack, transitionStack)) {
                    return new ArrayList<>(transitionStack);
                } else {
                	stack.pop();
                	transitionStack.pop();
                }
            }
        }
        return new ArrayList<>();
    }
    
    private boolean secondDfs(IntersectionNode<?> initialNode, Deque<IntersectionNode<?>> mainStack,
    		Deque<IntersectionTransition<?>> mainTransStack) {
    	Deque<IntersectionTransition<?>> transitionStack = new LinkedList<>();
    	Deque<IntersectionNode<?>> stack = new LinkedList<>();
        Set<IntersectionNode<?>> visited = new HashSet<>();
    	initialNode.resetIterator();
        visited.add(initialNode);
        stack.push(initialNode);
        transitionStack.push(new IntersectionTransition<>(null, initialNode));
        
        while (!stack.isEmpty()) {
            IntersectionNode<?> node = stack.getFirst();
            IntersectionTransition<?> trans = node.next();
            
            if (trans != null) {
            	if (trans.getTransition().getEvent() == null) {
                	continue;
                }
            	IntersectionNode<?> child = trans.getTarget();
            	if (mainStack.contains(child)) {
            		if (transitionStack.size() != 1) {
            			throw new AssertionError();
            		}
            		mainTransStack.push(trans);
                    return true;
                } else if (!visited.contains(child)) {
                	visited.add(child);
                	stack.push(child);
                	transitionStack.push(trans);
                    child.resetIterator();
                }
            } else {
            	stack.pop();
            	transitionStack.pop();
            }
        }
        return false;
    }
}

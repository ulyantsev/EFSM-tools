package egorov.verifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import egorov.ltl.buchi.BuchiAutomaton;
import egorov.ltl.buchi.BuchiNode;
import egorov.ltl.buchi.TransitionCondition;
import egorov.ltl.grammar.BooleanNode;
import egorov.ltl.grammar.IExpression;

public class FiniteCounterexampleNodeSearcher {
    public static Set<BuchiNode> findCounterexampleBuchiStates(BuchiAutomaton a) {
    	final Set<BuchiNode> nodesWithDevilTransitions = nodesWithDevilTransitions(a);
    	
    	final Set<BuchiNode> nodesWithRejectingLoops = new LinkedHashSet<>();
    	for (BuchiNode node : a.getNodes()) {
    		if (hasRejectingLoop(node, a, nodesWithDevilTransitions)) {
    			nodesWithRejectingLoops.add(node);
    		}
    	}
    	
    	final Set<BuchiNode> result = new HashSet<>(a.getNodes());
    	for (BuchiNode node : a.getNodes()) {
    		final Set<BuchiNode> reachabilitySet = reachibilitySet(node);
    		for (BuchiNode loopStart : reachabilitySet) {
    			if (nodesWithRejectingLoops.contains(loopStart)) {
    				result.remove(node);
    				break;
    			}
    		}
    	}
    	
    	return result;
    }
    
    private static Set<BuchiNode> reachibilitySet(BuchiNode node) {
    	final Set<BuchiNode> visited = new HashSet<>();
    	visited.add(node);
    	final Deque<BuchiNode> queue = new ArrayDeque<>();
    	queue.add(node);
    	while (!queue.isEmpty()) {
    		final BuchiNode n = queue.removeFirst();
    		for (BuchiNode child : n.getTransitions().values()) {
    			if (!visited.contains(child)) {
    				visited.add(child);
    				queue.add(child);
    			}
    		}
    	}
    	return visited;
    }
    
    private static Set<BuchiNode> nodesWithDevilTransitions(BuchiAutomaton a) {
    	final Set<BuchiNode> result = new HashSet<>();
    	for (BuchiNode node : a.getNodes()) {
    		// if transition conditions are not complete, there is a rejecting loop!
    		final Set<IExpression<Boolean>> allExpressions = new HashSet<>();
    		final Set<TransitionCondition> conditions = node.getTransitions().keySet();
    		for (TransitionCondition condition : conditions) {
    			allExpressions.addAll(condition.expressions());
				allExpressions.addAll(condition.negativeExpressions());
    		}
    		allExpressions.removeIf(x -> x instanceof BooleanNode);
    		final List<IExpression<Boolean>> allExpressionsList = new ArrayList<>(allExpressions);
    		
    		int maxI = 1 << allExpressions.size();
    		for (int i = 0; i < maxI; i++) {
    			final Set<IExpression<Boolean>> positive = new HashSet<>();
    			for (int j = 0; j < allExpressions.size(); j++) {
    				if (((i >> j) & 1) == 1) {
    					positive.add(allExpressionsList.get(j));
    				}
    			}
    			boolean transitionPassed = false; // either condition passes
    			for (TransitionCondition condition : conditions) {
        			boolean transitionConditionPassed = true; // all expressions pass
    				for (IExpression<Boolean> expression : allExpressions) {
    					final boolean failed = positive.contains(expression)
    						&& condition.negativeExpressions().contains(expression)
    						|| !positive.contains(expression)
							&& condition.expressions().contains(expression);
    					if (failed) {
    						transitionConditionPassed = false;
    						break;
    					}
    				}
    				if (condition.expressions().stream()
    						.anyMatch(x -> x instanceof BooleanNode && x.toString().equals("false"))) {
    					transitionConditionPassed = false;
    				}
    				if (condition.negativeExpressions().stream()
    						.anyMatch(x -> x instanceof BooleanNode && x.toString().equals("true"))) {
    					transitionConditionPassed = false;
    				}
    				if (transitionConditionPassed) {
    					transitionPassed = true;
    					break;
    				}
    			}
    			if (!transitionPassed) {
    				result.add(node);
    			}
    		}
    	}
    	return result;
    }
    
    private static boolean hasRejectingLoop(BuchiNode node, BuchiAutomaton a,
    		Set<BuchiNode> nodesWithDevilTransitions) {
    	final Set<BuchiNode> visited = new HashSet<>();
    	visited.add(node);
    	final Deque<BuchiNode> queue = new ArrayDeque<>();
    	queue.add(node);
    	while (!queue.isEmpty()) {
    		final BuchiNode n = queue.removeFirst();
    		if (nodesWithDevilTransitions.contains(n)) {
    			return true;
    		}
    		for (BuchiNode child : n.getTransitions().values()) {
    			if (a.getAcceptSet().contains(child)) {
    				continue;
    			} else if (visited.contains(child)) {
    				return true;
    			} else {
    				visited.add(child);
    				queue.add(child);
    			}
    		}
    	}
    	return false;
    }
}
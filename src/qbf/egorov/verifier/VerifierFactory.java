/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.verifier;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.LtlParser;
import qbf.egorov.ltl.buchi.BuchiAutomata;
import qbf.egorov.ltl.buchi.BuchiNode;
import qbf.egorov.ltl.buchi.TransitionCondition;
import qbf.egorov.ltl.buchi.translator.JLtl2baTranslator;
import qbf.egorov.ltl.grammar.BooleanNode;
import qbf.egorov.ltl.grammar.IExpression;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.LtlUtils;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.ltl.grammar.predicate.PredicateFactory;
import qbf.egorov.statemachine.Action;
import qbf.egorov.statemachine.ControlledObject;
import qbf.egorov.statemachine.EventProvider;
import qbf.egorov.statemachine.SimpleState;
import qbf.egorov.statemachine.StateMachine;
import qbf.egorov.statemachine.StateTransition;
import qbf.egorov.statemachine.StateType;
import qbf.egorov.transducer.FST;
import qbf.egorov.transducer.Transition;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class VerifierFactory {
    private final AutomataContext context;
    private final IPredicateFactory predicates = new PredicateFactory();
    private final LtlParser parser;
    private BuchiAutomata[] preparedFormulas;

    public VerifierFactory(String[] setOfInputs, String[] setOfOutputs) {
        context = new AutomataContext(
        		new ControlledObject(setOfOutputs),
        		new EventProvider(getEvents(setOfInputs))
        );
        parser = new LtlParser(context, predicates);
    }
    
    @SuppressWarnings("unchecked")
	public void prepareFormulas(List<String> formulas) throws LtlParseException {
    	 JLtl2baTranslator translator = new JLtl2baTranslator();

         int j = 0;
         preparedFormulas = new BuchiAutomata[formulas.size()];
         for (String f : formulas) {
             LtlNode node = parser.parse(f);
             node = LtlUtils.getInstance().neg(node);
             preparedFormulas[j++] = translator.translate(node);
         }
         
         finiteCounterexampleBuchiStates = new Set[formulas.size()];
         for (int i = 0; i < formulas.size(); i++) {
        	 finiteCounterexampleBuchiStates[i] = new FiniteCounterexampleNodeSearcher()
        			 .findCounterexampleBuchiStates(preparedFormulas[i]);
        	 //System.out.println(preparedFormulas[i]);
        	 //System.out.println(finiteCounterexampleBuchiStates[i]);
         }
    }
    
    private Set<BuchiNode>[] finiteCounterexampleBuchiStates;
    
    private static class FiniteCounterexampleNodeSearcher {
	    private Set<BuchiNode> findCounterexampleBuchiStates(BuchiAutomata a) {
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
	    
	    private Set<BuchiNode> reachibilitySet(BuchiNode node) {
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
	    
	    private Set<BuchiNode> nodesWithDevilTransitions(BuchiAutomata a) {
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
	    
	    private boolean hasRejectingLoop(BuchiNode node, BuchiAutomata a,
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
	    			if (a.getAcceptSet(0).contains(child)) {
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

    public void configureStateMachine(FST fst) {
        Transition[][] states = fst.getStates();

        ControlledObject co = context.getControlledObject();
        EventProvider ep = context.getEventProvider();
        StateMachine machine = new StateMachine("A1");

		SimpleState[] statesArr = new SimpleState[states.length];
		for (int i = 0; i < states.length; i++) {
			statesArr[i] = new SimpleState("" + i,
                    (fst.getInitialState() == i) ? StateType.INITIAL : StateType.NORMAL, 
                    Collections.emptyList());
		}
		for (int i = 0; i < states.length; i++) {
			Transition[] currentState = states[i];
			for (Transition t : currentState) {
				StateTransition out = new StateTransition(
                        ep.getEvent(extractEvent(t.input())), statesArr[t.newState()]);

                for (String a: t.output()) {
                    Action action = co.getAction(a);
                    if (action != null) {
                        out.addAction(co.getAction(a));
                    }
                }
				statesArr[i].addOutcomingTransition(out);
			}
			machine.addState(statesArr[i]);
		}
        context.setStateMachine(machine);
    }

    public static class Counterexample {
    	private final List<String> events;
    	public final int loopLength;
    	
    	public List<String> events() {
    		return Collections.unmodifiableList(events);
    	}
    	
		public Counterexample(List<String> events, int loopLength) {
			this.events = events;
			this.loopLength = loopLength;
		}
		
		public boolean isEmpty() {
			return events.isEmpty();
		}
		
		@Override
		public String toString() {
			return "[" + events + ", loop " + loopLength + "]";
		}
    }
    
    // returns counterexamples
    public List<Counterexample> verify() {
        List<Counterexample> counterexamples = new ArrayList<>();
        SimpleVerifier verifier = new SimpleVerifier(context.getStateMachine().getInitialState());
        for (int i = 0; i < preparedFormulas.length; i++) {
        	BuchiAutomata buchi = preparedFormulas[i];
            Pair<List<IntersectionTransition>, Integer> list = verifier.verify(buchi, predicates,
            		finiteCounterexampleBuchiStates[i]);
            
            if (!list.getLeft().isEmpty()) {
                List<String> eventList = list.getLeft().stream()
                    	.map(t -> String.valueOf(t.transition.event))
                    	.collect(Collectors.toList());
                counterexamples.add(new Counterexample(eventList, list.getRight()));
            } else {
            	counterexamples.add(new Counterexample(Collections.emptyList(), 0));
            }
        }
        return counterexamples;
    }

    /**
     * Remove [expr] from input, return only events;
     * @param inputs inputs
     * @return events
     */
    private String[] getEvents(String[] inputs) {
        Set<String> res = new HashSet<>();
        for (String e: inputs) {
            res.add(extractEvent(e));
        }
        return res.toArray(new String[res.size()]);
    }

    private String extractEvent(String input) {
        return StringUtils.substringBefore(input, "[").trim();
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package egorov.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import structures.Automaton;
import structures.Node;
import structures.Transition;
import egorov.ltl.LtlParseException;
import egorov.ltl.LtlParser;
import egorov.ltl.buchi.BuchiAutomaton;
import egorov.ltl.buchi.BuchiNode;
import egorov.ltl.buchi.translator.JLtl2baTranslator;
import egorov.ltl.grammar.LtlNode;
import egorov.ltl.grammar.LtlUtils;
import egorov.ltl.grammar.predicate.IPredicateFactory;
import egorov.ltl.grammar.predicate.PredicateFactory;
import egorov.statemachine.Action;
import egorov.statemachine.ControlledObject;
import egorov.statemachine.EventProvider;
import egorov.statemachine.SimpleState;
import egorov.statemachine.StateMachine;
import egorov.statemachine.StateTransition;
import egorov.statemachine.StateType;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class VerifierFactory {
    private final AutomataContext context;
    private final IPredicateFactory predicates = new PredicateFactory();
    private final LtlParser parser;
    
    private final List<BuchiAutomaton> preparedFormulas = new ArrayList<>();
    private final List<Set<BuchiNode>> finiteCounterexampleBuchiStates = new ArrayList<>();;
    
    public VerifierFactory(String[] setOfInputs, String[] setOfOutputs) {
        context = new AutomataContext(
        		new ControlledObject(setOfOutputs),
        		new EventProvider(getEvents(setOfInputs))
        );
        parser = new LtlParser(context, predicates);
    }
    
	public void prepareFormulas(List<String> formulas) throws LtlParseException {
    	 JLtl2baTranslator translator = new JLtl2baTranslator();

         for (String f : formulas) {
             LtlNode node = parser.parse(f);
             node = LtlUtils.getInstance().neg(node);
             preparedFormulas.add(translator.translate(node));
         }
         
         finiteCounterexampleBuchiStates.addAll(preparedFormulas.stream()
        		 .map(FiniteCounterexampleNodeSearcher::findCounterexampleBuchiStates)
        		 .collect(Collectors.toList()));
    }
        
    public void configureStateMachine(Automaton automaton) {
    	final ControlledObject co = context.getControlledObject();
    	final EventProvider ep = context.getEventProvider();
    	final StateMachine machine = new StateMachine("A1");

    	final SimpleState[] statesArr = new SimpleState[automaton.statesCount()];
		for (int i = 0; i < automaton.statesCount(); i++) {
			statesArr[i] = new SimpleState("" + i,
                    (automaton.getStartState().getNumber() == i) ? StateType.INITIAL : StateType.NORMAL, 
                    Collections.emptyList());
		}
		for (int i = 0; i < automaton.statesCount(); i++) {
			final Node currentState = automaton.getState(i);
			for (Transition t : currentState.getTransitions()) {
				final StateTransition out = new StateTransition(
                        ep.getEvent(extractEvent(t.getEvent())), statesArr[t.getDst().getNumber()]);

                for (String a: t.getActions().getActions()) {
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

    public List<Counterexample> verify() {
        final List<Counterexample> counterexamples = new ArrayList<>();
        final SimpleVerifier verifier = new SimpleVerifier(context.getStateMachine().getInitialState());
        for (int i = 0; i < preparedFormulas.size(); i++) {
        	final BuchiAutomaton buchi = preparedFormulas.get(i);
        	final Pair<List<IntersectionTransition>, Integer> list = verifier.verify(buchi, predicates,
            		finiteCounterexampleBuchiStates.get(i));
            
            if (!list.getLeft().isEmpty()) {
                final List<String> eventList = list.getLeft().stream()
                    	.map(t -> String.valueOf(t.transition.event))
                    	.collect(Collectors.toList());
                final List<List<String>> actionList = list.getLeft().stream()
                    	.map(t -> t.transition.getActions().stream().map(Action::toString).collect(Collectors.toList()))
                    	.collect(Collectors.toList());
                counterexamples.add(new Counterexample(eventList, actionList, list.getRight()));
            } else {
            	counterexamples.add(new Counterexample(Collections.emptyList(), Collections.emptyList(), 0));
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

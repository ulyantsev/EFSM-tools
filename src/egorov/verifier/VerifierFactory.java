/* 
 * Developed by eVelopers Corporation, 2009
 */
package egorov.verifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import egorov.ltl.grammar.PredicateFactory;
import egorov.statemachine.SimpleState;
import egorov.statemachine.StateMachine;
import egorov.statemachine.StateTransition;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class VerifierFactory {
    private StateMachine machine;
    private final PredicateFactory predicates = new PredicateFactory();
    private final LtlParser parser;
    
    private final List<BuchiAutomaton> preparedFormulas = new ArrayList<>();
    private final List<Set<BuchiNode>> finiteCounterexampleBuchiStates = new ArrayList<>();;
    
    public VerifierFactory() {
        parser = new LtlParser(predicates);
    }
    
	public void prepareFormulas(List<String> formulas) throws LtlParseException {
    	 final JLtl2baTranslator translator = new JLtl2baTranslator();

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
    	final StateMachine machine = new StateMachine();

    	final SimpleState[] statesArr = new SimpleState[automaton.statesCount()];
		for (int i = 0; i < automaton.statesCount(); i++) {
			statesArr[i] = new SimpleState("" + i,
                    automaton.getStartState().getNumber() == i, 
                    Collections.emptyList());
		}
		for (int i = 0; i < automaton.statesCount(); i++) {
			final Node currentState = automaton.getState(i);
			for (Transition t : currentState.getTransitions()) {
				final StateTransition out = new StateTransition(
                        extractEvent(t.getEvent()), statesArr[t.getDst().getNumber()]);
				Arrays.stream(t.getActions().getActions()).forEach(out::addAction);
				statesArr[i].addOutcomingTransition(out);
			}
			machine.addState(statesArr[i]);
		}
		this.machine = machine;
    }

    public List<Counterexample> verify() {
        final List<Counterexample> counterexamples = new ArrayList<>();
        final SimpleVerifier verifier = new SimpleVerifier(machine.getInitialState());
        for (int i = 0; i < preparedFormulas.size(); i++) {
        	final BuchiAutomaton buchi = preparedFormulas.get(i);
        	final Pair<List<IntersectionTransition>, Integer> list = verifier.verify(buchi, predicates,
            		finiteCounterexampleBuchiStates.get(i));
            
            if (!list.getLeft().isEmpty()) {
                final List<String> eventList = list.getLeft().stream()
                    	.map(t -> String.valueOf(t.transition.event))
                    	.collect(Collectors.toList());
                final List<List<String>> actionList = list.getLeft().stream()
                    	.map(t -> t.transition.getActions())
                    	.collect(Collectors.toList());
                counterexamples.add(new Counterexample(eventList, actionList, list.getRight()));
            } else {
            	counterexamples.add(new Counterexample(Collections.emptyList(), Collections.emptyList(), 0));
            }
        }
        return counterexamples;
    }

    private String extractEvent(String input) {
        return StringUtils.substringBefore(input, "[").trim();
    }
}

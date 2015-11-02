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
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NondetMooreAutomaton;
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
    
    private final List<BuchiAutomaton> preparedFormulae = new ArrayList<>();
    private final List<Set<BuchiNode>> finiteCounterexampleBuchiStates = new ArrayList<>();
    private final List<LtlNode> preparedLtlNodes = new ArrayList<>();
    
    public VerifierFactory() {
        parser = new LtlParser(predicates);
    }
    
    public List<LtlNode> preparedLtlNodes() {
    	return Collections.unmodifiableList(preparedLtlNodes);
    }
    
	public void prepareFormulas(List<String> formulas) throws LtlParseException {
    	 final JLtl2baTranslator translator = new JLtl2baTranslator();

         for (String f : formulas) {
             LtlNode node = parser.parse(f);
             preparedLtlNodes.add(node);
             node = LtlUtils.getInstance().neg(node);
             preparedFormulae.add(translator.translate(node));
         }
         
         finiteCounterexampleBuchiStates.addAll(preparedFormulae.stream()
        		 .map(FiniteCounterexampleNodeSearcher::findCounterexampleBuchiStates)
        		 .collect(Collectors.toList()));
    }
        
    public void configureStateMachine(Automaton automaton) {
    	final StateMachine machine = new StateMachine();

    	final SimpleState[] statesArr = new SimpleState[automaton.statesCount()];
		for (int i = 0; i < automaton.statesCount(); i++) {
			statesArr[i] = new SimpleState(String.valueOf(i),
                    automaton.getStartState().getNumber() == i);
		}
		for (int i = 0; i < automaton.statesCount(); i++) {
			final Node currentState = automaton.getState(i);
			for (Transition t : currentState.getTransitions()) {
				final StateTransition out = new StateTransition(
                        extractEvent(t.getEvent()), statesArr[t.getDst().getNumber()]);
				Arrays.stream(t.getActions().getActions()).forEach(out::addAction);
				statesArr[i].addOutgoingTransition(out);
			}
			machine.addState(statesArr[i]);
		}
		this.machine = machine;
    }

    public void configureNondetMooreMachine(NondetMooreAutomaton automaton) {
    	final StateMachine machine = new StateMachine();

    	final SimpleState[] statesArr = new SimpleState[automaton.statesCount() + 1];
		for (int i = 0; i < automaton.statesCount(); i++) {
			statesArr[i] = new SimpleState(String.valueOf(i), false);
		}
		final SimpleState nondetInit = new SimpleState("nondet_init", true);
		for (int i = 0; i < automaton.statesCount(); i++) {
			if (automaton.isStartState(i)) {
				final StateTransition out = new StateTransition("", statesArr[i]);
				Arrays.stream(automaton.state(i).actions().getActions()).forEach(out::addAction);
				nondetInit.addOutgoingTransition(out);
			}
		}
		machine.addState(nondetInit);
		for (int i = 0; i < automaton.statesCount(); i++) {
			final MooreNode currentState = automaton.state(i);
			for (MooreTransition t : currentState.transitions()) {
				final StateTransition out = new StateTransition(
                        extractEvent(t.event()), statesArr[t.dst().number()]);
				Arrays.stream(t.dst().actions().getActions()).forEach(out::addAction);
				statesArr[i].addOutgoingTransition(out);
			}
			machine.addState(statesArr[i]);
		}
		this.machine = machine;
    }
    
    public List<Counterexample> verify() {
        final List<Counterexample> counterexamples = new ArrayList<>();
        final SimpleVerifier verifier = new SimpleVerifier(machine.initialState());
        for (int i = 0; i < preparedFormulae.size(); i++) {
        	final BuchiAutomaton buchi = preparedFormulae.get(i);
        	final Pair<List<IntersectionTransition>, Integer> list = verifier.verify(buchi, predicates,
            		finiteCounterexampleBuchiStates.get(i));
            
            if (!list.getLeft().isEmpty()) {
                final List<String> eventList = list.getLeft().stream()
                    	.map(t -> t.transition.event)
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

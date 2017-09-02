/* 
 * Developed by eVelopers Corporation, 2009
 */
package verification.verifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;
import structures.moore.MooreNode;
import structures.moore.MooreTransition;
import structures.moore.NondetMooreAutomaton;
import verification.ltl.GrammarConverter;
import verification.ltl.LtlParseException;
import verification.ltl.LtlParser;
import verification.ltl.buchi.BuchiAutomaton;
import verification.ltl.buchi.BuchiNode;
import verification.ltl.buchi.translator.JLtl2baTranslator;
import verification.ltl.grammar.PredicateFactory;
import verification.ltl.grammar.UnaryOperator;
import verification.ltl.grammar.UnaryOperatorType;
import verification.statemachine.SimpleState;
import verification.statemachine.StateMachine;
import verification.statemachine.StateTransition;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class VerifierFactory {
    private StateMachine machine;
    private final PredicateFactory predicates = new PredicateFactory();
    
    private final List<BuchiAutomaton> preparedFormulae = new ArrayList<>();
    private final List<Set<BuchiNode>> finiteCounterexampleBuchiStates = new ArrayList<>();

    private final boolean verifyFromAllStates;
    
    VerifierFactory(boolean verifyFromAllStates) {
        this.verifyFromAllStates = verifyFromAllStates;
    }

    void prepareFormulas(List<String> formulas) throws LtlParseException {
         final JLtl2baTranslator translator = new JLtl2baTranslator();

        preparedFormulae.addAll(LtlParser.parse(formulas, new GrammarConverter(predicates)).stream()
                .map(node -> translator.translate(new UnaryOperator(UnaryOperatorType.NEG, node)))
                .collect(Collectors.toList()));
         
         finiteCounterexampleBuchiStates.addAll(preparedFormulae.stream()
                 .map(FiniteCounterexampleNodeSearcher::findCounterexampleBuchiStates)
                 .collect(Collectors.toList()));
    }
        
    void configureDetMealyMachine(MealyAutomaton automaton) {
        final StateMachine machine = new StateMachine();

        final SimpleState nondetInit = verifyFromAllStates ? new SimpleState("nondet_init", true) : null;
        if (verifyFromAllStates) {
            machine.addState(nondetInit);
        }
        
        final SimpleState[] statesArr = new SimpleState[automaton.stateCount()];
        for (int i = 0; i < automaton.stateCount(); i++) {
            statesArr[i] = new SimpleState(String.valueOf(i),
                    automaton.startState().number() == i && !verifyFromAllStates);
        }
        for (int i = 0; i < automaton.stateCount(); i++) {
            final MealyNode currentState = automaton.state(i);
            for (MealyTransition t : currentState.transitions()) {
                final StateTransition out = new StateTransition(extractEvent(t.event()), statesArr[t.dst().number()]);
                Arrays.stream(t.actions().getActions()).forEach(out::addAction);
                statesArr[i].addOutgoingTransition(out);
                if (verifyFromAllStates) {
                    nondetInit.addOutgoingTransition(out);
                }
            }
            machine.addState(statesArr[i]);
        }
        this.machine = machine;
    }

    void configureNondetMooreMachine(NondetMooreAutomaton automaton) {
        final StateMachine machine = new StateMachine();

        final SimpleState[] statesArr = new SimpleState[automaton.stateCount() + 1];
        for (int i = 0; i < automaton.stateCount(); i++) {
            statesArr[i] = new SimpleState(String.valueOf(i), false);
        }
        final SimpleState nondetInit = new SimpleState("nondet_init", true);
        
        for (int i = 0; i < automaton.stateCount(); i++) {
            if (automaton.isInitialState(i)) {
                final StateTransition out = new StateTransition("", statesArr[i]);
                Arrays.stream(automaton.state(i).actions().getActions()).forEach(out::addAction);
                if (!verifyFromAllStates) {
                    nondetInit.addOutgoingTransition(out);
                }
            }
        }
        machine.addState(nondetInit);
        for (int i = 0; i < automaton.stateCount(); i++) {
            final MooreNode currentState = automaton.state(i);
            for (MooreTransition t : currentState.transitions()) {
                final StateTransition out = new StateTransition(extractEvent(t.event()), statesArr[t.dst().number()]);
                Arrays.stream(t.dst().actions().getActions()).forEach(out::addAction);
                statesArr[i].addOutgoingTransition(out);
                if (verifyFromAllStates) {
                    nondetInit.addOutgoingTransition(out);
                }
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
                final List<String> eventList = list.getLeft().stream().map(t -> t.transition.event)
                        .collect(Collectors.toList());
                final List<List<String>> actionList = list.getLeft().stream().map(t -> t.transition.getActions())
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

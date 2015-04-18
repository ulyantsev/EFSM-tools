/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.LtlParser;
import qbf.egorov.ltl.buchi.BuchiAutomata;
import qbf.egorov.ltl.buchi.translator.JLtl2baTranslator;
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
    
    public void prepareFormulas(List<String> formulas) throws LtlParseException {
    	 JLtl2baTranslator translator = new JLtl2baTranslator();

         int j = 0;
         preparedFormulas = new BuchiAutomata[formulas.size()];
         for (String f : formulas) {
             LtlNode node = parser.parse(f);
             node = LtlUtils.getInstance().neg(node);
             preparedFormulas[j++] = translator.translate(node);
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

    // returns counterexamples
    public List<List<String>> verify() {
        List<List<String>> counterexamples = new ArrayList<>();
        SimpleVerifier verifier = new SimpleVerifier(context.getStateMachine(null).getInitialState());

        for (BuchiAutomata buchi : preparedFormulas) {
            List<IntersectionTransition> list = verifier.verify(buchi, predicates);
            
            if (!list.isEmpty()) {
                List<String> eventList = list.stream().skip(1)
                    	.map(t -> String.valueOf(t.transition.event))
                    	.collect(Collectors.toList());
                counterexamples.add(eventList);
            } else {
            	counterexamples.add(Collections.emptyList());
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

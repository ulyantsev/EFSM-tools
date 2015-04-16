/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.LtlParser;
import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.ITranslator;
import qbf.egorov.ltl.buchi.translator.JLtl2baTranslator;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.LtlUtils;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.ltl.grammar.predicate.PredicateFactory;
import qbf.egorov.statemachine.IAction;
import qbf.egorov.statemachine.IControlledObject;
import qbf.egorov.statemachine.IEventProvider;
import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.StateType;
import qbf.egorov.statemachine.impl.ControlledObjectStub;
import qbf.egorov.statemachine.impl.EventProviderStub;
import qbf.egorov.statemachine.impl.SimpleState;
import qbf.egorov.statemachine.impl.StateMachine;
import qbf.egorov.transducer.FST;
import qbf.egorov.transducer.Transition;
import qbf.egorov.verifier.IDfsListener;
import qbf.egorov.verifier.automata.IntersectionTransition;
import qbf.egorov.verifier.impl.SimpleVerifier;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class VerifierFactory {
    private ModifiableAutomataContext context;
    private IPredicateFactory<IState> predicates = new PredicateFactory<>();
    private LtlParser parser;
    private IBuchiAutomata[][] preparedFormulas;

    private IDfsListener marker = new TransitionMarker();
    private TransitionCounter counter = new TransitionCounter();

    private FST fst;

    public VerifierFactory(String[] setOfInputs, String[] setOfOutputs) {
        IControlledObject co = new ControlledObjectStub("co", setOfOutputs);
        String[] filteredInputs = getEvents(setOfInputs);
		IEventProvider ep = new EventProviderStub("ep", filteredInputs);

        context = new ModifiableAutomataContext(co, ep);
        parser = new LtlParser(context, predicates);
    }
    
    public void prepareFormulas(List<String> formulas) throws LtlParseException {
    	 ITranslator translator = new JLtl2baTranslator();

         preparedFormulas = new IBuchiAutomata[formulas.size()][];

         int j = 0;
         preparedFormulas = new IBuchiAutomata[formulas.size()][1];
         for (String f : formulas) {
             LtlNode node = parser.parse(f);
             node = LtlUtils.getInstance().neg(node);
             preparedFormulas[j++][0] = translator.translate(node);
         }
    }

    public void configureStateMachine(FST fst) {
        Transition[][] states = fst.getStates();
        this.fst = fst;

        IControlledObject co = context.getControlledObject(null);
		IEventProvider ep = context.getEventProvider(null);

        StateMachine<IState> machine = new StateMachine<>("A1");

		SimpleState[] statesArr = new SimpleState[states.length];
		for (int i = 0; i < states.length; i++) {
			statesArr[i] = new SimpleState("" + i,
                    (fst.getInitialState() == i) ? StateType.INITIAL : StateType.NORMAL, 
                    Collections.<IAction>emptyList());
		}
		for (int i = 0; i < states.length; i++) {
			Transition[] currentState = states[i];
			for (Transition t : currentState) {
                //mark as not verified yet
                t.setVerified(false);
                t.setUsedByVerifier(false);

                AutomataTransition out = new AutomataTransition(
                        ep.getEvent(extractEvent(t.getInput())), null, statesArr[t.getNewState()]);
                out.setAlgTransition(t);

                for (String a: t.getOutput()) {
                    IAction action = co.getAction(a);
                    if (action != null) {
                        out.addAction(co.getAction(a));
                    }
                }
				statesArr[i].addOutcomingTransition(out);
			}
			machine.addState(statesArr[i]);
		}
        machine.addControlledObject(co.getName(), co);
		machine.addEventProvider(ep);

        context.setStateMachine(machine);
    }

    // returns (numbers of verified transitions, counterexamples)
    public Pair<int[], List<List<String>>> verify() {
        int[] res = new int[preparedFormulas.length];
        List<List<String>> counterexamples = new ArrayList<>();
        SimpleVerifier<IState> verifier = new SimpleVerifier<>(context.getStateMachine(null).getInitialState());
        int usedTransitions = fst.getUsedTransitionsCount();

        for (int i = 0; i < preparedFormulas.length; i++) {
            int marked = 0;

            for (IBuchiAutomata buchi : preparedFormulas[i]) {
                counter.resetCounter();
                List<IntersectionTransition<?>> list = verifier.verify(buchi, predicates, marker, counter);
                if (list.isEmpty()) {
                	counterexamples.add(Collections.emptyList());
                } else {
                	List<String> eventList = list.stream().skip(1)
                        	.map(t -> String.valueOf(t.getTransition().getEvent())).collect(Collectors.toList());
                    counterexamples.add(eventList.subList(0, eventList.size() - 1 + 1));
                }
                
                if (list != null && !list.isEmpty()) {

                    ListIterator<IntersectionTransition<?>> iter = list.listIterator(list.size());

                    int failTransitions = buchi.size() - 1;

                    for (int j = 0; iter.hasPrevious() && (j < failTransitions);) {
                        IntersectionTransition<?> t = iter.previous();
                        if ((t.getTransition() != null)
                                && (t.getTransition().getClass() == AutomataTransition.class)) {
                        	AutomataTransition trans = (AutomataTransition) t.getTransition();
                            if (trans.getAlgTransition() != null) {
                                trans.getAlgTransition().setUsedByVerifier(true);
                                j++;
                            }
                        }
                    }

                    marked += counter.countVerified() / 2;
                } else {
                    marked += usedTransitions;
                }
            }
            res[i] = marked;
        }
        return Pair.of(res, counterexamples);
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

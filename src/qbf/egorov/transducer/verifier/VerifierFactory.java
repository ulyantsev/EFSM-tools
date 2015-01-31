/* 
 * Developed by eVelopers Corporation, 2009
 */
package qbf.egorov.transducer.verifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.ITranslator;
import qbf.egorov.ltl.buchi.translator.JLtl2baTranslator;
import qbf.egorov.ltl.converter.ILtlParser;
import qbf.egorov.ltl.converter.LtlParser;
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
import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.algorithm.Transition;
import qbf.egorov.verifier.IDfsListener;
import qbf.egorov.verifier.IVerifier;
import qbf.egorov.verifier.automata.IIntersectionTransition;
import qbf.egorov.verifier.impl.SimpleVerifier;

/**
 * @author kegorov
 *         Date: Jun 18, 2009
 */
public class VerifierFactory implements IVerifierFactory {
//    private static Random RANDOM = new Random();
    private ModifiableAutomataContext context;
    private IPredicateFactory<IState> predicates = new PredicateFactory<IState>();
    private ILtlParser parser;
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

         preparedFormulas = new IBuchiAutomata[1][];

         IBuchiAutomata[] group = new IBuchiAutomata[formulas.size()];
         int j = 0;
         for (String f : formulas) {
             LtlNode node = parser.parse(f);
             node = LtlUtils.getInstance().neg(node);
             group[j++] = translator.translate(node);
         }
         preparedFormulas[0] = group;
//         System.out.println("Buchi automaton hash = " + Digest.RSHash(group[0].toString()));
    }

    public void configureStateMachine(FST fst) {
        Transition[][] states = fst.getStates();
        this.fst = fst;

        IControlledObject co = context.getControlledObject(null);
		IEventProvider ep = context.getEventProvider(null);

        StateMachine<IState> machine = new StateMachine<IState>("A1");

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

    public int[] verify() {
        int[] res = new int[preparedFormulas.length];
        IVerifier<IState> verifier = new SimpleVerifier<IState>(context.getStateMachine(null).getInitialState());
//        List<IIntersectionTransition> longestList = Collections.emptyList();
        int usedTransitions = fst.getUsedTransitionsCount();

        for (int i = 0; i < preparedFormulas.length; i++) {
            int marked = 0;

            for (IBuchiAutomata buchi : preparedFormulas[i]) {
                counter.resetCounter();
                
                List<IIntersectionTransition> list;
//                if (RANDOM.nextInt(length) < barrier) {
                    list = verifier.verify(buchi, predicates, marker, counter);
//                } else {
//                    list = verifier.verify(buchi, predicates, counter);
//                }
                if ((list != null) && (!list.isEmpty())) {
                    /*if (longestList.size() < list.size()) {
                        longestList = list;
                    }*/
                    ListIterator<IIntersectionTransition> iter = list.listIterator(list.size());

//                    int failTransitions = (buchi.size() == 2) ? 1 : 2; //1 -- invariant, 2 -- pre(post) condition
                    int failTransitions = buchi.size() - 1;

                    for (int j = 0; iter.hasPrevious() && (j < failTransitions);) {
                        IIntersectionTransition t = iter.previous();
                        if ((t.getTransition() != null)
                                && (t.getTransition().getClass() == AutomataTransition.class)) {
                            AutomataTransition trans = (AutomataTransition) t.getTransition();
                            if (trans.getAlgTransition() != null) {
                                trans.getAlgTransition().setUsedByVerifier(true);
                                j++;
                            }
                        }
                    }
                    /*for (IIntersectionTransition t : list) {
                        if ((t.getTransition() != null)
                                && (t.getTransition().getClass() == AutomataTransition.class)) {
                            AutomataTransition trans = (AutomataTransition) t.getTransition();
                            if (trans.getAlgTransition() != null) {
                                trans.getAlgTransition().setUsedByVerifier(true);
                            }
                        }
                    }*/
                    marked += counter.countVerified() / 2;
//                    System.out.println("verified = " + counter.countVerified());
                } else {
                    marked += usedTransitions;
                }
            }
            res[i] = marked;
        }

        /*for (IIntersectionTransition t : longestList) {
            AutomataTransition trans = (AutomataTransition) t.getTransition();
            if ((trans != null) && (trans.getAlgTransition() != null)) {
                trans.getAlgTransition().setUsedByVerifier(true);
            }
        }*/

        return res;
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

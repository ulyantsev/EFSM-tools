/* 
 * Developed by eVelopers Corporation, 2012
 */
package ru.ifmo.ctddev.genetic.transducer;

import ru.ifmo.automata.statemachine.*;
import ru.ifmo.automata.statemachine.impl.AutomataContext;
import ru.ifmo.automata.statemachine.io.StateMachineReader;
import ru.ifmo.ctddev.genetic.transducer.io.ITestsReader;
import ru.ifmo.ctddev.genetic.transducer.io.OneGroupTestsReader;
import ru.ifmo.ctddev.genetic.transducer.scenario.Path;
import ru.ifmo.ctddev.genetic.transducer.scenario.TestGroup;
import ru.ifmo.ltl.LtlParseException;
import ru.ifmo.ltl.buchi.translator.JLtl2baTranslator;
import ru.ifmo.ltl.converter.ILtlParser;
import ru.ifmo.ltl.converter.LtlParser;
import ru.ifmo.ltl.grammar.predicate.IPredicateFactory;
import ru.ifmo.ltl.grammar.predicate.PredicateFactory;
import ru.ifmo.verifier.IVerifier;
import ru.ifmo.verifier.automata.IIntersectionTransition;
import ru.ifmo.verifier.impl.SimpleVerifier;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author kegorov
 *         Date: 6/9/12
 */
public class BankTest {

    static IPredicateFactory<IState> predicates;
    static IVerifier<IState> verifier;
    
    public static void main(String[] args) throws Exception {
        predicates = createPredicateUtils();
        IAutomataContext context = new AutomataContext(new StateMachineReader(
                new File("../AutomataVerificator/test/resources/bankomat.xml")));
        ILtlParser parser = new LtlParser(context, predicates);
        verifier = createVerifier(context.getStateMachine("A1"), parser);

        ITestsReader reader = new OneGroupTestsReader(new File("bankomat.xml"), false);
        
        for (TestGroup group : reader.getGroups()) {
            for (String ltl : group.getFormulas()) {
                test(ltl);
            }
            for (Path np : group.getNegativeTests()) {
                IState s = context.getStateMachine("A1").getInitialState();
                for (int i = 0; i < np.getInput().length; i++) {
                    String e = np.getInput()[i];
                    boolean notFound = true;
                    for (IStateTransition t : s.getOutcomingTransitions()) {
                        if (t.getEvent() == null) {
                            continue;
                        }
                        if (e.equals(t.getEvent().getName())) {
                            s = t.getTarget();
                            notFound = false;
                            break;
                        }
                    }
                    if ( (notFound && (i != (np.getInput().length - 1))) || (!notFound && (i == (np.getInput().length - 1))) ) {
                        throw new RuntimeException("Wrong negative Test: " + Arrays.toString(np.getInput()));
                    }
                }
            }
            
            for (Path test : group.getTests()) {
                IState s = context.getStateMachine("A1").getInitialState();
                for (int i = 0; i < test.getInput().length; i++) {
                    String e = test.getInput()[i];
                    String out = test.getFixedOutput()[i];
                    boolean notFound = true;

                    for (IStateTransition t : s.getOutcomingTransitions()) {
                        if (t.getEvent() == null) {
                            continue;
                        }
                        if (e.equals(t.getEvent().getName())) {
                            s = t.getTarget();
                            
                            StringBuilder buf = new StringBuilder();
                            for (IAction a : t.getActions()) {
                                if (buf.length() != 0) {
                                    buf.append(",");
                                }
                                buf.append(a.getName());
                            }

                            if (!buf.toString().equals(out)) {
                                throw new RuntimeException("Unexpected output " + e + "/" + buf.toString());
                            }
                            notFound = false;
                            break;
                        }
                    }
                    if (notFound) {
                        throw new RuntimeException("Wrong test: " + Arrays.toString(test.getInput()));
                    }
                }
            }
        }
    }

    protected static void test(String formula) throws LtlParseException {
        List<IIntersectionTransition> stack = verifier.verify(formula, predicates);

        if (!stack.isEmpty()) {
            throw new RuntimeException("Formula is wrong: " + formula);
        }
    }

    protected static IVerifier<IState> createVerifier(IStateMachine<? extends IState> stateMachine, ILtlParser parser) {
        return new SimpleVerifier<IState>(stateMachine.getInitialState(), parser, new JLtl2baTranslator());
    }

    protected static IPredicateFactory<IState> createPredicateUtils() {
        return new PredicateFactory<IState>();
    }
}

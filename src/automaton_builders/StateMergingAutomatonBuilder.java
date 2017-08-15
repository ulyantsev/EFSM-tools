package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import scenario.StringScenario;
import structures.mealy.APTA;
import structures.mealy.MealyAutomaton;
import verification.verifier.Counterexample;
import verification.verifier.Verifier;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StateMergingAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    public static APTA getAPTA(List<List<String>> posSc, Iterable<List<String>> negSc) {
        final APTA a = new APTA();
        for (List<String> sc : posSc) {
            a.addScenario(sc, true);
        }
        for (List<String> sc : negSc) {
            a.addScenario(sc, false);
        }
        a.resetColors();
        return a;
    }
    
    public static Optional<MealyAutomaton> build(Logger logger, Verifier verifier, List<String> scenarioFilePaths,
                                                 String negscFilePath) throws IOException, ParseException {
        final List<List<String>> posSc = new ArrayList<>();
        for (String filePath : scenarioFilePaths) {
            for (StringScenario sc : StringScenario.loadScenarios(filePath, false)) {
                List<String> l = new ArrayList<>();
                for (int i = 0; i < sc.size(); i++) {
                    l.add(sc.getEvents(i).get(0));
                }
                posSc.add(l);
            }
        }
        
        final Set<List<String>> negSc = new LinkedHashSet<>();
        if (negscFilePath != null) {
            for (StringScenario sc : StringScenario.loadScenarios(negscFilePath, false)) {
                List<String> l = new ArrayList<>();
                for (int i = 0; i < sc.size(); i++) {
                    l.add(sc.getEvents(i).get(0));
                }
                negSc.add(l);
            }
        }
        
        APTA a = getAPTA(posSc, negSc);
        
        int iterations = 1;
        while (true) {
            a.updateColors();
            final boolean succ = a.bestMerge();
            if (succ) {
                final List<Counterexample> counterexamplesAll
                    = verifier.verifyWithCounterexamplesWithNoDeadEndRemoval(a.toAutomaton());
                if (counterexamplesAll.stream().anyMatch(ce -> ce.loopLength > 0)) {
                    logger.severe("Looping counterexample! There might be non-safety LTL properties"
                            + " in the input specification.");
                }
                final List<Counterexample> counterexamples = counterexamplesAll.stream()
                        .filter(ce -> ce.loopLength == 0)
                        .collect(Collectors.toList());
                if (!counterexamples.stream().allMatch(Counterexample::isEmpty)) {
                    System.out.println();
                    int added = 0;
                    for (Counterexample ce : counterexamples) {
                        if (ce.isEmpty()) {
                            continue;
                        }
                        added++;
                        negSc.add(ce.events());
                        logger.info("ADDING COUNTEREXAMPLE: " + ce.events());
                    }
                    logger.info("(ADDED COUNTEREXAMPLES: " + added + ")");
                    a = getAPTA(posSc, negSc);
                    iterations++;
                } else {
                    System.out.print(".");
                }
            } else {
                break;
            }
        }

        logger.info("ITERATIONS: " + iterations);
        return Optional.of(a.toAutomaton());
    }
}
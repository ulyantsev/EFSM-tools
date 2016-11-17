package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import org.apache.commons.lang3.ArrayUtils;
import scenario.StringActions;
import scenario.StringScenario;
import structures.mealy.APTA;
import structures.moore.NondetMooreAutomaton;
import verification.verifier.Counterexample;
import verification.verifier.Verifier;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StateMergingNondetAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
    public static Optional<NondetMooreAutomaton> build(Logger logger, List<String> events, List<String> actions,
            List<String> arguments, List<String> strFormulae) throws FileNotFoundException, ParseException {
        final List<String> eventsWithDummy = new ArrayList<>(events);
        eventsWithDummy.add("");
        final List<String> actionCombinations = new ArrayList<>();

        final int maxAction = 1 << actions.size();
        for (int i = 0; i < maxAction; i++) {
            String s = Integer.toBinaryString(i);
            while (s.length() < actions.size()) {
                s = "0" + s;
            }
            actionCombinations.add(s);
        }

        final List<List<String>> newSc = new ArrayList<>();
        for (String filePath : arguments) {
            for (StringScenario sc : StringScenario.loadScenarios(filePath, false)) {
                final List<String> scOldEvents = new ArrayList<>();
                final List<StringActions> scOldActions = new ArrayList<>();
                for (int i = 0; i < sc.size(); i++) {
                    scOldEvents.add(sc.getEvents(i).get(0));
                    scOldActions.add(sc.getActions(i));
                }
                final List<String> scEvents = transformPath(scOldEvents, scOldActions, actions);
                newSc.add(scEvents);
            }
        }

        /*final List<String> formulaeUpdated = new ArrayList<>();
        for (String f : strFormulae) {
            String newF = f;
            for (String e : events) {
                final String before = "event(" + e + ")";
                final String after = "(" + String.join(" || ", actionCombinations.stream().map(a -> "event(" + e + a + ")")
                        .collect(Collectors.toList())) + ")";
                newF = newF.replace(before, after);
            }
            for (int actionNum = 0; actionNum < actions.size(); actionNum++) {
                final String a = actions.get(actionNum);
                final String before = "action(" + a + ")";
                final List<String> chunks = new ArrayList<>();
                for (String e : eventsWithDummy) {
                    for (String ac : actionCombinations) {
                        if (ac.charAt(actionNum) == '1') {
                            chunks.add("event(" + e + ac + ")");
                        }
                    }
                }
                final String after = "(" + String.join(" || ", chunks) + ")";
                newF = newF.replace(before, after);
            }
            formulaeUpdated.add(newF);
        }*/

        final Verifier v = new Verifier(logger, strFormulae, events, actions);
        return build2(logger, v, newSc, actions);
    }

    public static List<String> transformPath(List<String> eventList, List<StringActions> actionList, List<String> actions) {
        final List<String> newEvents = new ArrayList<>();
        for (int i = 0; i < eventList.size(); i++) {
            final char[] scAction = new char[actions.size()];
            for (int j = 0; j < actions.size(); j++) {
                scAction[j] = ArrayUtils.contains(actionList.get(i).getActions(), actions.get(j)) ? '1' : '0';
            }
            newEvents.add(eventList.get(i) + Arrays.toString(scAction)
                    .replaceAll(",", "").replaceAll(" ", "")
                    .replace("[", "").replace("]", ""));
        }
        return newEvents;
    }

    public static Optional<NondetMooreAutomaton> build2(Logger logger, Verifier verifier, List<List<String>> possc,
                                                  List<String> actions) throws FileNotFoundException, ParseException {
        final List<List<String>> negsc = new ArrayList<>();
        APTA a = StateMergingAutomatonBuilder.getAPTA(possc, negsc);

        int iterations = 1;
        while (true) {
            a.updateColors();
            final Optional<APTA> merge = a.bestMerge();
            if (merge.isPresent()) {
                final APTA newA = merge.get();

                final NondetMooreAutomaton moore = newA.toNondetMooreAutomaton(actions);
                final List<Counterexample> counterexamples = verifier.verifyNondetMoore(moore).stream()
                        .filter(ce -> !ce.isEmpty())
                        .distinct()
                        .collect(Collectors.toList());

                if (!counterexamples.isEmpty()) {
                    System.out.println();
                    int added = 0;
                    for (Counterexample ce : counterexamples) {
                        if (ce.loopLength > 0) {
                            throw new RuntimeException("Looping counterexample!");
                        }
                        added++;

                        final List<String> transformedEvents = transformPath(ce.events(), ce.actions().stream().map(
                                list -> new StringActions(String.join(",", list)))
                                .collect(Collectors.toList()), actions);

                        negsc.add(transformedEvents);
                        logger.info("ADDING COUNTEREXAMPLE: " + transformedEvents);
                        System.out.println(ce.toString());
                    }
                    logger.info("(ADDED COUNTEREXAMPLES: " + added + ")");
                    a = StateMergingAutomatonBuilder.getAPTA(possc, negsc);
                    iterations++;
                } else {
                    a = newA;
                    System.out.print(".");
                }
            } else {
                break;
            }
        }

        logger.info("ITERATIONS: " + iterations);
        return Optional.of(a.toNondetMooreAutomaton(actions));
    }
}
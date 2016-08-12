package automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import scenario.StringScenario;
import structures.mealy.APTA;
import structures.mealy.MealyAutomaton;
import verification.verifier.Counterexample;
import verification.verifier.Verifier;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

public class StateMergingAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static APTA getAPTA(List<List<String>> possc, Set<List<String>> negsc) {
		final APTA a = new APTA();
		for (List<String> sc : possc) {
			a.addScenario(sc, true);
		}
		for (List<String> sc : negsc) {
			a.addScenario(sc, false);
		}
		a.resetColors();
		return a;
	}
	
	public static Optional<MealyAutomaton> build(Logger logger, Verifier verifier, List<String> scenarioFilePaths,
                                                 String negscFilePath) throws FileNotFoundException, ParseException {
		final List<List<String>> possc = new ArrayList<>();
		for (String filePath : scenarioFilePaths) {
			for (StringScenario sc : StringScenario.loadScenarios(filePath, false)) {
				List<String> l = new ArrayList<>();
				for (int i = 0; i < sc.size(); i++) {
					l.add(sc.getEvents(i).get(0));
				}
				possc.add(l);
			}
		}
		
		final Set<List<String>> negsc = new LinkedHashSet<>();
		if (negscFilePath != null) {
			for (StringScenario sc : StringScenario.loadScenarios(negscFilePath, false)) {
				List<String> l = new ArrayList<>();
				for (int i = 0; i < sc.size(); i++) {
					l.add(sc.getEvents(i).get(0));
				}
				negsc.add(l);
			}
		}
		
		APTA a = getAPTA(possc, negsc);
		
		int iterations = 1;
		while (true) {
			a.updateColors();
			final Optional<APTA> merge = a.bestMerge();
			if (merge.isPresent()) {
				final APTA newA = merge.get();
				final List<Counterexample> counterexamples
					= verifier.verifyWithCounterexamplesWithNoDeadEndRemoval(newA.toAutomaton());
				if (!counterexamples.stream().allMatch(Counterexample::isEmpty)) {
					int added = 0;
					for (Counterexample ce : counterexamples) {
						if (ce.isEmpty()) {
							continue;
						}
						if (ce.loopLength > 0) {
							throw new RuntimeException("Looping counterexample!");
						}
						added++;
						negsc.add(ce.events());
						logger.info("ADDING COUNTEREXAMPLE: " + ce.events());
					}
					logger.info("(ADDED COUNTEREXAMPLES: " + added + ")");
					a = getAPTA(possc, negsc);
					iterations++;
				} else {
					a = newA;
				}
			} else {
				break;
			}
		}

		logger.info("ITERATIONS: " + iterations);
		return Optional.of(a.toAutomaton());
	}
}
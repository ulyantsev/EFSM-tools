package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import qbf.egorov.verifier.Counterexample;
import qbf.reduction.Verifier;
import structures.APTA;
import structures.Automaton;

public class StateMergingAutomatonBuilder extends ScenarioAndLtlAutomatonBuilder {
	private static APTA getAPTA(List<List<String>> possc, Set<List<String>> negsc) {
		APTA a = new APTA();
		for (List<String> sc : possc) {
			a.addScenario(sc, true);
		}
		for (List<String> sc : negsc) {
			a.addScenario(sc, false);
		}
		a.resetColors();
		return a;
	}
	
	private static void print(APTA a, String comment) {
		System.out.println(comment + " " + a);
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}
	}
	
	public static Optional<Automaton> build(Logger logger, List<String> events, Verifier verifier,
			List<List<String>> possc, Set<List<String>> negsc) {		
		APTA a = getAPTA(possc, negsc);
		
		while (true) {
			//print(a, "RESTART");
			a.updateColors();
			//print(a, "NEW COLORS");
			final Optional<APTA> merge = a.bestMerge();
			if (merge.isPresent()) {
				final APTA newA = merge.get();
				//print(newA, "AFTER MERGE");
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
						System.out.println("ADDING COUNTEREXAMPLE: " + ce.events());
					}
					System.out.println("(ADDED COUNTEREXAMPLES: " + added + ")");
					 a = getAPTA(possc, negsc);
				} else {
					a = newA;
				}
			} else {
				break;
			}
		}

		return Optional.of(a.toAutomaton());
	}
}
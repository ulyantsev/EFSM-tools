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
	public static Optional<Automaton> build(Logger logger, List<String> events, Verifier verifier,
			List<List<String>> possc, Set<List<String>> negsc) {		
		APTA a = new APTA();
		for (List<String> sc : possc) {
			a.addScenario(sc, true);
		}
		for (List<String> sc : negsc) {
			a.addScenario(sc, false);
		}
		
		l: while (true) {
			//System.out.println(a);
			/*try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}*/
			
			List<APTA> merges = a.possibleMerge();
			for (APTA newA : merges) {
				final List<Counterexample> counterexamples
					= verifier.verifyWithCounterexamplesWithNoDeadEndRemoval(newA.toAutomaton());
				if (!counterexamples.stream().allMatch(Counterexample::isEmpty)) {
					int added = 0;
					for (Counterexample ce : counterexamples) {
						if (ce.isEmpty()) {
							continue;
						}
						added++;
						if (ce.loopLength > 0) {
							throw new RuntimeException("Looping counterexample!");
						}
						a.addScenario(ce.events(), false);
						System.out.println("ADDING COUNTEREXAMPLE: " + ce.events());
					}
					System.out.println("(ADDED COUNTEREXAMPLES: " + added + ")");
				} else {
					a = newA;
				}
				continue l;
			}
			break;
		}

		return Optional.of(a.toAutomaton());
	}
}
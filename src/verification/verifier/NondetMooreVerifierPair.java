package verification.verifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

import structures.plant.NondetMooreAutomaton;

public class NondetMooreVerifierPair {
	private final Verifier ordinaryVerifier;
	private final Verifier globalVerifier;
	
	public NondetMooreVerifierPair(Logger logger, List<String> strFormulae, List<String> events, List<String> actions) {
		final List<String> ordinaryLTL = new ArrayList<>();
		final List<String> globalLTL = new ArrayList<>();
		for (String formula : strFormulae) {
			if (formula.startsWith("G(") && formula.endsWith(")")) {
				final String cropped = formula.substring(2, formula.length() - 1);
				ordinaryLTL.add(cropped);
				globalLTL.add(formula);
			} else {
				ordinaryLTL.add(formula);
			}
		}
		ordinaryVerifier = new Verifier(logger, ordinaryLTL, events, actions, false);
		globalVerifier = new Verifier(logger, globalLTL, events, actions, true);
	}
	
	public Pair<List<Counterexample>, List<Counterexample>> verifyNondetMoore(NondetMooreAutomaton automaton) {
		final List<Counterexample> ordinary = ordinaryVerifier.verifyNondetMoore(automaton);
		final List<Counterexample> global = globalVerifier.verifyNondetMoore(automaton);
		final List<Counterexample> globalPatched = new ArrayList<>();
		for (Counterexample ce : global) {
			if (ce.isEmpty()) {
				globalPatched.add(ce);
			} else {
				final List<String> newEvents = new ArrayList<>();
				newEvents.add("");
				newEvents.addAll(ce.events());
				final List<List<String>> newActions = new ArrayList<>();
				newActions.add(Collections.emptyList());
				newActions.addAll(ce.actions());
				globalPatched.add(new Counterexample(newEvents, newActions, ce.loopLength));
			}
		}
		return Pair.of(ordinary, globalPatched);
	}
}

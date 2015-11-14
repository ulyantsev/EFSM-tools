package egorov.verifier;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import structures.Automaton;
import structures.Transition;
import structures.plant.NondetMooreAutomaton;
import egorov.ltl.LtlParseException;
import egorov.ltl.buchi.translator.TranslationException;

public class Verifier {
	private final List<String> ltlFormulae;
	private final Set<String> allEvents;
	private final Set<String> allActions;
	private final VerifierFactory verifier;
	
	public Verifier(Logger logger, List<String> ltlFormulae, List<String> events, List<String> actions, int varNumber) {
		this.ltlFormulae = ltlFormulae;
		logger.info(ltlFormulae.toString());

		allEvents = new TreeSet<>(events);
		allActions = new TreeSet<>(actions);
		ensureContextSufficiency();
		verifier = new VerifierFactory();

		try {
			verifier.prepareFormulas(ltlFormulae);
		} catch (TranslationException e) {
			logger.warning("Caught TranslationException: " + e.getMessage());
			e.printStackTrace();
		} catch (LtlParseException e) {
			logger.warning("Failed to parse formulae: " + ltlFormulae + " ");
			e.printStackTrace();
		}
	}
	
	private void ensureContextSufficiency() {
		final Pattern p1 = Pattern.compile("co\\.(\\w+)\\)");
		final Pattern p2 = Pattern.compile("ep\\.(\\w+)\\)");
		Matcher m;
		for (String formula : ltlFormulae) {
			m = p1.matcher(formula);
			while (m.find()) {
				assert allActions.contains(m.group(1));
			}
			m = p2.matcher(formula);
			while (m.find()) {
				assert allEvents.contains(m.group(1));
			}
		}
	}
	
	private Automaton removeDeadEnds(Automaton automaton) {
		Automaton currentA = automaton;
		while (true) {
			boolean changed = false;
			Automaton newA = new Automaton(automaton.stateCount());
			boolean[] deadEnd = new boolean[automaton.stateCount()];
			for (int i = 0; i < automaton.stateCount(); i++) {
				if (currentA.state(i).transitions().isEmpty()) {
					deadEnd[i] = true;
				}
			}
			for (int i = 0; i < automaton.stateCount(); i++) {
				for (Transition t : currentA.state(i).transitions()) {
					if (!deadEnd[t.dst().number()]) {
						newA.addTransition(newA.state(i), new Transition(newA.state(i),
								newA.state(t.dst().number()), t.event(),
								t.expr(), t.actions()));
					} else {
						changed = true;
					}
				}
			}
			if (!changed) {
				return currentA;
			}
			currentA = newA;
		}
	}
	
	public boolean verify(Automaton a) {
		return verifyWithCounterexamples(a).stream().allMatch(Counterexample::isEmpty);
	}
	
	public List<Counterexample> verifyWithCounterexamples(Automaton a) {
		return verifyWithCounterexamplesWithNoDeadEndRemoval(removeDeadEnds(a));
	}
	
	public List<Counterexample> verifyWithCounterexamplesWithNoDeadEndRemoval(Automaton a) {
		verifier.configureStateMachine(a);
		return verifier.verify();
	}
	
	public List<Counterexample> verifyWithCounterexamplesWithNoDeadEndRemoval(NondetMooreAutomaton a) {
		verifier.configureNondetMooreMachine(a);
		return verifier.verify();
	}
}

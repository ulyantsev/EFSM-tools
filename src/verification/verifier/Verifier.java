package verification.verifier;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import structures.Automaton;
import structures.Transition;
import structures.plant.NondetMooreAutomaton;
import verification.ltl.LtlParseException;
import verification.ltl.buchi.translator.TranslationException;

public class Verifier {
	private final List<String> ltlFormulae;
	private final Set<String> allEvents;
	private final Set<String> allActions;
	private final VerifierFactory verifier;
	
	public Verifier(Logger logger, List<String> ltlFormulae, List<String> events, List<String> actions, int varNumber) {
		this(logger, ltlFormulae, events, actions, varNumber, false);
	}
	
	public static final String G_REGEX = "^ *G *\\(.*$";
	
	public Verifier globalVerifier() {
		final VerifierFactory globalFactory = new VerifierFactory(true);
		
		final List<String> projection = ltlFormulae.stream()
				.filter(f -> f.matches(G_REGEX))
				.collect(Collectors.toList());
		try {
			globalFactory.prepareFormulas(projection);
		} catch (TranslationException | LtlParseException e) {
			throw new RuntimeException(e);
		}
		
		return new Verifier(projection, allEvents, allActions, globalFactory);
	}
	
	private Verifier(List<String> ltlFormulae, Set<String> events, Set<String> actions, VerifierFactory verifier) {
		this.ltlFormulae = ltlFormulae;
		allEvents = events;
		allActions = actions;
		this.verifier = verifier;
	}
		
	public Verifier(Logger logger, List<String> ltlFormulae, List<String> events, List<String> actions, int varNumber, boolean verifyFromAllStates) {
		this.ltlFormulae = ltlFormulae;
		logger.info(ltlFormulae.toString());

		allEvents = new TreeSet<>(events);
		allActions = new TreeSet<>(actions);
		ensureContextSufficiency();
		verifier = new VerifierFactory(verifyFromAllStates);

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
		final Pattern p1 = Pattern.compile("action\\((\\w+)\\)");
		final Pattern p2 = Pattern.compile("event\\((\\w+)\\)");
		Matcher m;
		for (String formula : ltlFormulae) {
			m = p1.matcher(formula);
			while (m.find()) {
				if (!allActions.contains(m.group(1))) {
					throw new RuntimeException("Unexpected action " + m.group(1));
				}
			}
			m = p2.matcher(formula);
			while (m.find()) {
				if (!allEvents.contains(m.group(1))) {
					throw new RuntimeException("Unexpected event " + m.group(1));
				}
			}
		}
	}
	
	private static Automaton removeDeadEnds(Automaton automaton) {
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
		verifier.configureDetMealyMachine(a);
		return verifier.verify();
	}
	
	public List<Counterexample> verifyNondetMoore(NondetMooreAutomaton a) {
		verifier.configureNondetMooreMachine(a);
		return verifier.verify();
	}
}

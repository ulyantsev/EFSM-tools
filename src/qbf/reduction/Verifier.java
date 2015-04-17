package qbf.reduction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.LtlParser;
import qbf.egorov.ltl.buchi.translator.TranslationException;
import qbf.egorov.transducer.FST;
import qbf.egorov.transducer.verifier.VerifierFactory;
import structures.Automaton;
import structures.Transition;

/**
 * (c) Igor Buzhinsky
 */

public class Verifier {
	private final Logger logger;
	private final int size;
	private final List<String> ltlFormulae;
	private final VerifierFactory verifier;
	private final Set<String> allEvents;
	private final Set<String> allActions;
	
	public Verifier(int size, Logger logger, String ltlPath, List<String> events, List<String> actions, int varNumber) {
		this.logger = logger;
		this.size = size;
		ltlFormulae = loadFormulae(ltlPath, varNumber);
		logger.info(ltlFormulae.toString());

		allEvents = new TreeSet<>(events);
		allActions = new TreeSet<>(actions);
		ensureContextSufficiency();
		verifier = new VerifierFactory(allEvents.toArray(new String[allEvents.size()]), allActions.toArray(new String[allActions.size()]));
		FST fst = new FST(new Automaton(size), allEvents, allActions, size);
		verifier.configureStateMachine(fst);

		try {
			while (true) {
				try {
					verifier.prepareFormulas(ltlFormulae);
					break;
				} catch (TranslationException e) {
					logger.warning("Caught TranslationException: " + e.getMessage());
				}
			}
		} catch (LtlParseException e) {
			logger.warning("Failed to parse formulae: " + ltlFormulae + " ");
			e.printStackTrace();
		}
	}

	private List<String> loadFormulae(String path, int varNumber) {
		List<String> formulae = new ArrayList<>();
		try (Scanner in = new Scanner(new File(path))) {
			while (in.hasNext()) {
				formulae.add(LtlParser.duplicateEvents(in.nextLine(), varNumber));
			}
		} catch (FileNotFoundException e) {
			logger.warning("File " + path + " not found: " + e.getMessage());
		}
		return formulae;
	}
	
	private void ensureContextSufficiency() {
		Pattern p1 = Pattern.compile("co\\.(\\w+)\\)");
		Pattern p2 = Pattern.compile("ep\\.(\\w+)\\)");
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
			Automaton newA = new Automaton(automaton.statesCount());
			boolean[] deadEnd = new boolean[automaton.statesCount()];
			for (int i = 0; i < automaton.statesCount(); i++) {
				if (currentA.getState(i).getTransitions().isEmpty()) {
					deadEnd[i] = true;
				}
			}
			for (int i = 0; i < automaton.statesCount(); i++) {
				for (Transition t : currentA.getState(i).getTransitions()) {
					if (!deadEnd[t.getDst().getNumber()]) {
						newA.addTransition(newA.getState(i), new Transition(newA.getState(i),
								newA.getState(t.getDst().getNumber()), t.getEvent(),
								t.getExpr(), t.getActions()));
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
		final FST fst = new FST(removeDeadEnds(a), allEvents, allActions, size);
		final int numberOfUsedTransitions = fst.getUsedTransitionsCount();
		verifier.configureStateMachine(fst);
		List<Integer> verified = verifier.verify().getLeft();
		return verified.stream().allMatch(x -> x == numberOfUsedTransitions);
	}
	
	public List<List<String>> verifyWithCounterExamples(Automaton a) {
		return verifyPure(a).getRight();
	}
	
	public Pair<List<Integer>, List<List<String>>> verifyPure(Automaton a) {
		final FST fst = new FST(removeDeadEnds(a), allEvents, allActions, size);
		verifier.configureStateMachine(fst);
		return verifier.verify();
	}
}

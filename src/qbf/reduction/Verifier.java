package qbf.reduction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import qbf.egorov.ltl.LtlParseException;
import qbf.egorov.ltl.buchi.translator.TranslationException;
import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.verifier.VerifierFactory;
import structures.Automaton;

/**
 * (c) Igor Buzhinsky
 */

public class Verifier {
	private final Logger logger;
	private final int size;
	private final List<String> ltlFormulae;
	
	public Verifier(int size, Logger logger, String ltlPath) {
		this.logger = logger;
		this.size = size;
		ltlFormulae = loadFormulae(ltlPath);
	}

	private List<String> loadFormulae(String path) {
		List<String> formulae = new ArrayList<>();
		try (Scanner in = new Scanner(new File(path))) {
			while (in.hasNext()) {
				formulae.add(in.nextLine());
			}
		} catch (FileNotFoundException e) {
			logger.warning("File " + path + " not found: " + e.getMessage());
		}
		return formulae;
	}
	
	private void fillEventsAndActionsFromFormulae(Set<String> allEvents, Set<String> allActions) {
		Pattern p1 = Pattern.compile("co\\.(\\w+)\\)");
		Pattern p2 = Pattern.compile("ep\\.(\\w+)\\)");
		Matcher m;
		for (String formula : ltlFormulae) {
			m = p1.matcher(formula);
			while (m.find()) {
				allActions.add(m.group(1));
			}
			m = p2.matcher(formula);
			while (m.find()) {
				allEvents.add(m.group(1));
			}
		}
	}
	
	public boolean verify(Automaton a) {
		Set<String> allEvents = new TreeSet<>();
		Set<String> allActions = new TreeSet<>();
		fillEventsAndActionsFromFormulae(allEvents, allActions);
		FST fst = new FST(a, allEvents, allActions, size);
		int numberOfUsedTransitions = fst.getUsedTransitionsCount();

		for (int i = 0; i < ltlFormulae.size(); i++) {
			List<String> f = Arrays.asList(ltlFormulae.get(i));
			VerifierFactory verifier = new VerifierFactory(fst.getSetOfInputs(), fst.getSetOfOutputs());
			verifier.configureStateMachine(fst);
			
			try {
				while (true) {
					try {
						verifier.prepareFormulas(f);
						break;
					} catch (TranslationException e) {
					}
				}
			} catch (LtlParseException e) {
				logger.warning("Failed to parse formula: " + ltlFormulae.get(i) + " ");
				e.printStackTrace();
				continue;
			}
			
			int result = verifier.verify()[0];
			if (result != numberOfUsedTransitions) {
				logger.info("EGOROV VERIFICATION FALSE");
				return false;		
			}
		}
		logger.info("EGOROV VERIFICATION TRUE");
		return true;
	}
}

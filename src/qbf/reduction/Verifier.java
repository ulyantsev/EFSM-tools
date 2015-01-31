package qbf.reduction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.verifier.IVerifierFactory;
import qbf.egorov.transducer.verifier.VerifierFactory;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;

/**
 * (c) Igor Buzhinsky
 */

public class Verifier {
	private final Logger logger;
	private final int size;
	private final List<String> ltlFormulae;
	private final ScenariosTree tree;
	
	public Verifier(ScenariosTree tree, String ltlFilePath, int size, Logger logger, String ltlPath) {
		this.logger = logger;
		this.size = size;
		this.tree = tree;
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
	
	private void logAutomation(Logger logger, Automaton a) {
		for (Node state : a.getStates()) {
			for (Transition t : state.getTransitions()) {
				logger.info(t.toString());
			}
		}
	}
	
	public boolean verify(Automaton a) {
		FST fst = new FST(a, Arrays.asList(tree.getEvents()), tree.getActions(), size);
		int numberOfUsedTransitions = fst.getUsedTransitionsCount();

		for (int i = 0; i < ltlFormulae.size(); i++) {
			List<String> f = Arrays.asList(ltlFormulae.get(i));
			IVerifierFactory verifier = new VerifierFactory(fst.getSetOfInputs(), fst.getSetOfOutputs());
			verifier.configureStateMachine(fst);
			try {
				verifier.prepareFormulas(f);
			} catch (Exception e) {
				logger.warning("Failed to parse formula: " + ltlFormulae.get(i) + " ");
				logAutomation(logger, a);
				continue;
			}
			logger.info("Parsed formula: " + ltlFormulae.get(i));
			logAutomation(logger, a);
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

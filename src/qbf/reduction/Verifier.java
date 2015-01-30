package qbf.reduction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

import qbf.egorov.transducer.algorithm.FST;
import qbf.egorov.transducer.verifier.IVerifierFactory;
import qbf.egorov.transducer.verifier.VerifierFactory;
import qbf.ltl.LtlNode;
import structures.Automaton;

/**
 * (c) Igor Buzhinsky
 */

public class Verifier {
	private final List<LtlNode> formulae;
	private final String resultFilePath;
	private final Logger logger;
	private final int size;
	private final String ltlPath;
	
	public Verifier(String resultFilePath, String ltlFilePath, int size, List<LtlNode> formulae, Logger logger, String ltlPath) {
		this.formulae = formulae;
		this.resultFilePath = resultFilePath;
		this.logger = logger;
		this.size = size;
		this.ltlPath = ltlPath;
	}

	private static List<String> loadFormulae(String path) {
		List<String> formulas = new ArrayList<>();
		Scanner in = null;
		try {
			in = new Scanner(new File(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while (in.hasNext()) {
			formulas.add(in.nextLine());
		}
		in.close();
		
		return formulas;
	}
	
	public boolean verify(Automaton a) throws IOException {
		try (PrintWriter resultPrintWriter = new PrintWriter(new File(resultFilePath))) {
			resultPrintWriter.println(a);
		} catch (FileNotFoundException e) {
			logger.warning("File " + resultFilePath + " not found: " + e.getMessage());
		}
		
		FST fst = new FST(a, size);

		List<String> formulas = loadFormulae(ltlPath);
		double numberOfUsedTransitions = fst.getUsedTransitionsCount();

		for (int i = 0; i < formulas.size(); i++) {
			List<String> f = new ArrayList<>();
			f.add(formulas.get(i));
			IVerifierFactory verifier = new VerifierFactory(fst.getSetOfInputs(), fst.getSetOfOutputs());
			verifier.configureStateMachine(fst);
			try {
				verifier.prepareFormulas(f);
			} catch (Exception e) {
				System.err.println("Failed to parse formula: " + formulas.get(i));
				continue;
			}
			double result = (double) verifier.verify()[0] / numberOfUsedTransitions;
			if (Math.abs(result - 1.0) >= 1e-5) {
				logger.info("EGOROV VERIFICATION FALSE");
				return false;		
			}
		}
		logger.info("EGOROV VERIFICATION TRUE");
		return true;
	}
}

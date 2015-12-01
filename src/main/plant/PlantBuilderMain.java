package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import sat_solving.SatSolver;
import scenario.StringScenario;
import structures.plant.MooreNode;
import structures.plant.NegativePlantScenarioForest;
import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import algorithms.automaton_builders.PlantAutomatonBuilder;
import egorov.ltl.LtlParseException;
import egorov.ltl.LtlParser;
import egorov.ltl.grammar.LtlNode;
import egorov.verifier.Counterexample;
import egorov.verifier.Verifier;
import egorov.verifier.VerifierPair;

public class PlantBuilderMain {
	@Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
	private List<String> arguments = new ArrayList<>();

	@Option(name = "--size", aliases = { "-s" }, usage = "automaton size", metaVar = "<size>", required = true)
	private int size;
	
	@Option(name = "--eventNumber", aliases = { "-en" }, usage = "number of events", metaVar = "<eventNumber>", required = true)
	private int eventNumber;
	
	@Option(name = "--eventNames", aliases = { "-enm" }, usage = "optional comma-separated event names (default: A, B, C, ...)",
			metaVar = "<eventNames>")
	private String eventNames;
	
	@Option(name = "--actionNumber", aliases = { "-an" }, usage = "number of actions", metaVar = "<actionNumber>", required = true)
	private int actionNumber;
	
	@Option(name = "--actionNames", aliases = { "-anm" }, usage = "optional comma-separated action names (default: z0, z1, z2, ...)",
			metaVar = "<actionNames>")
	private String actionNames;
	
	@Option(name = "--varNumber", aliases = { "-vn" }, usage = "number of variables (x0, x1, ...)", metaVar = "<varNumber>")
	private int varNumber = 0;
	
	@Option(name = "--log", aliases = { "-l" }, usage = "write log to this file", metaVar = "<file>")
	private String logFilePath;

	@Option(name = "--result", aliases = { "-r" }, usage = "write the obtained automaton in the GV format to this file",
			metaVar = "<GV file>")
	private String resultFilePath = "automaton.gv";

	@Option(name = "--tree", aliases = { "-t" }, usage = "write the obtained scenario tree in the GV format to this file",
			metaVar = "<GV file>")
	private String treeFilePath;

	@Option(name = "--ltl", aliases = { "-lt" }, usage = "file with LTL properties (optional)", metaVar = "<file>")
	private String ltlFilePath;
	
	@Option(name = "--actionspec", aliases = { "-as" }, usage = "file with action propositional formulae", metaVar = "<file>")
	private String actionspecFilePath;

	@Option(name = "--negsc", aliases = { "-ns" }, usage = "file with negative scenarios (optional)",
			metaVar = "<file>")
	private String negscFilePath;
	
	@Option(name = "--satSolver", aliases = { "-qss" }, usage = "SAT solver: LINGELING (default), CRYPTOMINISAT",
			metaVar = "<satSolver>")
	private String satSolver = SatSolver.LINGELING.name();
	
	@Option(name = "--solverParams", aliases = { "-sp" }, usage = "additional solver parameters", metaVar = "<solverParams>")
	private String solverParams = "";
	
	@Option(name = "--timeout", aliases = { "-to" }, usage = "solver timeout (sec)", metaVar = "<timeout>")
	private int timeout = 60 * 60 * 24;

	private void launcher(String[] args) throws IOException {
		Locale.setDefault(Locale.US);

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Plant automaton builder from scenarios and LTL formulae");
			System.out.println("Authors: Igor Buzhinsky (igor.buzhinsky@gmail.com), Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)\n");
			System.out.print("Usage: ");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}
		
		Logger logger = Logger.getLogger("Logger");
		if (logFilePath != null) {
			try {
				final FileHandler fh = new FileHandler(logFilePath, false);
				logger.addHandler(fh);
				final SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);

				logger.setUseParentHandlers(false);
				System.out.println("Log redirected to " + logFilePath);
			} catch (Exception e) {
				System.err.println("Can't work with file " + logFilePath + ": " + e.getMessage());
				return;
			}
		}

		final PositivePlantScenarioForest positiveForest = new PositivePlantScenarioForest();
		for (String filePath : arguments) {
			try {
				positiveForest.load(filePath, varNumber);
				logger.info("Loaded scenarios from " + filePath);
				logger.info("  Total scenarios tree size: " + positiveForest.nodeCount());
			} catch (IOException | ParseException e) {
				logger.warning("Can't load scenarios from file " + filePath);
				e.printStackTrace();
				return;
			}
		}
		
		

		if (treeFilePath != null) {
			try (PrintWriter treePrintWriter = new PrintWriter(new File(treeFilePath))) {
				treePrintWriter.println(positiveForest);
				logger.info("Scenarios tree saved to " + treeFilePath);
			} catch (Exception e) {
				logger.warning("Can't save scenarios tree to " + treeFilePath);
			}
		}
		
		SatSolver satsolver;
		try {
			satsolver = SatSolver.valueOf(satSolver);
		} catch (IllegalArgumentException e) {
			logger.warning(satSolver + " is not a valid SAT solver.");
			return;
		}
		
		List<String> eventnames;
		if (eventNames != null) {
			eventnames = Arrays.asList(eventNames.split(","));
			if (eventnames.size() != eventNumber) {
				logger.warning("The number of events in <eventNames> does not correspond to <eventNumber>!");
				return;
			}
		} else {
			eventnames = new ArrayList<>();
			for (int i = 0; i < eventNumber; i++) {
				eventnames.add(String.valueOf((char) ('A' +  i)));
			}
		}
		
		final List<String> events = new ArrayList<>();
		for (int i = 0; i < eventNumber; i++) {
			final String event = eventnames.get(i);
			for (int j = 0; j < 1 << varNumber; j++) {
				StringBuilder sb = new StringBuilder(event);
				for (int pos = 0; pos < varNumber; pos++) {
					sb.append(((j >> pos) & 1) == 1 ? 1 : 0);
				}
				events.add(sb.toString());
			}
		}
		
		List<String> actions;
		if (actionNames != null) {
			actions = Arrays.asList(actionNames.split(","));
			if (actions.size() != actionNumber) {
				logger.warning("The number of actions in <actionNames> does not correspond to <actionNumber>!");
				return;
			}
		} else {
			actions = new ArrayList<>();
			for (int i = 0; i < actionNumber; i++) {
				actions.add("z" + i);
			}
		}
		
		try {
			final List<String> strFormulae = LtlParser.load(ltlFilePath, varNumber, eventnames);
			final List<LtlNode> formulae = LtlParser.parse(strFormulae);
			logger.info("LTL formula from " + ltlFilePath);
			
			final List<StringScenario> scenarios = new ArrayList<>();
			for (String scenarioPath : arguments) {
				scenarios.addAll(StringScenario.loadScenarios(scenarioPath, varNumber));
			}
			
			final List<StringScenario> negativeScenarios = new ArrayList<>();
			final NegativePlantScenarioForest negativeForest = new NegativePlantScenarioForest();
			if (negscFilePath != null) {
				negativeScenarios.addAll(StringScenario.loadScenarios(negscFilePath, varNumber));
				negativeForest.load(negscFilePath, varNumber);
			}

			long startTime = System.currentTimeMillis();
			logger.info("Start building automaton");
			
			Optional<NondetMooreAutomaton> resultAutomaton = null;
			final VerifierPair verifier = new VerifierPair(logger, strFormulae, events, actions, varNumber);
			final long finishTime = System.currentTimeMillis() + timeout * 1000;
			resultAutomaton = PlantAutomatonBuilder.build(logger, positiveForest, negativeForest, size, solverParams,
					resultFilePath, ltlFilePath, actionspecFilePath, formulae, events, actions, satsolver, verifier, finishTime);
			final double executionTime = (System.currentTimeMillis() - startTime) / 1000.;
			
			if (!resultAutomaton.isPresent()) {
				logger.info("Automaton with " + size + " states NOT FOUND!");
				logger.info("Automaton builder execution time: " + executionTime);
			} else {
				logger.info("Automaton with " + size + " states WAS FOUND!");
				logger.info("Automaton builder execution time: " + executionTime);
				
				if (resultAutomaton.get().isCompliantWithScenarios(positiveForest)) {
					logger.info("COMPLIES WITH SCENARIOS");
				} else {
					logger.severe("NOT COMPLIES WITH SCENARIOS");
				}
				
				if (resultAutomaton.get().isCompliantWithNegativeScenarios(negativeScenarios)) {
					logger.info("COMPLIES WITH NEGATIVE SCENARIOS");
				} else {
					logger.severe("NOT COMPLIES WITH NEGATIVE SCENARIOS");
				}

				// writing to a file
				try (PrintWriter resultPrintWriter = new PrintWriter(new File(resultFilePath))) {
					resultPrintWriter.println(resultAutomaton.get());
				} catch (FileNotFoundException e) {
					logger.warning("File " + resultFilePath + " not found: " + e.getMessage());
				}
				
				final Verifier usualVerifier = new Verifier(logger, strFormulae, events, actions, varNumber);
				final List<Counterexample> counterexamples =
						usualVerifier.verifyNondetMoore(resultAutomaton.get());
				if (counterexamples.stream().allMatch(Counterexample::isEmpty)) {
					logger.info("VERIFIED");
				} else {
					logger.severe("NOT VERIFIED");
				}
				
				// completeness check
				boolean complete = true;
				for (MooreNode s : resultAutomaton.get().states()) {
					for (String event : events) {
						if (!s.transitions().stream().anyMatch(t -> t.event().endsWith(event))) {
							complete = false;
						}
					}
				}
				if (complete) {
					logger.info("COMPLETE");
				} else {
					logger.severe("INCOMPLETE");
				}
			}
		} catch (ParseException | LtlParseException e) {
			logger.warning("Can't get LTL formula from " + treeFilePath);
			throw new RuntimeException(e);
		}
	}

	public void run(String[] args) {
		try {
			launcher(args);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new PlantBuilderMain().run(args);
	}
}

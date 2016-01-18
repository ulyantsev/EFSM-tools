package main;

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
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import scenario.StringScenario;
import structures.Automaton;
import structures.NegativeScenarioTree;
import structures.Node;
import structures.ScenarioTree;
import algorithms.automaton_builders.FastAutomatonBuilder;
import egorov.ltl.LtlParseException;
import egorov.ltl.LtlParser;
import egorov.ltl.grammar.LtlNode;
import egorov.verifier.Verifier;

public class FastAutomatonBuilderMain {
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
			metaVar = "<file>")
	private String resultFilePath = "automaton.gv";

	@Option(name = "--tree", aliases = { "-t" }, usage = "write the obtained scenario tree in the GV format to this file",
			metaVar = "<file>")
	private String treeFilePath;

	@Option(name = "--ltl", aliases = { "-lt" }, usage = "file with LTL properties (optional)", metaVar = "<file>")
	private String ltlFilePath;
	
	@Option(name = "--negsc", aliases = { "-ns" }, usage = "file with negative scenarios (optional)",
			metaVar = "<file>")
	private String negscFilePath;
	
	@Option(name = "--timeout", aliases = { "-to" }, usage = "solver timeout (sec)", metaVar = "<timeout>")
	private int timeout = 60 * 60 * 24;

	@Option(name = "--complete", aliases = { "-cm" }, handler = BooleanOptionHandler.class, usage = "completeness")
	private boolean complete;
	
	@Option(name = "--bfsConstraints", aliases = { "-bc" }, handler = BooleanOptionHandler.class, usage = "BFS symmetry breaking")
	private boolean bfsConstraints;
	
	@Option(name = "--globalTree", aliases = { "-gt" }, handler = BooleanOptionHandler.class,
			usage = "use a special negative tree to handle finite counterexamples produced from G(...) formulae")
	private boolean globalTree;
	
	private void launcher(String[] args) throws IOException {
		Locale.setDefault(Locale.US);

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("FSM builder from scenarios and LTL formulae");
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

		final ScenarioTree positiveForest = new ScenarioTree();
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
			try (PrintWriter pw = new PrintWriter(new File(treeFilePath))) {
				pw.println(positiveForest);
				logger.info("Scenarios tree saved to " + treeFilePath);
			} catch (Exception e) {
				logger.warning("Can't save scenarios tree to " + treeFilePath);
			}
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
			final NegativeScenarioTree negativeForest = new NegativeScenarioTree();
			if (negscFilePath != null) {
				negativeScenarios.addAll(StringScenario.loadScenarios(negscFilePath, varNumber));
				negativeForest.load(negscFilePath, varNumber);
			}

			long startTime = System.currentTimeMillis();
			logger.info("Start building automaton");
			
			final Verifier verifier = new Verifier(logger, strFormulae, events, actions, varNumber);
			final long finishTime = System.currentTimeMillis() + timeout * 1000;
			final Optional<Automaton> resultAutomaton = FastAutomatonBuilder.build(logger,
					positiveForest, negativeForest, size, resultFilePath, strFormulae, formulae,
					events, actions, verifier, finishTime, complete, bfsConstraints,
					globalTree);
			final double executionTime = (System.currentTimeMillis() - startTime) / 1000.;
			
			if (!resultAutomaton.isPresent()) {
				logger.info("Automaton with " + size + " states NOT FOUND!");
				logger.info("Automaton builder execution time: " + executionTime);
			} else {
				logger.info("Automaton with " + size + " states WAS FOUND!");
				logger.info("Automaton builder execution time: " + executionTime);
				
				if (scenarios.stream().allMatch(sc -> resultAutomaton.get().isCompliantWithScenario(sc))) {
					logger.info("COMPLIES WITH SCENARIOS");
				} else {
					logger.severe("NOT COMPLIES WITH SCENARIOS");
				}
				
				if (negativeScenarios.stream().allMatch(sc -> !resultAutomaton.get().isCompliantWithScenario(sc))) {
					logger.info("COMPLIES WITH NEGATIVE SCENARIOS");
				} else {
					logger.severe("NOT COMPLIES WITH NEGATIVE SCENARIOS");
				}

				// writing to a file
				try (PrintWriter pw = new PrintWriter(new File(resultFilePath))) {
					pw.println(resultAutomaton.get());
				} catch (FileNotFoundException e) {
					logger.warning("File " + resultFilePath + " not found: " + e.getMessage());
				}
				
				boolean verified = verifier.verify(resultAutomaton.get());
				if (verified) {
					logger.info("VERIFIED");
				} else {
					logger.severe("NOT VERIFIED");
				}
				
				// completeness check
				boolean isComplete = true;
				if (complete) {
					for (Node s : resultAutomaton.get().states()) {
						if (s.transitionCount() != events.size()) {
							isComplete = false;
						}
					}
				} else {
					for (Node s : resultAutomaton.get().states()) {
						if (s.transitionCount() == 0) {
							isComplete = false;
						}
					}
				}
				if (isComplete) {
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
		new FastAutomatonBuilderMain().run(args);
	}
}

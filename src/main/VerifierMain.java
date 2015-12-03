package main;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import structures.Automaton;
import structures.plant.NondetMooreAutomaton;
import algorithms.AutomatonGVLoader;
import egorov.ltl.LtlParser;
import egorov.verifier.Counterexample;
import egorov.verifier.Verifier;

public class VerifierMain {
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
	
	@Option(name = "--ltl", aliases = { "-lt" }, usage = "file with LTL properties", metaVar = "<file>", required = true)
	private String ltlFilePath;

	@Option(name = "--automaton", aliases = { "-au" }, usage = "automaton to verify", metaVar = "<file>", required = true)
	private String automatonPath;
	
	@Option(name = "--plantModel", aliases = { "-pm" }, handler = BooleanOptionHandler.class, usage = "the supplied automaton is a plant model")
	private boolean plantModel;
	
	private void launcher(String[] args) throws IOException, ParseException {
		Locale.setDefault(Locale.US);

		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Interface to the built-in automaton verifier");
			System.out.println("Authors: Igor Buzhinsky (igor.buzhinsky@gmail.com)\n");
			System.out.print("Usage: ");
			parser.printSingleLineUsage(System.out);
			System.out.println();
			parser.printUsage(System.out);
			return;
		}
		
		final Logger logger = Logger.getLogger("Logger");
		
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
				final StringBuilder sb = new StringBuilder(event);
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
		
		final Verifier verifier = new Verifier(logger, LtlParser.load(ltlFilePath, varNumber, events),
				events, actions, varNumber);
		final List<Counterexample> counterexamples;
		if (plantModel) {
			final NondetMooreAutomaton a = NondetMooreAutomaton.readGV(automatonPath);
			counterexamples = verifier.verifyNondetMoore(a);
		} else {
			final Automaton a = AutomatonGVLoader.load(automatonPath);
			counterexamples = verifier.verifyWithCounterexamplesWithNoDeadEndRemoval(a);
		}
		for (Counterexample ce : counterexamples) {
			System.out.println(ce.isEmpty() ? "PASS" : ("FAIL " + ce));
		}
	}
	
	public void run(String[] args) {
		try {
			launcher(args);
		} catch (IOException | ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new VerifierMain().run(args);
	}
}

package main;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import structures.Automaton;
import structures.plant.NondetMooreAutomaton;
import verification.ltl.LtlParser;
import verification.verifier.Counterexample;
import verification.verifier.Verifier;
import algorithms.AutomatonGVLoader;

public class VerifierMain extends MainBase {
	@Option(name = "--eventNumber", aliases = { "-en" },
            usage = "number of events", metaVar = "<eventNumber>", required = true)
	private int eventNumber;
	
	@Option(name = "--eventNames", aliases = { "-enm" },
            usage = "optional comma-separated event names (default: A, B, C, ...)",
			metaVar = "<eventNames>")
	private String eventNames;
	
	@Option(name = "--actionNumber", aliases = { "-an" },
            usage = "number of actions", metaVar = "<actionNumber>", required = true)
	private int actionNumber;
	
	@Option(name = "--actionNames", aliases = { "-anm" },
            usage = "optional comma-separated action names (default: z0, z1, z2, ...)",
			metaVar = "<actionNames>")
	private String actionNames;
	
	@Option(name = "--varNumber", aliases = { "-vn" },
            usage = "number of variables (x0, x1, ...)", metaVar = "<varNumber>")
	private int varNumber = 0;
	
	@Option(name = "--ltl", aliases = { "-lt" },
            usage = "file with LTL properties", metaVar = "<file>", required = true)
	private String ltlFilePath;

	@Option(name = "--automaton", aliases = { "-au" },
            usage = "automaton to verify", metaVar = "<file>", required = true)
	private String automatonPath;
	
	@Option(name = "--plantModel", aliases = { "-pm" }, handler = BooleanOptionHandler.class,
            usage = "the supplied automaton is a plant model")
	private boolean plantModel;

    @Override
    protected void launcher() throws IOException {
        initializeLogger(null);
        final List<String> eventnames = eventNames(eventNames, eventNumber);
        final List<String> events = events(eventnames, eventNumber, varNumber);
        final List<String> actions = actions(actionNames, actionNumber);
		
		final Verifier verifier = new Verifier(logger(), LtlParser.load(ltlFilePath, varNumber, events),
				events, actions);
		final List<Counterexample> counterexamples;
		if (plantModel) {
			final NondetMooreAutomaton a = NondetMooreAutomaton.readGV(automatonPath);
			counterexamples = verifier.verifyNondetMoore(a);
		} else {
            try {
                final Automaton a = AutomatonGVLoader.load(automatonPath);
                counterexamples = verifier.verifyWithCounterexamplesWithNoDeadEndRemoval(a);
            } catch (ParseException e) {
                System.err.println("Can't read EFSM from file " + automatonPath);
                e.printStackTrace();
                return;
            }
		}
		for (Counterexample ce : counterexamples) {
			System.out.println(ce.isEmpty() ? "PASS" : ("FAIL " + ce));
		}
	}

	public static void main(String[] args) {
        new VerifierMain().run(args, Author.IB,
                "Interface to the built-in automaton verifier");
	}
}

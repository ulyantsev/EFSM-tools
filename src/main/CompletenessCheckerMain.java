package main;
import java.io.IOException;
import java.text.ParseException;

import algorithms.AutomatonCompletenessChecker;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import structures.Automaton;

public class CompletenessCheckerMain extends MainBase {
    @Argument(usage = "path to EFSM in Graphviz format", metaVar = "<efsm.gv>", required = true)
    private String efsm;

	public static void main(String[] args) {
        new CompletenessCheckerMain().run(args, Author.VU, "Tool for checking EFSM variable completeness");
	}

    @Override
    protected void launcher() throws IOException, ParseException {
        final Automaton automaton = loadAutomaton(efsm);
        System.out.println(AutomatonCompletenessChecker.checkCompleteness(automaton));
    }
}

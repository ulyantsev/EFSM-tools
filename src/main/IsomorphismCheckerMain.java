package main;
import java.io.IOException;
import java.text.ParseException;

import algorithms.AutomatonIsomorphismChecker;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import structures.mealy.MealyAutomaton;

public class IsomorphismCheckerMain extends MainBase {
    @Argument(usage = "path to EFSM in Graphviz format", metaVar = "<first.gv>", required = true, index = 0)
    private String efsm1;

    @Argument(usage = "path to EFSM in Graphviz format", metaVar = "<second.gv>", required = true, index = 1)
    private String efsm2;

    public static void main(String[] args) {
        new IsomorphismCheckerMain().run(args, Author.VU, "Tool for EFSM isomorphism checking");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        final MealyAutomaton a1 = loadAutomaton(efsm1);
        final MealyAutomaton a2 = loadAutomaton(efsm2);
        final boolean ans = AutomatonIsomorphismChecker.isIsomorphic(a1, a2);
        System.out.println(ans ? "ISOMORPHIC" : "NOT ISOMORPHIC");
    }
}

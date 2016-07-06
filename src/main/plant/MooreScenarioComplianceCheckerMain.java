package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import scenario.StringScenario;
import structures.moore.NondetMooreAutomaton;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

public class MooreScenarioComplianceCheckerMain extends MainBase {
    @Argument(usage = "path to Moore machine in Graphviz format", metaVar = "<fsm.gv>", required = true, index = 0)
    private String automatonFilepath;

    @Argument(usage = "scenario file path", metaVar = "<scenarios>", required = true, index = 1)
    private String sc;

    @Option(name = "--varNumber", aliases = {"-vn"},
            usage = "number of variables (x0, x1, ...)", metaVar = "<num>")
    private int varNumber = 0;

    public static void main(String[] args) {
        new MooreScenarioComplianceCheckerMain().run(args, Author.IB,
                "Tool for checking Moore machine compliance with a scenario set");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        final NondetMooreAutomaton automaton = NondetMooreAutomaton.readGV(automatonFilepath);
        final List<StringScenario> scenarios = loadScenarios(sc, varNumber);
        int ok = 0;
        for (int i = 0; i < scenarios.size(); i++) {
            final StringScenario sc = scenarios.get(i);
            final boolean result = automaton.compliesWith(Collections.singletonList(sc), true, false);
            //System.out.println(i + ": " + (result ? "COMPLIES" : "NOT COMPLIES"));
            ok += result ? 1 : 0;
        }
        System.out.println("COMPLIES WITH " + ok + " / " + scenarios.size());
        System.out.println("COMPLIANCE PERCENTAGE: " + (float) ok / scenarios.size() * 100 + "%");
    }
}

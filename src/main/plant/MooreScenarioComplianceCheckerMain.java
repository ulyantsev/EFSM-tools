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
import java.util.List;

public class MooreScenarioComplianceCheckerMain extends MainBase {
    @Argument(usage = "path to Moore machine in Graphviz format", metaVar = "<fsm.gv>", required = true, index = 0)
    private String automatonFilepath;

    @Argument(usage = "scenario file path", metaVar = "<scenarios>", required = true, index = 1)
    private String sc;

    @Option(name = "--varNumber", aliases = {"-vn"},
            usage = "number of variables (x0, x1, ...)", metaVar = "<num>")
    private int varNumber = 0;

    @Option(name = "--measure",
            usage = "how compliance with one scenario is measured: STRONG (yes or no, default), "
                    + "MEDIUM (length of the matching prefix / total length), "
                    + "WEAK (number of matched output symbols / total length)", metaVar = "<file>")
    private String measure = "STRONG";

    public static void main(String[] args) {
        new MooreScenarioComplianceCheckerMain().run(args, Author.IB,
                "Tool for checking Moore machine compliance with a scenario set");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        final NondetMooreAutomaton automaton = NondetMooreAutomaton.readGV(automatonFilepath);
        final List<StringScenario> scenarios = loadScenarios(sc, true);
        final double result;
        switch (measure) {
        case "STRONG":
            result = automaton.strongCompliance(scenarios);
            System.out.println("COMPLIES WITH " + Math.round(result * scenarios.size())
                    + " / " + scenarios.size());
            System.out.println("COMPLIANCE PERCENTAGE: " + (float) result * 100 + "%");
            break;
        case "MEDIUM":
            result = automaton.mediumCompliance(scenarios);
            System.out.println("MEDIUM COMPLIANCE: " + (float) result * 100 + "%");
            break;
        case "WEAK":
            result = automaton.weakCompliance(scenarios);
            System.out.println("WEAK COMPLIANCE: " + (float) result * 100 + "%");
            break;
        default:
            System.err.println("Unknown compliance measure!");
        }
    }
}

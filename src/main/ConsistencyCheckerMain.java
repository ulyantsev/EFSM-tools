package main;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import scenario.StringScenario;
import structures.Automaton;

public class ConsistencyCheckerMain extends MainBase {
    @Argument(usage = "path to EFSM in Graphviz format", metaVar = "<efsm.gv>", required = true, index = 0)
    private String efsm;

    @Argument(usage = "scenario file path", metaVar = "<scenarios>", required = true, index = 1)
    private String sc;

    public static void main(String[] args) {
        new ConsistencyCheckerMain().run(args, Author.VU, "Tool for checking EFSM and scenarios set consistency");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        final Automaton automaton = loadAutomaton(efsm);
        final List<StringScenario> scenarios = loadScenarios(sc, -1);

        int faultCount = 0;
        int actionsMistakes = 0;
        int scenariosSumLength = 0;
        for (StringScenario scenario : scenarios) {
            faultCount += automaton.compliesWith(scenario) ? 0 : 1;
            actionsMistakes += automaton.calcMissedActions(scenario);
            scenariosSumLength += scenario.size();
        }

        System.out.println("Total scenarios count: " + scenarios.size());
        System.out.println("Total scenarios length: " + scenariosSumLength);
        System.out.println("Complies with: " + (scenarios.size() - faultCount));
        System.out.println("Incomplies with: " + faultCount);
        System.out.printf("Complies percent: %.2f\n\n", 100. * (scenarios.size() - faultCount) / scenarios.size());
        System.out.println("Total actions mistakes done: " + actionsMistakes);
        System.out.printf("Actions mistakes percent: %.2f\n",
                100. * actionsMistakes / scenariosSumLength);
    }
}

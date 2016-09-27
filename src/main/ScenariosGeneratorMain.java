package main;
import java.io.IOException;
import java.text.ParseException;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import structures.mealy.MealyAutomaton;
import algorithms.ScenarioGenerator;

public class ScenariosGeneratorMain extends MainBase {
    @Option(name = "--automaton", aliases = { "-a" }, usage = "given EFSM in GV format", metaVar = "<fp>", required = true)
    private String automatonFilepath;

    @Option(name = "--count", aliases = { "-cnt" }, usage = "scenario number to generate", metaVar = "<cnt>")
    private int scenarioNumber;

    @Option(name = "--output", aliases = { "-o" }, usage = "file to write scenarios", metaVar = "<fp>")
    private String scenarioFilepath;

    @Option(name = "--randseed", aliases = { "-rs" }, usage = "random seed", metaVar = "<seed>")
    private int randseed;

    @Option(name = "--minLength", aliases = { "-minl" }, usage = "minimum scenario length", metaVar = "<min>")
    private int minLength;

    @Option(name = "--maxLength", aliases = { "-maxl" }, usage = "maximum scenario length", metaVar = "<max>")
    private int maxLength;

    @Option(name = "--sumLength", aliases = { "-suml" }, usage = "total scenario length", metaVar = "<sum>")
    private int sumLength;

    @Option(name = "--cover", aliases = { "-c" }, handler = BooleanOptionHandler.class, usage = "BFS-based generation")
    private boolean cover;

    public static void main(String[] args) {
        new ScenariosGeneratorMain().run(args, Author.VU,
                "Random scenario generator from given extended finite state machine (EFSM)");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        initializeRandom(randseed);
        final MealyAutomaton automaton = loadAutomaton(automatonFilepath);

        String scenarios = null;
        if (cover) {
            if (scenarioNumber != 0 || minLength != 0 || maxLength != 0) {
                System.err.println("With --cover option on, --count, --minLength, --maxLength options are not available");
                return;
            }

            scenarios = sumLength == 0
                    ? ScenarioGenerator.generateScenariosWithBFS(automaton)
                    : ScenarioGenerator.generateScenariosWithBFS(automaton, sumLength, random());
        } else {
            if (scenarioNumber == 0) {
                System.err.println("With --cover option OFF, --count option must be defined");
                return;             
            }
            
            if (maxLength == 0) {
                maxLength = automaton.stateCount();
            }

            if (sumLength == 0) {
                sumLength = (maxLength + minLength) * scenarioNumber / 2;
            }

            scenarios = ScenarioGenerator.generateScenarios(automaton, scenarioNumber,
                    minLength, maxLength, sumLength, random());
        }
        
        if (scenarioFilepath != null) {
            saveToFile(scenarios, scenarioFilepath);
        } else {
            System.out.println(scenarios);
        }
    }
}

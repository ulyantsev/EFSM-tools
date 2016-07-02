package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import algorithms.plant.PlantAutomatonBuilder;
import algorithms.plant.RapidPlantAutomatonBuilder;
import sat_solving.SatSolver;
import scenario.StringScenario;
import structures.plant.MooreNode;
import structures.plant.NegativePlantScenarioForest;
import structures.plant.NondetMooreAutomaton;
import structures.plant.PositivePlantScenarioForest;
import verification.ltl.LtlParser;
import verification.verifier.Counterexample;
import verification.verifier.NondetMooreVerifierPair;
import verification.verifier.Verifier;

public class PlantBuilderMain extends MainBase {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<>();

    @Option(name = "--size", aliases = {"-s"},
            usage = "automaton size", metaVar = "<size>", required = true)
    private int size;

    @Option(name = "--eventNumber", aliases = {"-en"},
            usage = "number of events", metaVar = "<eventNumber>", required = true)
    private int eventNumber;

    @Option(name = "--eventNames", aliases = {"-enm"},
            usage = "optional comma-separated event names (default: A, B, C, ...)",
            metaVar = "<eventNames>")
    private String eventNames;

    @Option(name = "--actionNumber", aliases = {"-an"},
            usage = "number of actions", metaVar = "<actionNumber>", required = true)
    private int actionNumber;

    @Option(name = "--actionNames", aliases = {"-anm"},
            usage = "optional comma-separated action names (default: z0, z1, z2, ...)",
            metaVar = "<actionNames>")
    private String actionNames;

    @Option(name = "--colorRules",
            usage = "comma-separated state coloring rules for GV output, each in the form action->color",
            metaVar = "<colorRules>")
    private String colorRules;

    @Option(name = "--varNumber", aliases = {"-vn"},
            usage = "number of variables (x0, x1, ...)", metaVar = "<varNumber>")
    private int varNumber = 0;

    @Option(name = "--log", aliases = {"-l"},
            usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--result", aliases = {"-r"},
            usage = "write the obtained automaton in the GV format to this file",
            metaVar = "<file>")
    private String resultFilePath = "automaton.gv";

    @Option(name = "--tree", aliases = {"-t"},
            usage = "write the obtained scenario tree in the GV format to this file",
            metaVar = "<file>")
    private String treeFilePath;

    @Option(name = "--ltl", aliases = {"-lt"},
            usage = "file with LTL properties (optional)", metaVar = "<file>")
    private String ltlFilePath;

    @Option(name = "--actionspec", aliases = {"-as"},
            usage = "file with action propositional formulae", metaVar = "<file>")
    private String actionspecFilePath;

    @Option(name = "--negsc", aliases = {"-ns"},
            usage = "file with negative scenarios (optional)",
            metaVar = "<file>")
    private String negscFilePath;

    @Option(name = "--timeout", aliases = {"-to"},
            usage = "solver timeout (sec)", metaVar = "<timeout>")
    private int timeout = 60 * 60 * 24;

    @Option(name = "--nusmv",
            usage = "file for NuSMV output (optional)", metaVar = "<file>")
    private String nusmvFilePath;

    @Option(name = "--fast", handler = BooleanOptionHandler.class,
            usage = "use the fast but inprecise way of model generation: "
                    + "size, negative scenarios, LTL formulae and action specifications are ignored")
    private boolean fast = false;

    @Option(name = "--solver",
            usage = "SAT solver: INCREMENTAL_CRYPTOMINISAT (default), LINGELING, CRYPTOMINISAT",
            metaVar = "<solver>")
    private String strSolver = SatSolver.INCREMENTAL_CRYPTOMINISAT.name();

    private Optional<NondetMooreAutomaton> resultAutomaton = null;
    private final Map<String, String> colorRuleMap = new LinkedHashMap<>();

    public Optional<NondetMooreAutomaton> resultAutomaton() {
        return resultAutomaton;
    }

    public Map<String, String> colorRuleMap() {
        return colorRuleMap;
    }

    public static void main(String[] args) {
        new PlantBuilderMain().run(args, Author.IB, "Plant automaton builder from scenarios and LTL formulae");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        initializeLogger(logFilePath);

        SatSolver solver;
        try {
            solver = SatSolver.valueOf(strSolver);
        } catch (IllegalArgumentException e) {
            logger().warning(strSolver + " is not a valid SAT solver.");
            return;
        }

        final PositivePlantScenarioForest positiveForest = new PositivePlantScenarioForest();
        final List<StringScenario> scenarios = new ArrayList<>();
        for (String scenarioPath : arguments) {
            scenarios.addAll(loadScenarios(scenarioPath, varNumber));
            logger().info("Loaded scenarios from " + scenarioPath);
        }
        scenarios.forEach(positiveForest::addScenario);
        logger().info("Scenario tree size: " + positiveForest.nodeCount());
        saveScenarioTree(positiveForest, treeFilePath);
        final List<String> eventnames = eventNames(eventNames, eventNumber);
        final List<String> events = events(eventnames, eventNumber, varNumber);
        final List<String> actions = actions(actionNames, actionNumber);
        final List<String> strFormulae = LtlParser.load(ltlFilePath, varNumber, eventnames);
        logger().info("LTL formula from " + ltlFilePath);

        final List<StringScenario> negativeScenarios = new ArrayList<>();
        final NegativePlantScenarioForest negativeForest = new NegativePlantScenarioForest();
        if (negscFilePath != null) {
            negativeScenarios.addAll(StringScenario.loadScenarios(negscFilePath, varNumber));
            negativeForest.load(negscFilePath, varNumber);
        }

        logger().info("Initializing the verifier...");
        final NondetMooreVerifierPair verifier = new NondetMooreVerifierPair(logger(), strFormulae,
                events, actions);
        final long finishTime = System.currentTimeMillis() + timeout * 1000;

        logger().info("Started building automaton.");

        resultAutomaton = fast
                ? RapidPlantAutomatonBuilder.build(positiveForest, events)
                : PlantAutomatonBuilder.build(logger(), positiveForest, negativeForest, size,
                actionspecFilePath, events, actions, verifier, finishTime, solver);

        if (!resultAutomaton.isPresent()) {
            logger().info("Automaton with " + size + " states NOT FOUND!");
            logger().info("Automaton builder execution time: " + executionTime());
        } else {
            logger().info("Automaton with " + resultAutomaton.get().stateCount()
                    + " states WAS FOUND!");
            logger().info("Automaton builder execution time: " + executionTime());

            if (resultAutomaton.get().isCompliantWithScenarios(scenarios, true, true)) {
                logger().info("COMPLIES WITH SCENARIOS");
            } else {
                logger().severe("NOT COMPLIES WITH SCENARIOS");
            }

            if (resultAutomaton.get().isCompliantWithScenarios(negativeScenarios, false, false)) {
                logger().info("COMPLIES WITH NEGATIVE SCENARIOS");
            } else {
                logger().severe("NOT COMPLIES WITH NEGATIVE SCENARIOS");
            }

            if (colorRules != null) {
                final String[] tokens = colorRules.split(",");
                // linked, since the order is important
                for (String token : tokens) {
                    final String[] parts = token.split("->");
                    if (parts.length != 2 || !actions.contains(parts[0])) {
                        logger().warning("Invalid color rule " + token + "!");
                    } else {
                        colorRuleMap.put(parts[0], parts[1]);
                    }
                }
            }

            saveToFile(resultAutomaton.get().toString(colorRuleMap, Optional.empty()), resultFilePath);

            if (nusmvFilePath != null) {
                saveToFile(resultAutomaton.get().toNuSMVString(events, actions,
                        new ArrayList<>(), Optional.empty()), nusmvFilePath);
            }

            final Verifier usualVerifier = new Verifier(logger(), strFormulae, events, actions);
            final List<Counterexample> counterexamples =
                    usualVerifier.verifyNondetMoore(resultAutomaton.get());
            if (counterexamples.stream().allMatch(Counterexample::isEmpty)) {
                logger().info("VERIFIED");
            } else {
                logger().severe("NOT VERIFIED");
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
                logger().info("COMPLETE");
            } else {
                logger().severe("INCOMPLETE");
            }
        }
    }
}

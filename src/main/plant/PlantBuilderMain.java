package main.plant;

/**
 * (c) Igor Buzhinsky
 */

import automaton_builders.PlantAutomatonBuilder;
import automaton_builders.RapidPlantAutomatonBuilder;
import automaton_builders.StateMergingNondetAutomatonBuilder;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import sat_solving.SatSolver;
import scenario.StringScenario;
import structures.moore.*;
import verification.ltl.LtlParser;
import verification.verifier.Counterexample;
import verification.verifier.NondetMooreVerifierPair;
import verification.verifier.Verifier;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class PlantBuilderMain extends MainBase {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<>();

    @Option(name = "--size", aliases = {"-s"},
            usage = "automaton size", metaVar = "<size>", required = true)
    private int size;

    @Option(name = "--eventNumber", aliases = { "-en" },
            usage = "number of events (default 1)", metaVar = "<eventNumber>")
    private int eventNumber = 1;

    @Option(name = "--eventNames", aliases = { "-enm" },
            usage = "optional comma-separated event names (default: A, B, C, ...)",
            metaVar = "<eventNames>")
    private String eventNames;

    @Option(name = "--actionNumber", aliases = { "-an" },
            usage = "number of actions (default 0)", metaVar = "<actionNumber>")
    private int actionNumber = 0;

    @Option(name = "--actionNames", aliases = { "-anm" },
            usage = "optional comma-separated action names (default: z0, z1, z2, ...)",
            metaVar = "<actionNames>")
    private String actionNames;

    @Option(name = "--varNumber", aliases = { "-vn" },
            usage = "number of variables (default 0)", metaVar = "<varNumber>")
    private int varNumber = 0;

    @Option(name = "--varNames", aliases = { "-vnm" },
            usage = "optional comma-separated variable names (default: x0, x1, ...)", metaVar = "<varNames>")
    private String varNames;

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
    private int timeout = 1_000_000_000;

    @Option(name = "--nusmv",
            usage = "file for NuSMV output (optional)", metaVar = "<file>")
    private String nusmvFilePath;

    @Option(name = "--fast", handler = BooleanOptionHandler.class,
            usage = "use the fast but imprecise way of model generation: "
                    + "size, negative scenarios, LTL formulae and action specifications are ignored")
    private boolean fast = false;

    @Option(name = "--solver",
            usage = "SAT solver: INCREMENTAL_CRYPTOMINISAT (default), LINGELING, CRYPTOMINISAT",
            metaVar = "<solver>")
    private String strSolver = SatSolver.INCREMENTAL_CRYPTOMINISAT.name();

    @Option(name = "--bfsConstraints", handler = BooleanOptionHandler.class,
            usage = "BFS symmetry breaking (only with --deterministic)")
    private boolean bfsConstraints;

    @Option(name = "--deterministic", handler = BooleanOptionHandler.class,
            usage = "produce a deterministic Moore machine (i.e. for a controller, not for a plant)")
    private boolean deterministic;

    @Option(name = "--incomplete", handler = BooleanOptionHandler.class,
            usage = "disable completeness")
    private boolean incomplete;

    @Option(name = "--sm", handler = BooleanOptionHandler.class,
            usage = "use state merging instead of satisfiability")
    private boolean stateMerging;

    @Option(name = "--timedConstraints", handler = BooleanOptionHandler.class,
            usage = "with --fast: limit loop execution times based on traces")
    private boolean timedConstraints;

    private Optional<NondetMooreAutomaton> resultAutomaton = null;

    public Optional<NondetMooreAutomaton> resultAutomaton() {
        return resultAutomaton;
    }

    public static void main(String[] args) {
        new PlantBuilderMain().run(args, Author.IB, "Plant automaton builder from scenarios and LTL formulae");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        initializeLogger(logFilePath);
        eventNumber = eventNames == null ? eventNumber : eventNames.split(",").length;
        actionNumber = actionNames == null ? actionNumber : actionNames.split(",").length;
        varNumber = varNames == null ? varNumber : varNames.split(",").length;
        registerVariableNames(varNames, varNumber);
        if (resultFilePath.isEmpty()) {
            resultFilePath = null;
        }
        if (nusmvFilePath != null && nusmvFilePath.isEmpty()) {
            nusmvFilePath = null;
        }

        SatSolver solver;
        try {
            solver = SatSolver.valueOf(strSolver);
        } catch (IllegalArgumentException e) {
            logger().warning(strSolver + " is not a valid SAT solver.");
            return;
        }

        final List<String> eventnames = eventNames(eventNames, eventNumber);
        final List<String> events = events(eventnames, eventNumber, varNumber);
        final List<String> actions = actions(actionNames, actionNumber);
        final long finishTime = System.currentTimeMillis() + (long) timeout * 1000;
        if (fast) {
            if (arguments.size() != 1) {
                logger().severe("Strictly one scenario argument is required for fast construction!");
                return;
            }
            final LazyPositivePlantScenarioForest lazyForest = new LazyPositivePlantScenarioForest(arguments.get(0),
                    true);
            resultAutomaton = RapidPlantAutomatonBuilder.build(lazyForest, events, timedConstraints);
            final NondetMooreAutomaton a = resultAutomaton.get();
            logger().info("Automaton with " + a.stateCount() + " states WAS FOUND!");
            logger().info("Automaton builder execution time: " + executionTime());
            save(a, events, actions);
        } else {
            final PositivePlantScenarioForest positiveForest = new PositivePlantScenarioForest(!deterministic);
            final List<StringScenario> scenarios = new ArrayList<>();
            for (String scenarioPath : arguments) {
                scenarios.addAll(loadScenarios(scenarioPath, true));
                logger().info("Loaded scenarios from " + scenarioPath);
            }
            scenarios.forEach(positiveForest::addScenario);
            logger().info("Scenario forest size: " + positiveForest.nodeCount());
            saveScenarioTree(positiveForest, treeFilePath);
            final List<String> strFormulae = LtlParser.load(ltlFilePath, varNumber, eventnames);
            logger().info("LTL formula from " + ltlFilePath);

            final List<StringScenario> negativeScenarios = new ArrayList<>();
            final NegativePlantScenarioForest negativeForest = new NegativePlantScenarioForest();
            if (negscFilePath != null) {
                negativeScenarios.addAll(StringScenario.loadScenarios(negscFilePath, true));
                negativeForest.load(negscFilePath, true);
            }

            logger().info("Initializing the verifier...");

            final NondetMooreVerifierPair verifier = new NondetMooreVerifierPair(logger(), strFormulae, events, actions);

            logger().info("Started building automaton.");

            resultAutomaton = stateMerging
                    ? StateMergingNondetAutomatonBuilder.build(logger(), events, actions, arguments, strFormulae)
                    : PlantAutomatonBuilder.build(logger(), positiveForest, negativeForest, size, actionspecFilePath,
                    events, actions, verifier, finishTime, solver, deterministic, bfsConstraints, !incomplete);

            if (!resultAutomaton.isPresent()) {
                logger().info("Automaton with " + size + " states NOT FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());
            } else {
                final NondetMooreAutomaton a = resultAutomaton.get();
                logger().info("Automaton with " + a.stateCount() + " states WAS FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());

                if (a.compliesWith(scenarios, true, true)) {
                    logger().info("COMPLIES WITH SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH SCENARIOS");
                }

                if (a.compliesWith(negativeScenarios, false, false)) {
                    logger().info("COMPLIES WITH NEGATIVE SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH NEGATIVE SCENARIOS");
                }

                save(a, events, actions);

                final Verifier usualVerifier = new Verifier(logger(), strFormulae, events, actions);
                final List<Counterexample> counterexamples = usualVerifier.verifyNondetMoore(a);
                if (counterexamples.stream().allMatch(Counterexample::isEmpty)) {
                    logger().info("VERIFIED");
                } else {
                    logger().severe("NOT VERIFIED");
                }

                // completeness check
                boolean complete = true;
                if (!incomplete) {
                    for (MooreNode s : a.states()) {
                        for (String event : events) {
                            complete &= s.transitions().stream().anyMatch(t -> t.event().endsWith(event));
                        }
                    }
                } else {
                    for (MooreNode s : a.states()) {
                        complete &= s.transitionCount() != 0;
                    }
                }
                if (complete) {
                    logger().info("COMPLETE");
                } else {
                    logger().severe("INCOMPLETE");
                }

                // determinism check
                if (deterministic) {
                    if (a.isDeterministic()) {
                        logger().info("DETERMINISTIC");
                    } else {
                        logger().severe("NONDETERMINISTIC");
                    }
                }
            }
        }
    }

    private void save(NondetMooreAutomaton a, List<String> events, List<String> actions) {
        if (resultFilePath != null) {
            saveToFile(a.toString(), resultFilePath);
        }
        if (nusmvFilePath != null) {
            saveToFile(a.toNuSMVString(events, actions, Optional.empty()), nusmvFilePath);
        }
    }
}

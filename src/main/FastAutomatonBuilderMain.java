package main;

/**
 * (c) Igor Buzhinsky
 */

import automaton_builders.FastAutomatonBuilder;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import sat_solving.SatSolver;
import scenario.StringScenario;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.NegativeScenarioTree;
import structures.mealy.ScenarioTree;
import verification.ltl.LtlParser;
import verification.verifier.Verifier;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FastAutomatonBuilderMain extends MainBase {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<>();

    @Option(name = "--size", aliases = { "-s" },
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

    @Option(name = "--log", aliases = { "-l" },
            usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--result", aliases = { "-r" },
            usage = "write the obtained automaton in the GV format to this file",
            metaVar = "<file>")
    private String resultFilePath = "automaton.gv";

    @Option(name = "--tree", aliases = { "-t" },
            usage = "write the obtained scenario tree in the GV format to this file",
            metaVar = "<file>")
    private String treeFilePath;

    @Option(name = "--ltl", aliases = { "-lt" },
            usage = "file with LTL properties", metaVar = "<file>")
    private String ltlFilePath;
    
    @Option(name = "--negsc", aliases = { "-ns" },
            usage = "file with negative scenarios",
            metaVar = "<file>")
    private String negscFilePath;
    
    @Option(name = "--timeout", aliases = { "-to" },
            usage = "solver timeout (sec)", metaVar = "<timeout>")
    private int timeout = 10_000_000;

    @Option(name = "--complete", aliases = { "-cm" }, handler = BooleanOptionHandler.class,
            usage = "completeness")
    private boolean complete;
    
    @Option(name = "--bfsConstraints", aliases = { "-bc" }, handler = BooleanOptionHandler.class,
            usage = "BFS symmetry breaking")
    private boolean bfsConstraints;
    
    @Option(name = "--globalTree", aliases = { "-gt" }, handler = BooleanOptionHandler.class,
            usage = "use a special negative tree to handle finite counterexamples produced from G(...) formulae")
    private boolean globalTree;

    @Option(name = "--solver",
            usage = "SAT solver: INCREMENTAL_CRYPTOMINISAT (default), LINGELING, CRYPTOMINISAT",
            metaVar = "<solver>")
    private String strSolver = SatSolver.INCREMENTAL_CRYPTOMINISAT.name();

    public static void main(String[] args) {
        new FastAutomatonBuilderMain().run(args, Author.IB, "FSM builder from scenarios and LTL formulae");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        initializeLogger(logFilePath);
        eventNumber = eventNames == null ? eventNumber : eventNames.split(",").length;
        actionNumber = actionNames == null ? actionNumber : actionNames.split(",").length;
        varNumber = varNames == null ? varNumber : varNames.split(",").length;
        registerVariableNames(varNames, varNumber);
        final ScenarioTree tree = loadScenarioTree(arguments, true);
        saveScenarioTree(tree, treeFilePath);
        final List<String> eventnames = eventNames(eventNames, eventNumber);
        final List<String> events = events(eventnames, eventNumber, varNumber);
        final List<String> actions = actions(actionNames, actionNumber);

        SatSolver solver;
        try {
            solver = SatSolver.valueOf(strSolver);
        } catch (IllegalArgumentException e) {
            logger().warning(strSolver + " is not a valid SAT solver.");
            return;
        }

        try {
            final List<String> strFormulae = LtlParser.load(ltlFilePath, varNumber, eventnames);
            logger().info("LTL formula from " + ltlFilePath);

            final List<StringScenario> scenarios = new ArrayList<>();
            for (String scenarioPath : arguments) {
                scenarios.addAll(StringScenario.loadScenarios(scenarioPath, true));
            }

            final List<StringScenario> negativeScenarios = new ArrayList<>();
            final NegativeScenarioTree negativeForest = new NegativeScenarioTree();
            if (negscFilePath != null) {
                negativeScenarios.addAll(StringScenario.loadScenarios(negscFilePath, true));
                negativeForest.load(negscFilePath, true);
            }

            logger().info("Start building automaton");

            final Verifier verifier = new Verifier(logger(), strFormulae, events, actions);
            final long finishTime = System.currentTimeMillis() + (long) timeout * 1000;
            final Optional<MealyAutomaton> resultAutomaton = FastAutomatonBuilder.build(logger(),
                    tree, negativeForest, size, strFormulae,
                    events, actions, verifier, finishTime, complete, bfsConstraints,
                    globalTree, solver);

            if (!resultAutomaton.isPresent()) {
                logger().info("Automaton with " + size + " states NOT FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());
            } else {
                final MealyAutomaton a = resultAutomaton.get();
                logger().info("Automaton with " + size + " states WAS FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());

                if (scenarios.stream().allMatch(sc -> a.compliesWith(sc))) {
                    logger().info("COMPLIES WITH SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH SCENARIOS");
                }

                if (negativeScenarios.stream().allMatch(sc -> !a.compliesWith(sc))) {
                    logger().info("COMPLIES WITH NEGATIVE SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH NEGATIVE SCENARIOS");
                }

                saveToFile(a, resultFilePath);

                boolean verified = verifier.verify(a);
                if (verified) {
                    logger().info("VERIFIED");
                } else {
                    logger().severe("NOT VERIFIED");
                }

                // completeness check
                boolean isComplete = true;
                if (complete) {
                    for (MealyNode s : a.states()) {
                        isComplete &= s.transitionCount() == events.size();
                    }
                } else {
                    for (MealyNode s : a.states()) {
                        isComplete &= s.transitionCount() != 0;
                    }
                }
                if (isComplete) {
                    logger().info("COMPLETE");
                } else {
                    logger().severe("INCOMPLETE");
                }
            }
        } catch (ParseException e) {
            logger().warning("Can't get LTL formula from " + treeFilePath);
            throw new RuntimeException(e);
        }
    }
}

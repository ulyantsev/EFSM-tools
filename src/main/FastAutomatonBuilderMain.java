package main;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

import scenario.StringScenario;
import structures.Automaton;
import structures.NegativeScenarioTree;
import structures.Node;
import structures.ScenarioTree;
import verification.ltl.LtlParser;
import verification.verifier.Verifier;
import algorithms.automaton_builders.FastAutomatonBuilder;

public class FastAutomatonBuilderMain extends MainBase {
	@Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
	private List<String> arguments = new ArrayList<>();

	@Option(name = "--size", aliases = { "-s" },
            usage = "automaton size", metaVar = "<size>", required = true)
	private int size;
	
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
	private int timeout = 60 * 60 * 24;

	@Option(name = "--complete", aliases = { "-cm" }, handler = BooleanOptionHandler.class,
            usage = "completeness")
	private boolean complete;
	
	@Option(name = "--bfsConstraints", aliases = { "-bc" }, handler = BooleanOptionHandler.class,
            usage = "BFS symmetry breaking")
	private boolean bfsConstraints;
	
	@Option(name = "--globalTree", aliases = { "-gt" }, handler = BooleanOptionHandler.class,
			usage = "use a special negative tree to handle finite counterexamples produced from G(...) formulae")
	private boolean globalTree;

	public static void main(String[] args) {
        new FastAutomatonBuilderMain().run(args, Author.IB, "FSM builder from scenarios and LTL formulae");
	}

    @Override
    protected void launcher() throws IOException, ParseException {
        initializeLogger(logFilePath);
        final ScenarioTree tree = loadScenarioTree(arguments, varNumber);
        saveScenarioTree(tree, treeFilePath);
        final List<String> eventnames = eventNames(eventNames, eventNumber);
        final List<String> events = events(eventnames, eventNumber, varNumber);
        final List<String> actions = actions(actionNames, actionNumber);

        try {
            final List<String> strFormulae = LtlParser.load(ltlFilePath, varNumber, eventnames);
            logger().info("LTL formula from " + ltlFilePath);

            final List<StringScenario> scenarios = new ArrayList<>();
            for (String scenarioPath : arguments) {
                scenarios.addAll(StringScenario.loadScenarios(scenarioPath, varNumber));
            }

            final List<StringScenario> negativeScenarios = new ArrayList<>();
            final NegativeScenarioTree negativeForest = new NegativeScenarioTree();
            if (negscFilePath != null) {
                negativeScenarios.addAll(StringScenario.loadScenarios(negscFilePath, varNumber));
                negativeForest.load(negscFilePath, varNumber);
            }

            logger().info("Start building automaton");

            final Verifier verifier = new Verifier(logger(), strFormulae, events, actions);
            final long finishTime = System.currentTimeMillis() + timeout * 1000;
            final Optional<Automaton> resultAutomaton = FastAutomatonBuilder.build(logger(),
                    tree, negativeForest, size, strFormulae,
                    events, actions, verifier, finishTime, complete, bfsConstraints,
                    globalTree);

            if (!resultAutomaton.isPresent()) {
                logger().info("Automaton with " + size + " states NOT FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());
            } else {
                logger().info("Automaton with " + size + " states WAS FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());

                if (scenarios.stream().allMatch(sc -> resultAutomaton.get().compliesWith(sc))) {
                    logger().info("COMPLIES WITH SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH SCENARIOS");
                }

                if (negativeScenarios.stream().allMatch(sc -> !resultAutomaton.get().compliesWith(sc))) {
                    logger().info("COMPLIES WITH NEGATIVE SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH NEGATIVE SCENARIOS");
                }

                saveToFile(resultAutomaton.get(), resultFilePath);

                boolean verified = verifier.verify(resultAutomaton.get());
                if (verified) {
                    logger().info("VERIFIED");
                } else {
                    logger().severe("NOT VERIFIED");
                }

                // completeness check
                boolean isComplete = true;
                if (complete) {
                    for (Node s : resultAutomaton.get().states()) {
                        if (s.transitionCount() != events.size()) {
                            isComplete = false;
                        }
                    }
                } else {
                    for (Node s : resultAutomaton.get().states()) {
                        if (s.transitionCount() == 0) {
                            isComplete = false;
                        }
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

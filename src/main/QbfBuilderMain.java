package main;

/**
 * (c) Igor Buzhinsky
 */

import algorithms.AutomatonCompleter.CompletenessType;
import automaton_builders.BacktrackingAutomatonBuilder;
import automaton_builders.CounterexampleAutomatonBuilder;
import automaton_builders.QbfAutomatonBuilder;
import automaton_builders.StateMergingAutomatonBuilder;
import bool.MyBooleanExpression;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import sat_solving.QbfSolver;
import sat_solving.SatSolver;
import sat_solving.SolvingStrategy;
import scenario.StringScenario;
import structures.mealy.*;
import verification.ltl.LtlParseException;
import verification.ltl.LtlParser;
import verification.ltl.grammar.LtlNode;
import verification.ltl.grammar.LtlUtils;
import verification.verifier.Verifier;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class QbfBuilderMain extends MainBase {
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
            metaVar = "<GV file>")
    private String resultFilePath = "automaton.gv";

    @Option(name = "--tree", aliases = { "-t" },
            usage = "write the obtained scenario tree in the GV format to this file",
            metaVar = "<GV file>")
    private String treeFilePath;

    @Option(name = "--ltl", aliases = { "-lt" },
            usage = "file with LTL properties (optional)", metaVar = "<file>")
    private String ltlFilePath;

    @Option(name = "--negsc", aliases = { "-ns" },
            usage = "file with negative scenarios (optional, does not work for all solving modes)",
            metaVar = "<file>")
    private String negscFilePath;
    
    @Option(name = "--qbfSolver", aliases = { "-qs" },
            usage = "QBF solver (only for the QSAT strategy): DEPQBF is the only supported option",
            metaVar = "<qbfSolver>")
    private String qbfSolver = QbfSolver.DEPQBF.name();
    
    @Option(name = "--satSolver", aliases = { "-qss" },
            usage = "SAT solver: LINGELING (default), CRYPTOMINISAT",
            metaVar = "<satSolver>")
    private String satSolver = SatSolver.LINGELING.name();
    
    @Option(name = "--timeout", aliases = { "-to" },
            usage = "solver timeout (sec)", metaVar = "<timeout>")
    private int timeout = 10_000_000;
    
    @Option(name = "--strategy", aliases = { "-str" },
            usage = "solving mode: QSAT, EXP_SAT, BACKTRACKING, COUNTEREXAMPLE (default), STATE_MERGING",
            metaVar = "<strategy>")
    private String strategy = SolvingStrategy.COUNTEREXAMPLE.name();
    
    @Option(name = "--completenessType", aliases = { "-ct" },
            usage = "NORMAL = usual completeness, NO_DEAD_ENDS = at least one transition from each state)",
            metaVar = "<completenessType>")
    private String completenessType = CompletenessType.NORMAL.name();

    @Option(name = "--noCompletenessHeuristics", aliases = { "-nc" }, handler = BooleanOptionHandler.class,
            usage = "disable the completeness heuristics")
    private boolean noCompletenessHeuristics;
    
    @Option(name = "--ensureCoverageAndWeakCompleteness", aliases = { "-ec" }, handler = BooleanOptionHandler.class,
            usage = "special backtracking execution mode which ensures FSM coverage by the scenarios and its so-called weak completeness (this is a temporary feature, no LTL support!)")
    private boolean ensureCoverageAndWeakCompleteness;
    
    @Option(name = "--backtrackingErrorNumber", aliases = { "-ben" },
            usage = "special backtracking execution mode for the case of errors in scenarios (this is a temporary feature, no LTL support!)",
            metaVar = "<errorNumber>")
    private int backtrackingErrorNumber = -1;

    public static void main(String[] args) {
        new QbfBuilderMain().run(args, Author.IB, "Automaton builder from scenarios and LTL formulae");
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
        
        SolvingStrategy ss;
        try {
            ss = SolvingStrategy.valueOf(strategy);
        } catch (IllegalArgumentException e) {
            logger().warning(strategy + " is not a valid solving strategy.");
            return;
        }
        
        QbfSolver qbfsolver;
        try {
            qbfsolver = QbfSolver.valueOf(qbfSolver);
        } catch (IllegalArgumentException e) {
            logger().warning(qbfSolver + " is not a valid QBF solver.");
            return;
        }
        
        SatSolver satsolver;
        try {
            satsolver = SatSolver.valueOf(satSolver);
        } catch (IllegalArgumentException e) {
            logger().warning(satSolver + " is not a valid SAT solver.");
            return;
        }
        
        CompletenessType completenesstype;
        try {
            completenesstype = CompletenessType.valueOf(completenessType);
        } catch (IllegalArgumentException e) {
            logger().warning(completenessType + " is not a valid completeness type.");
            return;
        }
        
        final List<String> eventnames = eventNames(eventNames, eventNumber);
        final List<String> events = events(eventnames, eventNumber, varNumber);
        final List<String> actions = actions(actionNames, actionNumber);
        
        try {
            List<String> strFormulae = LtlParser.load(ltlFilePath, varNumber, eventnames);
            if (ss == SolvingStrategy.QSAT || ss == SolvingStrategy.EXP_SAT) {
                strFormulae = strFormulae.stream().map(LtlUtils::expandEventList).collect(Collectors.toList());
            }
            final List<LtlNode> formulae = LtlParser.parse(strFormulae);
            logger().info("LTL formulae from " + ltlFilePath);
            
            final List<StringScenario> scenarios = new ArrayList<>();
            for (String scenarioPath : arguments) {
                scenarios.addAll(StringScenario.loadScenarios(scenarioPath, true));
            }
            
            final NegativeScenarioTree negativeTree = new NegativeScenarioTree();
            if (negscFilePath != null) {
                negativeTree.load(negscFilePath, true);
            }
            
            logger().info("Start building automaton");
            
            Optional<MealyAutomaton> resultAutomaton = null;
            final Verifier verifier = new Verifier(logger(), strFormulae, events, actions);
            final long finishTime = startTime() + (long) timeout * 1000;
            switch (ss) {
            case QSAT: case EXP_SAT:
                resultAutomaton = QbfAutomatonBuilder.build(logger(), tree, formulae, size,
                        qbfsolver, ss == SolvingStrategy.EXP_SAT,
                        events, actions, satsolver, verifier, finishTime, completenesstype);
                break;
            case COUNTEREXAMPLE:
                resultAutomaton = CounterexampleAutomatonBuilder.build(logger(), tree, size,
                        events, actions, satsolver, verifier, finishTime,
                        completenesstype, negativeTree, !noCompletenessHeuristics);
                break;
            case STATE_MERGING:
                resultAutomaton = StateMergingAutomatonBuilder.build(logger(),
                        verifier, arguments, negscFilePath);
                break;
            case BACKTRACKING:
                resultAutomaton = BacktrackingAutomatonBuilder.build(logger(), tree, size,
                        formulae, events, actions, verifier, finishTime,
                        completenesstype, varNumber, ensureCoverageAndWeakCompleteness, eventnames,
                        backtrackingErrorNumber, scenarios);
                break;
            }

            if (!resultAutomaton.isPresent()) {
                logger().info("Automaton with " + size + " states NOT FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());
            } else {
                logger().info("Automaton with " + size + " states WAS FOUND!");
                logger().info("Automaton builder execution time: " + executionTime());
                
                // compliance with scenarios
                if (scenarios.stream().allMatch(resultAutomaton.get()::compliesWith)) {
                    logger().info("COMPLIES WITH SCENARIOS");
                } else {
                    logger().severe("NOT COMPLIES WITH SCENARIOS");
                }

                saveToFile(resultAutomaton.get(), resultFilePath);
                
                // verification
                boolean verified = verifier.verify(resultAutomaton.get());
                if (verified) {
                    logger().info("VERIFIED");
                } else {
                    logger().severe("NOT VERIFIED");
                }
                
                // bfs check
                if (ss != SolvingStrategy.BACKTRACKING) {
                    if (checkBfs(resultAutomaton.get(), events, logger())) {
                        logger().info("BFS");
                    } else {
                        logger().info("NOT BFS (possibly due to transition redirections)");
                    }
                }
                
                // completeness check
                boolean complete = true;
                switch (completenesstype) {
                case NORMAL:
                    for (MealyNode s : resultAutomaton.get().states()) {
                        if (s.transitionCount() != events.size()) {
                            complete = false;
                        }
                    }
                    break;
                case NO_DEAD_ENDS:
                    for (MealyNode s : resultAutomaton.get().states()) {
                        if (s.transitionCount() == 0) {
                            complete = false;
                        }
                    }
                    break;
                }
                if (complete) {
                    logger().info("COMPLETE");
                } else {
                    logger().severe("INCOMPLETE");
                }
            }
        } catch (LtlParseException e) {
            logger().warning("Can't get LTL formula from " + treeFilePath);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            logger().warning("ParseException");
            throw new RuntimeException(e);
        }
    }

    static boolean checkBfs(MealyAutomaton a, List<String> events, Logger logger) {
        final Deque<Integer> queue = new ArrayDeque<>();
        final boolean[] visited = new boolean[a.stateCount()];
        visited[a.startState().number()] = true;
        queue.add(a.startState().number());
        final List<Integer> dequedStates = new ArrayList<>();
        while (!queue.isEmpty()) {
            final int stateNum = queue.pollFirst();
            dequedStates.add(stateNum);
            for (String e : events) {
                MealyTransition t = a.state(stateNum).transition(e, MyBooleanExpression.getTautology());
                if (t != null) {
                    final int dst = t.dst().number();
                    if (!visited[dst]) {
                        queue.add(dst);
                    }
                    visited[dst] = true;
                }
            }
        }
        final List<Integer> sortedList = dequedStates.stream().sorted().collect(Collectors.toList());
        if (sortedList.equals(dequedStates)) {
            return true;
        } else {
            logger.warning(dequedStates + " instead of " + sortedList);
            return false;
        }
    }
}

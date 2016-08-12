package main;
import automaton_builders.ChocoAutomatonBuilder;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;
import structures.mealy.MealyAutomaton;
import structures.mealy.ScenarioTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class Main extends MainBase {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<>();

    @Option(name = "--size", aliases = {"-s"},
            usage = "automaton size", metaVar = "<size>", required = true)
    private int size;

    @Option(name = "--log", aliases = {"-l"},
            usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--result", aliases = {"-r"},
            usage = "write result automaton in GV format to this file", metaVar = "<GV file>")
    private String resultFilePath;

    @Option(name = "--tree", aliases = {"-t"},
            usage = "write scenarios tree in GV format to this file", metaVar = "<GV file>")
    private String treeFilePath;

    @Option(name = "--model", aliases = {"-m"},
            usage = "write CSP model to this file", metaVar = "<file>")
    private String modelFilePath;

    @Option(name = "--complete", aliases = {"-c"}, handler = BooleanOptionHandler.class,
            usage = "should the automaton be complete")
    private boolean isComplete;

    @Option(name = "--weak", aliases = {"-w"}, handler = BooleanOptionHandler.class,
            usage = "activate weak completeness, available in <-c> mode")
    private boolean isWeakCompleteness;

    @Option(name = "--all", aliases = {"-a"}, handler = BooleanOptionHandler.class,
            usage = "TODO find all solutions (without symmetry breaking)")
    private boolean solveAll;

    public static void main(String[] args) {
        new Main().run(args, Author.VU, "Extended finite state machine identification tool");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        if (isWeakCompleteness && !isComplete) {
            System.out.println("Unable to use <-w> option without <-c> option");
            return;
        }

        initializeLogger(logFilePath);
        final ScenarioTree tree = loadScenarioTree(arguments, false);
        saveScenarioTree(tree, treeFilePath);

        PrintWriter modelPrintWriter = null;
        if (modelFilePath != null) {
            try {
                modelPrintWriter = new PrintWriter(new File(modelFilePath));
                logger().info("CSP model file " + modelFilePath);
            } catch (FileNotFoundException e) {
                logger().warning("File " + modelFilePath + " not found: " + e.getMessage());
            }
        }

        if (!solveAll) {
            logger().info("Start building automaton with Choco CSP solver");
            MealyAutomaton resultAutomaton;
            if (modelPrintWriter == null) {
                resultAutomaton = ChocoAutomatonBuilder.build(tree, size, isComplete, isWeakCompleteness);
            } else {
                resultAutomaton =
                        ChocoAutomatonBuilder.build(tree, size, isComplete, isWeakCompleteness, modelPrintWriter);
                modelPrintWriter.close();
            }

            if (resultAutomaton == null) {
                logger().info("Automaton with " + size + " states NOT FOUND!");
            } else {
                logger().info("Automaton with " + size + " states WAS FOUND!");
                saveToFile(resultAutomaton, resultFilePath);
            }
            logger().info("Choco automaton builder execution time: " + executionTime());
        } else {
            logger().info("Start building all feasible automatons with Choco CSP solver");
            final List<MealyAutomaton> result =
                    ChocoAutomatonBuilder.buildAll(tree, size, isComplete, isWeakCompleteness, modelPrintWriter);
            if (modelPrintWriter != null) {
                modelPrintWriter.close();
            }

            logger().info(result.size() + " solutions found");
            for (MealyAutomaton automaton : result) {
                saveToFile(automaton, resultFilePath);
            }
            logger().info("Choco execution time for all solutions: " + executionTime());
        }
    }
}

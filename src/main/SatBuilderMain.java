package main;
import automaton_builders.CryptominisatAutomatonBuilder;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import structures.mealy.MealyAutomaton;
import structures.mealy.ScenarioTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class SatBuilderMain extends MainBase {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<>();

    @Option(name = "--size", aliases = { "-s" },
            usage = "automaton size", metaVar = "<size>", required = true)
    private int size;

    @Option(name = "--log", aliases = { "-l" },
            usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--result", aliases = { "-r" },
            usage = "write result automaton in GV format to this file", metaVar = "<GV file>")
    private String resultFilePath;

    @Option(name = "--tree", aliases = { "-t" },
            usage = "write scenarios tree in GV format to this file", metaVar = "<GV file>")
    private String treeFilePath;

    @Option(name = "--model", aliases = { "-m" },
            usage = "write SAT model to this file", metaVar = "<file>")
    private String modelFilePath;

    public static void main(String[] args) {
        new SatBuilderMain().run(args, Author.VU,
                "Extended finite state machine CryptoMiniSat based identification tool");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        try {
            Runtime.getRuntime().exec("cryptominisat4");
        } catch (Exception e) {
            System.err.println("ERROR: Problems with CryptoMiniSat execution");
            e.printStackTrace();
            return;
        }

        initializeLogger(logFilePath);
        final ScenarioTree tree = loadScenarioTree(arguments, false);
        saveScenarioTree(tree, treeFilePath);

        PrintWriter modelPrintWriter = null;
        if (modelFilePath != null) {
            try {
                modelPrintWriter = new PrintWriter(new File(modelFilePath));
                logger().info("SAT model file " + modelFilePath);
            } catch (FileNotFoundException e) {
                logger().warning("File " + modelFilePath + " not found: " + e.getMessage());
            }
        }

        logger().info("Start building automaton with CryptoMiniSat");
        final MealyAutomaton resultAutomaton;

        if (modelPrintWriter == null) {
            resultAutomaton = CryptominisatAutomatonBuilder.build(tree, size);
        } else {
            resultAutomaton = CryptominisatAutomatonBuilder.build(tree, size, modelPrintWriter);
            modelPrintWriter.close();
        }

        if (resultAutomaton == null) {
            logger().info("Automaton with " + size + " states NOT FOUND!");
        } else {
            logger().info("Automaton with " + size + " states WAS FOUND!");
            saveToFile(resultAutomaton, resultFilePath);
        }
        logger().info("SAT-solver automaton builder execution time: " + executionTime());
    }
}

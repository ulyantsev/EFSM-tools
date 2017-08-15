package meta;

import algorithms.AutomatonGVLoader;
import bool.MyBooleanExpression;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import scenario.StringScenario;
import structures.mealy.MealyAutomaton;
import structures.mealy.ScenarioTree;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by buzhinsky on 7/1/16.
 */
public abstract class MainBase {
    private static Random RANDOM;
    private static long START_TIME;
    private static Logger LOGGER;

    protected Random random() {
        return RANDOM;
    }

    protected void initializeRandom(int seed) {
        RANDOM = seed == 0 ? new Random() : new Random(seed);
    }

    protected long startTime() {
        return START_TIME;
    }

    protected double executionTime() {
        return (System.currentTimeMillis() - startTime()) / 1000.;
    }

    protected void initializeLogger(String logFilePath) {
        LOGGER = Logger.getLogger("Logger");
        if (logFilePath != null) {
            try {
                final FileHandler fh = new FileHandler(logFilePath, false);
                LOGGER.addHandler(fh);
                final SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);
                LOGGER.setUseParentHandlers(false);
                System.out.println("Log redirected to " + logFilePath);
            } catch (Exception e) {
                System.err.println("Can't work with file " + logFilePath + ": " + e.getMessage());
            }
        }
    }

    public Logger logger() {
        return LOGGER;
    }

    protected abstract void launcher() throws IOException, ParseException;

    public void run(String[] args, Author author, String intro) {
        START_TIME = System.currentTimeMillis();
        Locale.setDefault(Locale.US);
        if (!parseArgs(args, author, intro)) {
            return;
        }
        try {
            launcher();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private boolean parseArgs(String[] args, Author author, String intro) {
        final CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            return true;
        } catch (CmdLineException e) {
            System.out.println(intro);
            System.out.println(author);
            System.out.println();
            System.out.print("Usage: java -jar <jar filename> ");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
            return false;
        }
    }

    protected ScenarioTree loadScenarioTree(List<String> filePaths, boolean removeVars) throws IOException, ParseException {
        final ScenarioTree tree = new ScenarioTree();
        for (String filePath : filePaths) {
            try {
                tree.load(filePath, removeVars);
                logger().info("Loaded scenarios from " + filePath);
                logger().info("  Total scenarios tree size: " + tree.nodeCount());
            } catch (IOException | ParseException e) {
                logger().warning("Can't load scenarios from file " + filePath);
                throw e;
            }
        }
        return tree;
    }

    protected List<StringScenario> loadScenarios(String filename, boolean removeVars)
            throws IOException, ParseException {
        try {
            return StringScenario.loadScenarios(filename, removeVars);
        } catch (FileNotFoundException e) {
            System.err.println("Can't open file " + filename);
            throw e;
        } catch (ParseException e) {
            System.err.println("Can't read scenarios from file " + filename);
            throw e;
        }
    }

    protected void saveScenarioTree(Object tree, String treeFilePath) {
        if (treeFilePath != null) {
            try (PrintWriter pw = new PrintWriter(new File(treeFilePath))) {
                pw.println(tree);
                logger().info("Scenario tree saved to " + treeFilePath);
            } catch (FileNotFoundException e) {
                logger().warning("Can't save scenario tree to " + treeFilePath);
                e.printStackTrace();
            }
        }
    }

    protected MealyAutomaton loadAutomaton(String filename) throws IOException, ParseException {
        try {
            return AutomatonGVLoader.load(filename);
        } catch (IOException e) {
            System.err.println("Can't open file " + filename);
            e.printStackTrace();
            throw e;
        } catch (ParseException e) {
            System.err.println("Can't read EFSM from file " + filename);
            e.printStackTrace();
            throw e;
        }
    }

    protected void saveToFile(Object object, String filename) {
        if (filename != null) {
            try (PrintWriter pw = new PrintWriter(new File(filename))) {
                pw.println(object);
            } catch (FileNotFoundException e) {
                logger().warning("File " + filename + " not found: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    protected List<String> eventNames(String names, int number) {
        final List<String> eventNames;
        if (names != null) {
            eventNames = Arrays.asList(names.split(","));
        } else {
            eventNames = new ArrayList<>();
            for (int i = 0; i < number; i++) {
                eventNames.add(String.valueOf((char) ('A' +  i)));
            }
        }
        return eventNames;
    }

    protected void registerVariableNames(String names, int number) {
        final List<String> varNames;
        if (names != null) {
            varNames = Arrays.asList(names.split(","));
        } else {
            varNames = new ArrayList<>();
            for (int i = 0; i < number; i++) {
                varNames.add("x" + i);
            }
        }
        MyBooleanExpression.registerVariableNames(varNames);
    }

    protected List<String> events(List<String> eventNames, int eventNumber, int varNumber) {
        final List<String> events = new ArrayList<>();
        for (int i = 0; i < eventNumber; i++) {
            final String event = eventNames.get(i);
            for (int j = 0; j < 1 << varNumber; j++) {
                final StringBuilder sb = new StringBuilder(event);
                for (int pos = 0; pos < varNumber; pos++) {
                    sb.append(((j >> pos) & 1) == 1 ? 1 : 0);
                }
                events.add(sb.toString());
            }
        }
        return events;
    }

    protected List<String> actions(String actionNames, int actionNumber) {
        final List<String> actions;
        if (actionNames != null) {
            actions = actionNames.isEmpty() ? Collections.emptyList()
                    : Arrays.asList(actionNames.split(","));
        } else {
            actions = new ArrayList<>();
            for (int i = 0; i < actionNumber; i++) {
                actions.add("z" + i);
            }
        }
        return actions;
    }
}

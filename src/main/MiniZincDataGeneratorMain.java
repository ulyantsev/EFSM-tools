package main;
import algorithms.AdjacencyCalculator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import structures.Node;
import structures.ScenarioTree;
import structures.Transition;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by ulyantsev on 08.05.14.
 *
 */
public class MiniZincDataGeneratorMain {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<String>();

    @Option(name = "--size", aliases = {"-s"}, usage = "automaton size", metaVar = "<size>", required = true)
    private int size;

    @Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--output", aliases = {"-o"}, usage = "write result MiniZinc data file", metaVar = "<file>")
    private String resultFilePath = "data.mzn";

    @Option(name = "--model", aliases = {"-m"}, usage = "model to import", metaVar = "<file>")
    private String modelFP;

    private void launcher(String[] args) {
        Locale.setDefault(Locale.US);

        final CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("Part of MiniZinc-based extended finite state machine induction tool");
            System.out.println("Author: Vladimir Ulyantsev (ulyantsev@rain.ifmo.ru)\n");
            System.out.print("Usage: ");
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);
            return;
        }

        final Logger logger = Logger.getLogger("Logger");
        if (logFilePath != null) {
            try {
                FileHandler fh = new FileHandler(logFilePath, false);
                logger.addHandler(fh);
                SimpleFormatter formatter = new SimpleFormatter();
                fh.setFormatter(formatter);

                logger.setUseParentHandlers(false);
                System.out.println("Log redirected to " + logFilePath);
            } catch (Exception e) {
                System.err.println("Can't work with file " + logFilePath + ": " + e.getMessage());
                return;
            }
        }

        final ScenarioTree tree = new ScenarioTree();
        for (String filePath : arguments) {
            try {
                tree.load(filePath);
                logger.info("Loaded scenarios from " + filePath);
                logger.info("  Total scenarios tree size: " + tree.nodeCount());
            } catch (Exception e) {
                logger.warning("Can't load scenarios from file " + filePath);
                return;
            }
        }

        final Map<Node, Set<Node>> adjacent = AdjacencyCalculator.getAdjacent(tree);

        try (PrintWriter pw = new PrintWriter(resultFilePath)) {
            pw.println(getDataString(tree, adjacent));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getDataString(ScenarioTree tree, Map<Node, Set<Node>> adjacent) {
        Transition[] incomingTransition = new Transition[tree.nodeCount()];

        List<String> eventOrder = new ArrayList<>();
        List<String> eventExprOrder = new ArrayList<>();
        List<String> actionsOrder = new ArrayList<>();
        List<Integer> eventExprToEvent = new ArrayList<>();
        //List<Integer> eventExprVarsCount = new ArrayList<>();
        List<Integer> eventExprSatCount = new ArrayList<>();

        for (Node node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                if (!eventOrder.contains(t.event())) {
                    eventOrder.add(t.event());
                }

                String eventExpr = t.event() + "[" + t.expr().toString() + "]";
                if (!eventExprOrder.contains(eventExpr)) {
                    eventExprOrder.add(eventExpr);
                    eventExprToEvent.add(eventOrder.indexOf(t.event()) + 1);
                    int satCnt = t.expr().getSatisfiabilitySetsCount() *
                            (1 << (tree.variableCount() - t.expr().getVariablesCount()));
                    eventExprSatCount.add(satCnt);
                    //eventExprVarsCount.add(t.getExpr().getVariablesCount());
                }

                if (!actionsOrder.contains(t.actions().toString())) {
                    actionsOrder.add(t.actions().toString());
                }

                incomingTransition[t.dst().number()] = t;
            }
        }


        int adjacentPairs = 0;
        for (Set<Node> set : adjacent.values()) {
            adjacentPairs += set.size();
        }

        int[] events = new int[tree.nodeCount() - 1], actions = new int[tree.nodeCount() - 1],
                parents = new int[tree.nodeCount() - 1];
        for (int nodeNum = 1; nodeNum < tree.nodeCount(); nodeNum++) {
            Transition t = incomingTransition[nodeNum];

            String eventExpr = t.event() + "[" + t.expr().toString() + "]";
            events[nodeNum - 1] = eventExprOrder.indexOf(eventExpr) + 1;
            actions[nodeNum - 1] = actionsOrder.indexOf(t.actions().toString());
            parents[nodeNum - 1] = t.src().number();
        }

        int[] edgeSrc = new int[adjacentPairs], edgeDst = new int[adjacentPairs];
        int pos = 0;
        for (Node src : adjacent.keySet()) {
            for (Node dst : adjacent.get(src)) {
                edgeSrc[pos] = src.number();
                edgeDst[pos] = dst.number();
                pos++;
            }
        }

        StringBuilder sb = new StringBuilder();

        if (modelFP != null) {
            sb.append("include \"");
            sb.append(modelFP);
            sb.append("\";\n\n");
        }

        sb.append(String.format("C = %d;\n" +
                "V = %d;\n" +
                "E = %d;\n" +
                "TE = %d;\n" +
                "A = %d;\n" +
                "AE = %d;\n" +
                "TSC = %d;\n",
                size, tree.nodeCount(), eventExprOrder.size(),
                eventOrder.size(), actionsOrder.size(), adjacentPairs, 1 << tree.variableCount()));

        sb.append("tree_event = ").append(Arrays.toString(events)).append(";\n");
        sb.append("tree_action = ").append(Arrays.toString(actions)).append(";\n");
        sb.append("tree_parent = ").append(Arrays.toString(parents)).append(";\n");

        sb.append("edge_src = ").append(Arrays.toString(edgeSrc)).append(";\n");
        sb.append("edge_dst = ").append(Arrays.toString(edgeDst)).append(";\n");

        String[] eventsArray = eventExprOrder.toArray(new String[eventExprOrder.size()]);
        for (int i = 0; i < eventsArray.length; i++) {
            eventsArray[i] = "\"" + eventsArray[i] + "\"";
        }
        String[] actionsArray = actionsOrder.toArray(new String[actionsOrder.size()]);
        for (int i = 0; i < actionsArray.length; i++) {
            actionsArray[i] = "\"" + actionsArray[i] + "\"";
        }

        sb.append("event_str = ").append(Arrays.toString(eventsArray)).append(";\n");
        sb.append("ee_event = ").append(Arrays.toString(eventExprToEvent.toArray())).append(";\n");
        sb.append("ee_sat_sets = ").append(Arrays.toString(eventExprSatCount.toArray())).append(";\n");
        sb.append("action_str = ").append(Arrays.toString(actionsArray)).append(";\n");

        return sb.toString();
    }

    public void run(String[] args) {
        try {
            launcher(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        new MiniZincDataGeneratorMain().run(args);
    }

}

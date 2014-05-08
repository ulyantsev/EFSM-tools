import algorithms.AdjacentCalculator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import structures.Node;
import structures.ScenariosTree;
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

    @Option(name = "--result", aliases = {"-r"}, usage = "write result MiniZinc data file", metaVar = "<file>")
    private String resultFilePath;

    private void launcher(String[] args) {
        Locale.setDefault(Locale.US);

        CmdLineParser parser = new CmdLineParser(this);
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

        Logger logger = Logger.getLogger("Logger");
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

        ScenariosTree tree = new ScenariosTree();
        for (String filePath : arguments) {
            try {
                tree.load(filePath);
                logger.info("Loaded scenarios from " + filePath);
                logger.info("  Total scenarios tree size: " + tree.nodesCount());
            } catch (Exception e) {
                logger.warning("Can't load scenarios from file " + filePath);
                return;
            }
        }

        Map<Node, Set<Node>> adjacent = AdjacentCalculator.getAdjacent(tree);

        if (resultFilePath == null) {
            resultFilePath = "data.mzn";
        }

        try {
            PrintWriter pw = new PrintWriter(resultFilePath);
            pw.println(getDataString(tree, adjacent));
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getDataString(ScenariosTree tree, Map<Node, Set<Node>> adjacent) {
        Transition[] incomingTransition = new Transition[tree.nodesCount()];

        List<String> eventExprOrder = new ArrayList<String>();
        List<String> actionsOrder = new ArrayList<String>();
        for (Node node : tree.getNodes()) {
            for (Transition t : node.getTransitions()) {
                String eventExpr = t.getEvent() + "[" + t.getExpr().toString() + "]";
                if (!eventExprOrder.contains(eventExpr)) {
                    eventExprOrder.add(eventExpr);
                }

                if (!actionsOrder.contains(t.getActions().toString())) {
                    actionsOrder.add(t.getActions().toString());
                }

                incomingTransition[t.getDst().getNumber()] = t;
            }
        }
        Collections.sort(eventExprOrder);
        Collections.sort(actionsOrder);

        int adjacentPairs = 0;
        for (Set<Node> set : adjacent.values()) {
            adjacentPairs += set.size();
        }


        int[] events = new int[tree.nodesCount() - 1], actions = new int[tree.nodesCount() - 1],
                parents = new int[tree.nodesCount() - 1];
        for (int nodeNum = 1; nodeNum < tree.nodesCount(); nodeNum++) {
            Transition t = incomingTransition[nodeNum];

            String eventExpr = t.getEvent() + "[" + t.getExpr().toString() + "]";
            events[nodeNum - 1] = eventExprOrder.indexOf(eventExpr);
            actions[nodeNum - 1] = actionsOrder.indexOf(t.getActions().toString());
            parents[nodeNum - 1] = t.getSrc().getNumber();
        }

        int[] edgeSrc = new int[adjacentPairs], edgeDst = new int[adjacentPairs];
        int pos = 0;
        for (Node src : adjacent.keySet()) {
            for (Node dst : adjacent.get(src)) {
                edgeSrc[pos] = src.getNumber();
                edgeDst[pos] = dst.getNumber();
                pos++;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("include \"exact_EFSM.mzn.model\";\n\n");
        sb.append(String.format("C = %d;\nV = %d;\nE = %d;\nA = %d;\nAE = %d;\n",
                size, tree.nodesCount(), eventExprOrder.size(), actionsOrder.size(), adjacentPairs));

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

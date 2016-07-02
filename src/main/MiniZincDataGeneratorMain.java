package main;
import algorithms.AdjacencyCalculator;
import meta.Author;
import meta.MainBase;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import structures.mealy.MealyNode;
import structures.mealy.ScenarioTree;
import structures.mealy.MealyTransition;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by ulyantsev on 08.05.14.
 *
 */
public class MiniZincDataGeneratorMain extends MainBase {
    @Argument(usage = "paths to files with scenarios", metaVar = "files", required = true)
    private List<String> arguments = new ArrayList<>();

    @Option(name = "--size", aliases = {"-s"}, usage = "automaton size", metaVar = "<size>", required = true)
    private int size;

    @Option(name = "--log", aliases = {"-l"}, usage = "write log to this file", metaVar = "<file>")
    private String logFilePath;

    @Option(name = "--output", aliases = {"-o"}, usage = "write result MiniZinc data file", metaVar = "<file>")
    private String resultFilePath = "data.mzn";

    @Option(name = "--model", aliases = {"-m"}, usage = "model to import", metaVar = "<file>")
    private String modelFP;

    public static void main(String[] args) {
        new MiniZincDataGeneratorMain().run(args, Author.VU,
                "Part of MiniZinc-based extended finite state machine induction tool");
    }

    @Override
    protected void launcher() throws IOException, ParseException {
        initializeLogger(logFilePath);
        final ScenarioTree tree = loadScenarioTree(arguments, -1);
        final Map<MealyNode, Set<MealyNode>> adjacent = AdjacencyCalculator.getAdjacent(tree);
        saveToFile(getDataString(tree, adjacent), resultFilePath);
    }

    private String getDataString(ScenarioTree tree, Map<MealyNode, Set<MealyNode>> adjacent) {
        final MealyTransition[] incomingTransition = new MealyTransition[tree.nodeCount()];

        final List<String> eventOrder = new ArrayList<>();
        final List<String> eventExprOrder = new ArrayList<>();
        final List<String> actionsOrder = new ArrayList<>();
        final List<Integer> eventExprToEvent = new ArrayList<>();
        //List<Integer> eventExprVarsCount = new ArrayList<>();
        final List<Integer> eventExprSatCount = new ArrayList<>();

        for (MealyNode node : tree.nodes()) {
            for (MealyTransition t : node.transitions()) {
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
        for (Set<MealyNode> set : adjacent.values()) {
            adjacentPairs += set.size();
        }

        final int[] events = new int[tree.nodeCount() - 1], actions = new int[tree.nodeCount() - 1],
                parents = new int[tree.nodeCount() - 1];
        for (int nodeNum = 1; nodeNum < tree.nodeCount(); nodeNum++) {
            MealyTransition t = incomingTransition[nodeNum];

            String eventExpr = t.event() + "[" + t.expr().toString() + "]";
            events[nodeNum - 1] = eventExprOrder.indexOf(eventExpr) + 1;
            actions[nodeNum - 1] = actionsOrder.indexOf(t.actions().toString());
            parents[nodeNum - 1] = t.src().number();
        }

        final int[] edgeSrc = new int[adjacentPairs], edgeDst = new int[adjacentPairs];
        int pos = 0;
        for (MealyNode src : adjacent.keySet()) {
            for (MealyNode dst : adjacent.get(src)) {
                edgeSrc[pos] = src.number();
                edgeDst[pos] = dst.number();
                pos++;
            }
        }

        final StringBuilder sb = new StringBuilder();

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
}

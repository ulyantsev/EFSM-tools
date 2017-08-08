package structures.mealy;

/**
 * (c) Igor Buzhinsky
 */

import bool.MyBooleanExpression;
import scenario.StringActions;
import scenario.StringScenario;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NegativeScenarioTree {
    private final NegativeMealyNode root;
    private final Set<NegativeMealyNode> nodes;

    public NegativeScenarioTree() {
        root = new NegativeMealyNode(0);
        nodes = new LinkedHashSet<>();
        nodes.add(root);
    }

    public NegativeMealyNode getRoot() {
        return root;
    }

    public void load(String filepath, boolean removeVars) throws FileNotFoundException, ParseException {
        for (StringScenario scenario : StringScenario.loadScenarios(filepath, removeVars)) {
            addScenario(scenario, 0);
        }
    }
    
    public void addScenario(StringScenario scenario, int loopLength) throws ParseException {
        NegativeMealyNode loopNode = null;
        NegativeMealyNode node = root;
        for (int i = 0; i < scenario.size(); i++) {
            if (i == scenario.size() - loopLength) {
                loopNode = node;
            }
            addTransitions(node, scenario.getEvents(i), scenario.getExpr(i), scenario.getActions(i));
            node = node.dst(scenario.getEvents(i).get(0), scenario.getExpr(i), scenario.getActions(i));
        }
        if (loopLength == 0) {
            loopNode = node;
        }
        if (loopNode == null) {
            throw new AssertionError("loopNode is null!");
        }
        if (node.loops().contains(loopNode)) {
            throw new AssertionError("Duplicate counterexample!");
        }
        node.addLoop(loopNode);
    }

    /*
     * If events.size() > 1, will add multiple edges towards the same destination.
     */
    private void addTransitions(NegativeMealyNode src, List<String> events, MyBooleanExpression expr,
                                StringActions actions) throws ParseException {
        assert !events.isEmpty();
        NegativeMealyNode dst = null;
        for (String e : events) {
            if (src.dst(e, expr, actions) == null) {
                if (dst == null) {
                    dst = new NegativeMealyNode(nodes.size());
                    nodes.add(dst);
                }
                src.addTransition(e, expr, actions, dst);
            }
        }
    }

    public Collection<NegativeMealyNode> nodes() {
        return nodes;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# generated file, don't try to modify\n");
        sb.append("# command: dot -Tpng <filename> > tree.png\n");
        sb.append("digraph ScenariosTree {\n    node [shape = circle];\n");

        for (NegativeMealyNode node : nodes) {
            for (MealyTransition t : node.transitions()) {
                sb.append("    ").append(t.src().number()).append(" -> ").append(t.dst().number());
                sb.append(" [label = \"").append(t.event()).append(" [").append(t.expr().toString()).append("] (")
                        .append(t.actions().toString()).append(") \"];\n");
            }
            if (node.weakInvalid()) {
                for (NegativeMealyNode loop : node.loops()) {
                    sb.append("    ").append(node.number()).append(" -> ").append(loop.number()).append(";\n");
                }
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

package structures;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import scenario.StringScenario;
import actions.StringActions;
import bool.MyBooleanExpression;

public class NegativeScenariosTree {
    private final NegativeNode root;
    private final List<NegativeNode> nodes;

    public NegativeScenariosTree() {
        this.root = new NegativeNode(0);
        this.nodes = new ArrayList<>();
        this.nodes.add(root);
    }

    public NegativeNode getRoot() {
        return root;
    }

    public void addScenario(StringScenario scenario) throws ParseException {
    	NegativeNode node = root;
        for (int i = 0; i < scenario.size(); i++) {
            addTransitions(node, scenario.getEvents(i), scenario.getExpr(i), scenario.getActions(i));
            node = node.getDst(scenario.getEvents(i).get(0), scenario.getExpr(i), scenario.getActions(i));
        }
        node.setTerminal();
    }

    /*
     * If events.size() > 1, will add multiple edges towards the same destination.
     */
    private void addTransitions(NegativeNode src, List<String> events, MyBooleanExpression expr,
    		StringActions actions) throws ParseException {
    	assert !events.isEmpty();
    	NegativeNode dst = null;
    	for (String e : events) {
    		if (src.getDst(e, expr, actions) == null) {
    			if (dst == null) {
            		dst = new NegativeNode(nodes.size());
            		nodes.add(dst);
            	}
                src.addTransition(e, expr, actions, dst);
    		}
    	}
    }

    public List<NegativeNode> getNodes() {
        return nodes;
    }

    public int nodesCount() {
        return nodes.size();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# generated file, don't try to modify\n");
        sb.append("# command: dot -Tpng <filename> > tree.png\n");
        sb.append("digraph ScenariosTree {\n    node [shape = circle];\n");

        for (Node node : nodes) {
            for (Transition t : node.getTransitions()) {
                sb.append("    " + t.getSrc().getNumber() + " -> " + t.getDst().getNumber());
                sb.append(" [label = \"" + t.getEvent() + " [" + t.getExpr().toString() + "] ("
                        + t.getActions().toString() + ") \"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

package structures.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import scenario.StringActions;
import scenario.StringScenario;

public class NegativePlantScenarioForest extends PlantScenarioForest {
	
	private final Set<MooreNode> terminalNodes = new LinkedHashSet<>();
	
	/*
	 * Currently no loop support.
	 */
	
	@Override
	public void addScenario(StringScenario scenario, int loopLength) {
    	checkScenario(scenario);
    	final StringActions firstActions = scenario.getActions(0);
    	
    	MooreNode properRoot = null;
    	for (MooreNode root : roots) {
    		if (root.getActions().equals(firstActions)) {
    			properRoot = root;
    			break;
    		}
    	}
    	if (properRoot == null) {
    		properRoot = new MooreNode(nodes.size(), firstActions);
    		nodes.add(properRoot);
    		roots.add(properRoot);
    	}
    	
    	MooreNode node = properRoot;
        for (int i = 1; i < scenario.size(); i++) {
        	node = addTransition(node, scenario.getEvents(i).get(0), scenario.getActions(i));
        }
        terminalNodes.add(node);
    }

	public Collection<MooreNode> getTerminalNodes() {
        return Collections.unmodifiableSet(terminalNodes);
    }
	
	@Override
    protected MooreNode addTransition(MooreNode src, String event, StringActions actions) {
    	MooreNode dst = src.getScenarioDst(event, actions);
		if (dst == null) {
    		dst = new MooreNode(nodes.size(), actions);
    		nodes.add(dst);
            src.addTransition(event, dst);
		}
		return dst;
    }

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("# generated file\n");
        sb.append("# command: dot -Tpng <filename> > filename.png\n");
        sb.append("digraph ScenariosTree {\n    node [shape = circle];\n");

        for (MooreNode node : nodes) {
    		sb.append("    " + node.getNumber() + " [label = \"" + node + "\"];\n");
    	}
    	
        for (MooreNode node : nodes) {
            for (MooreTransition t : node.getTransitions()) {
                sb.append("    " + t.getSrc().getNumber() + " -> " + t.getDst().getNumber()
                		+ " [label = \"" + t.getEvent() + "\"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

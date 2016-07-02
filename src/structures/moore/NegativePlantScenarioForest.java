package structures.moore;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import scenario.StringActions;
import scenario.StringScenario;

public class NegativePlantScenarioForest extends PlantScenarioForest {
	private final Set<MooreNode> terminalNodes = new LinkedHashSet<>();

	private final Set<MooreNode> unprocessedRoots = new HashSet<>();
	private final Set<MooreNode> unprocessedTerminalNodes = new HashSet<>();
	private final Set<MooreNode> unprocessedChildren = new HashSet<>();
	
	public boolean processRoot(MooreNode node) {
		return unprocessedRoots.remove(node);
	}
	
	public boolean processTerminalNode(MooreNode node) {
		return unprocessedTerminalNodes.remove(node);
	}
	
	public boolean processChild(MooreNode node) {
		return unprocessedChildren.remove(node);
	}
	
	@Override
	public void addScenario(StringScenario scenario) {
    	checkScenario(scenario);
    	final StringActions firstActions = scenario.getActions(0);
    	
    	MooreNode properRoot = null;
    	for (MooreNode root : roots) {
    		if (root.actions().equals(firstActions)) {
    			properRoot = root;
    			break;
    		}
    	}
    	if (properRoot == null) {
    		properRoot = new MooreNode(nodes.size(), firstActions);
    		nodes.add(properRoot);
    		roots.add(properRoot);
    		unprocessedRoots.add(properRoot);
    	}
    	
    	MooreNode node = properRoot;
        for (int i = 1; i < scenario.size(); i++) {
        	final String event = scenario.getEvents(i).get(0);
        	node = addTransition(node, event, scenario.getActions(i));
        }
        
        if (!terminalNodes.add(node)) {
        	throw new AssertionError("Duplicate counterexample!");
        }
        unprocessedTerminalNodes.add(node);
    }

	public Collection<MooreNode> terminalNodes() {
        return Collections.unmodifiableSet(terminalNodes);
    }
	
	@Override
    protected MooreNode addTransition(MooreNode src, String event, StringActions actions) {
    	MooreNode dst = src.scenarioDst(event, actions);
		if (dst == null) {
    		dst = new MooreNode(nodes.size(), actions);
    		nodes.add(dst);
    		unprocessedChildren.add(dst);
            src.addTransition(event, dst);
		}
		return dst;
    }

	@Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("# generated file; view: dot -Tpng <filename> > filename.png\n");
        sb.append("digraph ScenarioForest {\n    node [shape = circle];\n");

        for (MooreNode node : nodes) {
    		sb.append("    " + node.number() + " [label = \"" + node + "\"];\n");
    	}
    	
        for (MooreNode node : nodes) {
            for (MooreTransition t : node.transitions()) {
                sb.append("    " + t.src().number() + " -> " + t.dst().number()
                		+ " [label = \"" + t.event() + "\"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

package structures.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import scenario.StringActions;
import scenario.StringScenario;

public class NegativePlantScenarioForest extends PlantScenarioForest {
	private final Set<MooreNode> terminalNodes = new LinkedHashSet<>();
	private final List<Loop> loops = new ArrayList<>();
	
	public static class Loop {
		public final MooreNode source;
		public final int index;
		public final List<MooreNode> nodes = new ArrayList<>();
		public final List<String> events = new ArrayList<>();
		
		public Loop(MooreNode source, int index) {
			this.source = source;
			this.index = index;
		}
		
		public int length() {
			return nodes.size();
		}
		
		void add(String event, MooreNode node) {
			events.add(event);
			nodes.add(node);
		}
		
		@Override
		public String toString() {
			return "[" + source + "]" + nodes;
		}
	}
	
	@Override
	public void addScenario(StringScenario scenario, int loopLength) {
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
    	}
    	
    	MooreNode node = properRoot;
		final int loopStart = scenario.size() - loopLength - 1;
    	Loop loop = loopLength > 0 && loopStart == 0 ? new Loop(node, loops.size()) : null;
        for (int i = 1; i < scenario.size(); i++) {
        	final String event = scenario.getEvents(i).get(0);
        	node = addTransition(node, event, scenario.getActions(i));
        	if (loopLength > 0) {
        		if (i == loopStart) {
        			loop = new Loop(node, loops.size());
        		} else if (i > loopStart) {
        			loop.add(event, node);
        		}
        	}
        }
        
        if (loopLength == 0) {
        	terminalNodes.add(node);
        } else {
        	loops.add(loop);
        }
    }

	public Collection<MooreNode> terminalNodes() {
        return Collections.unmodifiableSet(terminalNodes);
    }
	
	public Collection<Loop> loops() {
        return Collections.unmodifiableList(loops);
    }
	
	@Override
    protected MooreNode addTransition(MooreNode src, String event, StringActions actions) {
    	MooreNode dst = src.scenarioDst(event, actions);
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

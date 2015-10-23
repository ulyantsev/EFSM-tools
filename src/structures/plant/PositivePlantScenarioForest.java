package structures.plant;

/**
 * (c) Igor Buzhinsky
 */

import scenario.StringActions;
import scenario.StringScenario;

public class PositivePlantScenarioForest extends PlantScenarioForest {
    
	@Override
	public void addScenario(StringScenario scenario, int loopLength) {
    	checkScenario(scenario);
    	final StringActions firstActions = scenario.getActions(0);
    	MooreNode node = new MooreNode(nodes.size(), firstActions);
    	roots.add(node);
    	nodes.add(node);
        for (int i = 1; i < scenario.size(); i++) {
        	node = addTransition(node, scenario.getEvents(i).get(0), scenario.getActions(i));
        }
    }

	@Override
    protected MooreNode addTransition(MooreNode src, String event, StringActions actions) {
    	final MooreNode dst = new MooreNode(nodes.size(), actions);
		nodes.add(dst);
        src.addTransition(event, dst);
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

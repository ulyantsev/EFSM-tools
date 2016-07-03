package structures.moore;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import scenario.StringActions;
import scenario.StringScenario;

public abstract class PlantScenarioForest {
	protected final Set<MooreNode> roots = new LinkedHashSet<>();
	protected final Set<MooreNode> nodes = new LinkedHashSet<>();

    public Collection<MooreNode> roots() {
        return Collections.unmodifiableSet(roots);
    }
    
    public Collection<MooreNode> nodes() {
        return Collections.unmodifiableSet(nodes);
    }

    public int rootCount() {
        return roots.size();
    }
    
    public int nodeCount() {
        return nodes.size();
    }
    
    /*
     * varNumber = -1 for no variable removal
     */
    public void load(String filepath, int varNumber) throws FileNotFoundException, ParseException {
        StringScenario.loadScenarios(filepath, varNumber).forEach(this::addScenario);
    }
    
    protected abstract void addScenario(StringScenario scenario);
    protected abstract MooreNode addTransition(MooreNode src, String event, StringActions actions);

    protected MooreNode addScenarioFrom(MooreNode node, StringScenario scenario) {
        for (int i = 1; i < scenario.size(); i++) {
            node = addTransition(node, scenario.getEvents(i).get(0), scenario.getActions(i));
        }
        return node;
    }

    protected MooreNode properRoot(StringActions actions) {
        MooreNode properRoot = null;
        for (MooreNode root : roots) {
            if (root.actions().equals(actions)) {
                properRoot = root;
                break;
            }
        }
        return properRoot;
    }

    protected void checkScenario(StringScenario scenario) {
    	for (int i = 0; i < scenario.size(); i++) {
        	if (scenario.getEvents(i).size() != 1) {
        		throw new RuntimeException("Multi-edges are not supported!");
        	}
    	}
    	
    	if (!scenario.getEvents(0).get(0).isEmpty()) {
    		throw new RuntimeException("The first event must be dummy (i.e. empty string)!");
    	}
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

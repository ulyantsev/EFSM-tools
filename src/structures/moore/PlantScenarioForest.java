package structures.moore;

import scenario.StringActions;
import scenario.StringScenario;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

    public void load(String filepath, boolean removeVars) throws IOException, ParseException {
        StringScenario.loadScenarios(filepath, removeVars).forEach(this::addScenario);
    }
    
    protected abstract void addScenario(StringScenario scenario);
    protected abstract MooreNode addTransition(MooreNode src, String event, StringActions actions);

    MooreNode addScenarioFrom(MooreNode node, StringScenario scenario) {
        for (int i = 1; i < scenario.size(); i++) {
            node = addTransition(node, scenario.getEvents(i).get(0), scenario.getActions(i));
        }
        return node;
    }

    MooreNode properRoot(StringActions actions) {
        MooreNode properRoot = null;
        for (MooreNode root : roots) {
            if (root.actions().equals(actions)) {
                properRoot = root;
                break;
            }
        }
        return properRoot;
    }

    void checkScenario(StringScenario scenario) {
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
            sb.append("    ").append(node.number()).append(" [label = \"").append(node).append("\"];\n");
        }

        for (MooreNode node : nodes) {
            for (MooreTransition t : node.transitions()) {
                sb.append("    ").append(t.src().number()).append(" -> ").append(t.dst().number())
                        .append(" [label = \"").append(t.event()).append("\"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

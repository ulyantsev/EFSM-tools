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

    public Collection<MooreNode> terminalNodes() {
        return Collections.unmodifiableSet(terminalNodes);
    }

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
        MooreNode properRoot = properRoot(firstActions);
        if (properRoot == null) {
            properRoot = new MooreNode(nodes.size(), firstActions);
            nodes.add(properRoot);
            roots.add(properRoot);
            unprocessedRoots.add(properRoot);
        }
        
        final MooreNode end = addScenarioFrom(properRoot, scenario);
        if (!terminalNodes.add(end)) {
            throw new AssertionError("Duplicate counterexample!");
        }
        unprocessedTerminalNodes.add(end);
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
}

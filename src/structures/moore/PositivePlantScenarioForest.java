package structures.moore;

/**
 * (c) Igor Buzhinsky
 */

import scenario.StringActions;
import scenario.StringScenario;

public class PositivePlantScenarioForest extends PlantScenarioForest {
    private final boolean separatePaths;

    public PositivePlantScenarioForest(boolean separatePaths) {
        this.separatePaths = separatePaths;
    }

    @Override
    public void addScenario(StringScenario scenario) {
        checkScenario(scenario);
        final StringActions firstActions = scenario.getActions(0);
        MooreNode properRoot = separatePaths ? null : properRoot(firstActions);
        if (properRoot == null) {
            properRoot = new MooreNode(nodes.size(), firstActions);
            nodes.add(properRoot);
            roots.add(properRoot);
        }
        addScenarioFrom(properRoot, scenario);
    }

    @Override
    protected MooreNode addTransition(MooreNode src, String event, StringActions actions) {
        MooreNode dst = separatePaths ? null : src.scenarioDst(event, actions);
        if (dst == null) {
            dst = new MooreNode(nodes.size(), actions);
            nodes.add(dst);
            src.addTransition(event, dst);
        }
        return dst;
    }
}

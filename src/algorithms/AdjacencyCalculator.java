package algorithms;

import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;
import structures.mealy.ScenarioTree;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class AdjacencyCalculator {
    public static Map<MealyNode, Set<MealyNode>> getAdjacent(ScenarioTree tree) {
        final Map<MealyNode, Set<MealyNode>> ans = new HashMap<>();
        calcNode(tree, tree.root(), ans);
        return ans;
    }
    
    private static void calcNode(ScenarioTree tree, MealyNode node, Map<MealyNode, Set<MealyNode>> ans) {
        if (!ans.containsKey(node)) {
            Set<MealyNode> adjacentSet = new LinkedHashSet<>();
            ans.put(node, adjacentSet);
            if (node.transitionCount() == 0) {
                return;
            }

            for (MealyTransition t1 : node.transitions()) {
                // calculating for children
                calcNode(tree, t1.dst(), ans);
                tree.nodes().stream().filter(other -> other != node).forEach(other -> other.transitions().stream()
                        .filter(t2 -> t1.event().equals(t2.event())).forEach(t2 -> {
                    if (t1.expr() == t2.expr()) {
                        if (!t1.actions().equals(t2.actions())
                                || ans.get(t1.dst()).contains(t2.dst())) {
                            adjacentSet.add(other);
                        }
                    } else if (t1.expr().hasSolutionWith(t2.expr())) {
                        adjacentSet.add(other);
                    }
                }));
            }
        }
    }

}

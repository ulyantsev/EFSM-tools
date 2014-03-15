package algorithms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import structures.Node;
import structures.ScenariosTree;
import structures.Transition;

public class AdjacentCalculator {
    
    public static Map<Node, Set<Node>> getAdjacent(ScenariosTree tree) {
        Map<Node, Set<Node>> ans = new HashMap<Node, Set<Node>>();
        calcNode(tree, tree.getRoot(), ans);
        return ans;
    }
    
    private static void calcNode(ScenariosTree tree, Node node, Map<Node, Set<Node>> ans) {
        if (!ans.containsKey(node)) {
            HashSet<Node> adjacentSet = new HashSet<Node>();
            ans.put(node, adjacentSet);
            if (node.transitionsCount() == 0) {
                return;
            }

            for (Transition t1 : node.getTransitions()) {
                // считаем для детей
                calcNode(tree, t1.getDst(), ans);
                for (Node other : tree.getNodes()) {
                    if (other != node) {
                        for (Transition t2 : other.getTransitions()) {
                            if (t1.getEvent().equals(t2.getEvent())) {
                                if (t1.getExpr() == t2.getExpr()) {
                                    if (!t1.getActions().equals(t2.getActions())
                                            || ans.get(t1.getDst()).contains(t2.getDst())) {
                                        adjacentSet.add(other);
                                    }
                                } else if (t1.getExpr().hasSolutionWith(t2.getExpr())) {
                                    adjacentSet.add(other);
                                } else {

                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

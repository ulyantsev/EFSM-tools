package algorithms.formula_builders;

import java.util.*;

import algorithms.AdjacencyCalculator;
import structures.MealyNode;
import structures.ScenarioTree;
import structures.Transition;

public class DimacsCnfBuilder {
    public static String getCnf(ScenarioTree tree, int k) {
        Map<String, Integer> vars = new HashMap<>();
        for (MealyNode node : tree.nodes()) {
            for (int color = 0; color < k; color++) {
                vars.put("x_" + node.number() + "_" + color, vars.size() + 1);
            }
        }
        
        for (MealyNode node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                String key = "y_" + t.event() + "_" + t.expr().toString() + "_0_0";
                if (!vars.containsKey(key)) {
                    for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                        for (int childColor = 0; childColor < k; childColor++) {
                            String s = "y_" + t.event() + "_" + t.expr() + "_" + nodeColor + "_" + childColor;
                            vars.put(s, vars.size() + 1);
                        }
                    }
                }
            }
        }
        
        List<String> clauses = new ArrayList<>();
        String initClause = vars.get("x_0_0") + "";
        clauses.add(initClause);
        
        for (MealyNode node : tree.nodes()) {
            String clause = "";
            for (int color = 0; color < k; color++) {
                clause += vars.get("x_" + node.number() + "_" + color) + " "; 
            }
            clauses.add(clause);
        }
        
        for (MealyNode node : tree.nodes()) {
            for (int c1 = 0; c1 < k; c1++) {
                for (int c2 = 0; c2 < c1; c2++) {
                    int v1 = vars.get("x_" + node.number() + "_" + c1);
                    int v2 = vars.get("x_" + node.number() + "_" + c2);
                    clauses.add(-v1 + " " + -v2);
                }
            }
        }
        
        Map<MealyNode, Set<MealyNode>> adjacent = AdjacencyCalculator.getAdjacent(tree);
        for (MealyNode node : tree.nodes()) {
            for (MealyNode other : adjacent.get(node)) {
                if (other.number() < node.number()) {
                    for (int color = 0; color < k; color++) {
                        int v1 = vars.get("x_" + node.number() + "_" + color);
                        int v2 = vars.get("x_" + other.number() + "_" + color);
                        clauses.add("-" + v1 + " -" + v2);                      
                    }
                }
            }
        }
        
        Set<String> was = new HashSet<>();
        for (MealyNode node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                String key = t.event() + "_" + t.expr();
                if (!was.contains(key)) {
                    was.add(key);
                    for (int parentColor = 0; parentColor < k; parentColor++) {
                        for (int c1 = 0; c1 < k; c1++) {
                            for (int c2 = 0; c2 < c1; c2++) {
                                int v1 = vars.get("y_" + key + "_" + parentColor + "_" + c1);
                                int v2 = vars.get("y_" + key + "_" + parentColor + "_" + c2);
                                clauses.add(-v1 + " " + -v2);
                            }
                        }
                    }
                }
            }
        }
        
        for (MealyNode node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                    for (int childColor = 0; childColor < k; childColor++) {
                        int nodeVar = vars.get("x_" + node.number() + "_" + nodeColor);
                        int childVar = vars.get("x_" + t.dst().number() + "_" + childColor);
                        int relationVar = vars.get("y_" + t.event() + "_" + t.expr()
                                + "_" + nodeColor + "_" + childColor);
                        
                        clauses.add(relationVar + " " + -nodeVar + " " + -childVar);
                        clauses.add(-relationVar + " " + -nodeVar + " " + childVar);
                    }
                }
            }
        }

        // BFS order clauses
        {
            List<String> eventExprOrder = new ArrayList<String>();
            for (MealyNode node : tree.nodes()) {
                for (Transition t : node.transitions()) {
                    String eventExpr = t.event() + "_" + t.expr().toString();
                    if (!eventExprOrder.contains(eventExpr)) {
                         eventExprOrder.add(eventExpr);
                    }
                }
            }
            Collections.sort(eventExprOrder);
        
            for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                for (int childColor = nodeColor + 1; childColor < k; childColor++) {
                    vars.put("e_" + nodeColor + "_" + childColor, vars.size() + 1);
                }
            }

            // e_a_b <=> y_a_b_ee1 \/ ... \/ y_a_b_ee2
            for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                for (int childColor = nodeColor + 1; childColor < k; childColor++) {
                    int edgeVar = vars.get("e_" + nodeColor + "_" + childColor);
                    String edgeThenRelation = -edgeVar + " ";
                    for (String eventExpr : eventExprOrder) {
                        int relationVar = vars.get("y_" + eventExpr + "_" + nodeColor + "_" + childColor);
                        clauses.add(-relationVar + " " + edgeVar);

                        edgeThenRelation += relationVar + " ";
                    }
                    clauses.add(edgeThenRelation);
                }
            }

            for (int nodeColor = 1; nodeColor < k; nodeColor++) {
                for (int parentColor = 0; parentColor < nodeColor; parentColor++) {
                    vars.put("p_" + nodeColor + "_" + parentColor, vars.size() + 1);
                }
            }

            // p_a_1 \/ ... \/ p_a_{a-1}
            for (int nodeColor = 1; nodeColor < k; nodeColor++) {
                String hasParentClause = "";
                for (int parentColor = 0; parentColor < nodeColor; parentColor++) {
                    hasParentClause += vars.get("p_" + nodeColor + "_" + parentColor) + " ";
                }
                clauses.add(hasParentClause);
            }

            // p_a_b <=> e_b_a /\ ~e_{b-1}_a /\ ... /\ ~e_0_a
            for (int nodeColor = 1; nodeColor < k; nodeColor++) {
                for (int parentColor = 0; parentColor < nodeColor; parentColor++) {
                    int parentVar = vars.get("p_" + nodeColor + "_" + parentColor);
                    int edgeVar = vars.get("e_" + parentColor + "_" + nodeColor);
                    clauses.add(-parentVar + " " + edgeVar);
                                                                                
                    String edgesThenParent = -edgeVar + " ";
                    for (int otherParent = 0; otherParent < parentColor; otherParent++) {
                        int otherEdgeVar = vars.get("e_" + otherParent + "_" + nodeColor);
                        clauses.add(-parentVar + " " + -otherEdgeVar);

                        edgesThenParent += otherEdgeVar + " ";
                    }
                    edgesThenParent += parentVar + "";
                    clauses.add(edgesThenParent);
                }
            }
            
            for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                for (int childColor = nodeColor + 1; childColor < k; childColor++) {
                    for (String eventExpr : eventExprOrder) {
                        vars.put("m_" + eventExpr + "_" + nodeColor + "_" + childColor, vars.size() + 1);
                    }
                }
            }
            
            // m_a_b_ee <=> e_a_b /\ y_a_b_ee /\ ~y_a_b_{ee-1} /\ ... /\ ~y_a_b_{ee0}
            for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                for (int childColor = nodeColor + 1; childColor < k; childColor++) {
                    int edgeVar = vars.get("e_" + nodeColor + "_" + childColor);

                    for (String eventExpr : eventExprOrder) {
                        int minTransition = vars.get("m_" + eventExpr + "_" + nodeColor + "_" + childColor);
                        int relationVar = vars.get("y_" + eventExpr + "_" + nodeColor + "_" + childColor);

                        clauses.add(-minTransition + " " + edgeVar);
                        clauses.add(-minTransition + " " + relationVar);

                        String transitionThenMin = -edgeVar + " " + -relationVar + " ";
                        for (String otherEventExpr : eventExprOrder) {
                            if (otherEventExpr == eventExpr) {
                                break;
                            }
                            int otherRelationVar = vars.get("y_" + otherEventExpr + "_" + nodeColor + "_" + childColor);
                            clauses.add(-minTransition + " " + -otherRelationVar);

                            transitionThenMin += otherRelationVar + " ";
                        }
                        transitionThenMin += minTransition + "";
                        clauses.add(transitionThenMin);
                    }
                }
            }

            // p_a_b /\ p_{a+1}_b /\ m_b_a_ee1 => ~m_b_{a+1}_ee2 (ee2 < ee1)
            for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                for (int childColor = nodeColor + 1; childColor < k - 1; childColor++) {
                    int parentVar = vars.get("p_" + childColor + "_" + nodeColor);
                    int nextParentVar = vars.get("p_" + (childColor + 1) + "_" + nodeColor);
                        
                    for (String eventExpr : eventExprOrder) {
                        for (String otherEventExpr : eventExprOrder) {
                            if (otherEventExpr == eventExpr) {
                                break;
                            }
                            int minTransition = vars.get("m_" + eventExpr + "_" + nodeColor + "_" + childColor);
                            int otherMin = vars.get("m_" + otherEventExpr + "_" + nodeColor + "_" + (childColor + 1));

                            clauses.add(-parentVar + " " + -nextParentVar + " " + -minTransition + " " + -otherMin);
                        }
                    }
                }
            }

            // p_a_b /\ ~p_{a+1}_b => ~p_c_b (c > a + 1)
            for (int nodeColor = 0; nodeColor < k; nodeColor++) {
                for (int childColor = nodeColor + 1; childColor < k - 1; childColor++) {
                    int parentVar = vars.get("p_" + childColor + "_" + nodeColor);
                    int nextParentVar = vars.get("p_" + (childColor + 1) + "_" + nodeColor);

                    for (int otherChild = childColor + 2; otherChild < k; otherChild++) {
                        int otherParentVar = vars.get("p_" + otherChild + "_" + nodeColor);
                        clauses.add(-parentVar + " " + nextParentVar + " " + -otherParentVar);
                    }
                }
            }

            // p_a_b => ~p_a+1_d (d < b)
            for (int nodeColor = 1; nodeColor < k - 1; nodeColor++) {
                for (int parentColor = 0; parentColor < nodeColor; parentColor++) {
                    for (int nextParent = 0; nextParent < parentColor; nextParent++) {
                        int parentVar = vars.get("p_" + nodeColor + "_" + parentColor);
                        int nextParentVar = vars.get("p_" + (nodeColor + 1) + "_" + nextParent);

                        clauses.add(-parentVar + " " + -nextParentVar);
                    }
                }
            }
        
        }

        StringBuilder sb = new StringBuilder();

        String header = "c CNF for scenarios tree with " + tree.nodeCount() + " nodes, colored in " + k + " colors\n";
        header += "p cnf " + vars.size() + " " + clauses.size() + "\n";
        sb.append(header);

        for (String s : clauses) {
            sb.append(s).append(" 0\n");
        }
        
        return sb.toString();
    }
}

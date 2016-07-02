package algorithms.automaton_builders;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import structures.Automaton;
import structures.Node;
import structures.ScenarioTree;
import structures.Transition;
import algorithms.AdjacencyCalculator;
import bool.MyBooleanExpression;
import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valiterator.IncreasingDomain;
import choco.cp.solver.search.integer.varselector.MinDomain;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;

public class ChocoAutomatonBuilder {

    public static Automaton build(ScenarioTree tree, int size, boolean isComplete) {
        return build(tree, size, isComplete, false, null);
    }

    public static Automaton build(ScenarioTree tree, int size, boolean isComplete, boolean isWeakComplete) {
        return build(tree, size, isComplete, isWeakComplete, null);
    }

    public static Automaton build(ScenarioTree tree, int size, boolean isComplete, boolean isWeakComplete,
                                  PrintWriter modelPrintWriter) {
        IntegerVariable[] nodesColorsVars = Choco.makeIntVarArray("Color", tree.nodeCount(), 0, size - 1);
        CPModel model = buildModel(tree, size, isComplete, isWeakComplete, nodesColorsVars, modelPrintWriter);
        return buildAutomatonFromModel(size, tree, nodesColorsVars, model);
    }

    public static List<Automaton> buildAll(ScenarioTree tree, int size, boolean isComplete, boolean isWeakComplete,
                                           PrintWriter modelPrintWriter) {
        IntegerVariable[] nodesColorsVars = Choco.makeIntVarArray("Color", tree.nodeCount(), 0, size - 1);
        CPModel model = buildModel(tree, size, isComplete, isWeakComplete, nodesColorsVars, modelPrintWriter);
        return buildAllAutomatonsFromModel(size, tree, nodesColorsVars, model);
    }


    private static CPModel buildModel(ScenarioTree tree, int size, boolean isComplete, boolean isWeakCompleteness,
                                      IntegerVariable[] nodesColorsVars, PrintWriter modelPrintWriter) {
        CPModel model = new CPModel();

        Constraint rootColorConstraint = Choco.eq(nodesColorsVars[0], 0);
        model.addConstraint(rootColorConstraint);

        model.addConstraints(getTransitionsConstraints(size, tree, nodesColorsVars));
        model.addConstraints(getAdjacentConstraints(tree, nodesColorsVars));

        if (isComplete) {
            model.addConstraints(getCompleteConstraints(model, size, tree, nodesColorsVars, isWeakCompleteness));
        }

        if (modelPrintWriter != null) {
            modelPrintWriter.println(model.varsToString());
            modelPrintWriter.println(model.constraintsToString());
            modelPrintWriter.flush();
        }

        return model;
    }

    private static Constraint[] getCompleteConstraints(CPModel model, int size, ScenarioTree tree,
                                                       IntegerVariable[] nodesColorsVars, boolean isWeakCompleteness) {

        Map<String, Map<String, List<Node>>> eventExprToNodes = new TreeMap<>();

        Map<String, List<MyBooleanExpression>> pairs = tree.pairsEventExpression();
        for (String event : pairs.keySet()) {
            Map<String, List<Node>> exprMap = new TreeMap<>();
            eventExprToNodes.put(event, exprMap);
            for (MyBooleanExpression expr : pairs.get(event)) {
                exprMap.put(expr.toString(), new ArrayList<>());
            }
        }

        for (Node node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                eventExprToNodes.get(t.event()).get(t.expr().toString()).add(node);
            }
        }

        /*
         * for (String event : pairs.keySet()) { for (MyBooleanExpression expr :
         * pairs.get(event)) { System.out.print(event + " " + expr.toString());
         * for (Node node : eventExprToNodes.get(event).get(expr.toString())) {
         * System.out.print(" " + node.getNumber()); } System.out.println(); } }
         */

        int varsCount = tree.variableCount();

        Map<String, Integer> exprSetsCount = new TreeMap<>();
        for (MyBooleanExpression expr : tree.expressions()) {
            int cnt = expr.getSatisfiabilitySetsCount() * (1 << (varsCount - expr.getVariablesCount()));
            exprSetsCount.put(expr.toString(), cnt);
            //System.out.println(expr.toString() + " " + cnt);
        }

        Map<String, Map<String, IntegerVariable[]>> used = new TreeMap<>();
        for (String event : pairs.keySet()) {
            Map<String, IntegerVariable[]> map = new TreeMap<>();
            for (MyBooleanExpression expr : pairs.get(event)) {
                String arrayName = "used_" + event + "_" + expr.toString();
                IntegerVariable[] vars = Choco.makeBooleanVarArray(arrayName, size);//, Options.V_NO_DECISION); // ???
                model.addVariables(vars);
                map.put(expr.toString(), vars);
            }
            used.put(event, map);
        }

        ArrayList<Constraint> ans = new ArrayList<>();

        for (String event : pairs.keySet()) {
            for (MyBooleanExpression expr : pairs.get(event)) {
                List<Node> nodes = eventExprToNodes.get(event).get(expr.toString());
                IntegerVariable[] vars = used.get(event).get(expr.toString());
                for (int color = 0; color < size; color++) {
                    List<Constraint> orClauses = new ArrayList<>();
                    for (Node node : nodes) {
                        Constraint clause = Choco.eq(nodesColorsVars[node.number()], color);
                        orClauses.add(clause);
                    }

                    Constraint orConstraint = Choco.or(orClauses.toArray(new Constraint[orClauses.size()]));
                    Constraint eqConstraint = Choco.eq(vars[color], 1);
                    Constraint res = Choco.ifOnlyIf(orConstraint, eqConstraint);
                    ans.add(res);
                }
            }
        }

        int totalSetsCount = 1 << varsCount;
        for (String event : pairs.keySet()) {
            for (int color = 0; color < size; color++) {
                List<IntegerVariable> vars = new ArrayList<>();
                int[] countsArray = new int[pairs.get(event).size()];
                int[] onesArray = new int[pairs.get(event).size()];
                int i = 0;
                for (MyBooleanExpression expr : pairs.get(event)) {
                    IntegerVariable var = used.get(event).get(expr.toString())[color];
                    vars.add(var);
                    onesArray[i] = 1;
                    countsArray[i++] = exprSetsCount.get(expr.toString());
                }
                IntegerVariable[] varsArray = vars.toArray(new IntegerVariable[vars.size()]);

                String sumVarName = "sum_" + color + "_" + event;
                int[] values = new int[]{0, totalSetsCount};
                IntegerVariable sumVar = Choco.makeIntVar(sumVarName, values);
                model.addVariable(sumVar);

                System.out.println(sumVar.pretty());
                for (IntegerVariable v : varsArray) {
                    System.out.println(v.pretty() + " ");
                }

                if (!isWeakCompleteness) {
                    sumVar = Choco.constant(totalSetsCount);
                }

                //Constraint equation = Choco.equation(sumVar, varsArray, countsArray);
                //System.out.println("Equation created");
                //ans.add(equation);

                Constraint equationFull = Choco.equation(totalSetsCount, varsArray, countsArray);
                Constraint equationZero = Choco.equation(0, varsArray, onesArray);
                ans.add(Choco.or(equationFull, equationZero));

                Constraint equation = Choco.equation(sumVar, varsArray, countsArray);

            }
        }

        return ans.toArray(new Constraint[ans.size()]);
    }

    private static Constraint[] getAdjacentConstraints(ScenarioTree tree, IntegerVariable[] nodesColorsVars) {
        ArrayList<Constraint> ans = new ArrayList<>();
        Map<Node, Set<Node>> adjacent = AdjacencyCalculator.getAdjacent(tree);

        for (Node node : tree.nodes()) {
            for (Node other : adjacent.get(node)) {
                if (other.number() < node.number()) {
                    IntegerVariable nodeColor = nodesColorsVars[node.number()];
                    IntegerVariable otherColor = nodesColorsVars[other.number()];
                    ans.add(Choco.neq(nodeColor, otherColor));
                }
            }
        }

        return ans.toArray(new Constraint[ans.size()]);
    }

    private static Constraint[] getTransitionsConstraints(int size, ScenarioTree tree,
                                                          IntegerVariable[] nodesColorsVars) {
        List<Constraint> ans = new ArrayList<>();

        Map<String, IntegerVariable[]> transitionsVars = new HashMap<>();
        for (Node node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                String key = t.event() + "[" + t.expr().toString() + "]";
                if (!transitionsVars.containsKey(key)) {
                    IntegerVariable[] vars = Choco.makeIntVarArray(key, size, 0, size - 1);
                    transitionsVars.put(key, vars);
                }

                IntegerVariable[] transitionVars = transitionsVars.get(key);
                for (int i = 0; i < size; i++) {
                    Constraint c1 = Choco.eq(nodesColorsVars[node.number()], i);
                    Constraint c2 = Choco.eq(nodesColorsVars[t.dst().number()], transitionVars[i]);
                    Constraint c = Choco.implies(c1, c2);
                    ans.add(c);
                }
            }
        }

        return ans.toArray(new Constraint[ans.size()]);
    }

    private static Constraint[] getBFSSymmetryBreakingConstraints(int size, ScenarioTree tree,
                                                                  Map<String, IntegerVariable[]> transitionsVars) {
        List<Constraint> ans = new ArrayList<>();

        List<String> eventExprOrder = new ArrayList<>();
        for (Node node : tree.nodes()) {
            for (Transition t : node.transitions()) {
                String eventExpr = t.event() + "[" + t.expr().toString() + "]";
                if (!eventExprOrder.contains(eventExpr)) {
                    eventExprOrder.add(eventExpr);
                }
            }
        }
        Collections.sort(eventExprOrder);
        assert eventExprOrder.size() == transitionsVars.size();

        IntegerVariable[][] edgeExists = new IntegerVariable[size][];
        for (int nodeColor = 0; nodeColor < size; nodeColor++) {
            edgeExists[nodeColor] = Choco.makeBooleanVarArray("edgeExists_" + nodeColor, size);
        }

        // e_a_b <=> y_a_ee1 = b \/ ... \/ y_a_een = b
        for (int nodeColor = 0; nodeColor < size; nodeColor++) {
            for (int childColor = nodeColor + 1; childColor < size; childColor++) {

                IntegerVariable[] yVars = new IntegerVariable[transitionsVars.size()];
                for (int i = 0; i < eventExprOrder.size(); i++) {
                    yVars[i] = transitionsVars.get(eventExprOrder.get(i))[nodeColor];
                }

                Constraint[] orConstraints = new Constraint[yVars.length];
                for (int i = 0; i < yVars.length; i++) {
                    orConstraints[i] = Choco.eq(yVars[i], childColor);
                }

                Constraint right = Choco.or(orConstraints);
                Constraint cst = Choco.reifiedConstraint(edgeExists[nodeColor][childColor], right);

                ans.add(cst);
            }
        }

        IntegerVariable[] parentVars = new IntegerVariable[size];
        for (int nodeColor = 1; nodeColor < size; nodeColor++) {
            parentVars[nodeColor] = Choco.makeIntVar("parent_" + nodeColor, 0, nodeColor - 1);
        }

        // p_a = b <=> e_b_a /\ ~e_{b-1}_a /\ ... /\ ~e_0_a
        for (int nodeColor = 1; nodeColor < size; nodeColor++) {
            for (int parentColor = 0; parentColor < nodeColor; parentColor++) {

                Constraint[] andConstraints = new Constraint[parentColor + 1];
                andConstraints[parentColor] = Choco.eq(edgeExists[parentColor][nodeColor], 1);
                for (int otherParent = 0; otherParent < parentColor; otherParent++) {
                    andConstraints[otherParent] = Choco.eq(edgeExists[otherParent][nodeColor], 0);
                }

                Constraint left = Choco.eq(parentVars[nodeColor], parentColor);
                ans.add(Choco.ifOnlyIf(left, Choco.and(andConstraints)));
            }
        }

        // Minimum event+expression between states
        IntegerVariable[][] minEventExpression = new IntegerVariable[size][];
        for (int nodeColor = 0; nodeColor < size; nodeColor++) {
            minEventExpression[nodeColor] =
                    Choco.makeIntVarArray("min_" + nodeColor, size, 0, eventExprOrder.size() - 1);
        }

        // e_a_b => [m_a_b = ee <=> y_a_ee = b /\ ~(y_a_{ee-1} = b) /\ ... /\ ~(y_a_{ee0} = b)]

/*

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
        */

        return ans.toArray(new Constraint[ans.size()]);
    }

    private static List<Automaton> buildAllAutomatonsFromModel(int size, ScenarioTree tree,
                                                               IntegerVariable[] nodesColorsVars, CPModel model) {
        CPSolver solver = new CPSolver();
        solver.read(model);
        solver.setTimeLimit(300000);
        solver.addGoal(new AssignVar(
                new MinDomain(solver, solver.getVar(nodesColorsVars)),
                new IncreasingDomain()));
        solver.solve();

        if (!solver.existsSolution()) {
            solver = new CPSolver();
            solver.read(model);
            solver.solve();
        }

        List<Automaton> ans = new ArrayList<>();
        if (!solver.existsSolution()) {
            return ans;
        }

        do {
            Automaton automaton = new Automaton(size);
            for (int i = 0; i < tree.nodeCount(); i++) {
                int color = solver.getVar(nodesColorsVars[i]).getVal();
                Node state = automaton.state(color);
                for (Transition t : tree.nodes().get(i).transitions()) {
                    if (!state.hasTransition(t.event(), t.expr())) {
                        int childColor = solver.getVar(nodesColorsVars[t.dst().number()]).getVal();
                        state.addTransition(t.event(), t.expr(), t.actions(), automaton.state(childColor));
                    }
                }
            }
            ans.add(automaton);
        } while (solver.nextSolution());

        return ans;
    }

    private static Automaton buildAutomatonFromModel(int size, ScenarioTree tree, IntegerVariable[] nodesColorsVars,
                                                     CPModel model) {
        CPSolver solver = new CPSolver();
        solver.read(model);
        solver.setTimeLimit(300000);
        solver.addGoal(new AssignVar(
                new MinDomain(solver, solver.getVar(nodesColorsVars)),
                new IncreasingDomain()));
        solver.solve();

        if (!solver.existsSolution()) {
            solver = new CPSolver();
            solver.read(model);
//          solver.addGoal(new AssignVar(
//                  new MinDomain(solver, solver.getVar(nodesColorsVars)), 
//                  new IncreasingDomain()));
            solver.solve();
        }

        Automaton ans = null;
        if (solver.existsSolution()) {
            ans = new Automaton(size);
            for (int i = 0; i < tree.nodeCount(); i++) {
                int color = solver.getVar(nodesColorsVars[i]).getVal();
                Node state = ans.state(color);
                for (Transition t : tree.nodes().get(i).transitions()) {
                    if (!state.hasTransition(t.event(), t.expr())) {
                        int childColor = solver.getVar(nodesColorsVars[t.dst().number()]).getVal();
                        state.addTransition(t.event(), t.expr(), t.actions(), ans.state(childColor));
                    }
                }
            }
        }

        return ans;
    }
}

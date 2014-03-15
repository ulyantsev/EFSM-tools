package algorithms;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import bool.MyBooleanExpression;

import choco.Choco;
import choco.Options;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.cp.solver.search.integer.valiterator.IncreasingDomain;
import choco.cp.solver.search.integer.varselector.MinDomain;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.model.variables.set.SetConstantVariable;
import choco.kernel.solver.Solver;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;

public class ChocoAutomatonBuilder {

    public static Automaton build(ScenariosTree tree, int size, boolean isComplete) {
        return build(tree, size, isComplete, false, null);
    }

    public static Automaton build(ScenariosTree tree, int size, boolean isComplete, boolean isWeakCompleteness) {
        return build(tree, size, isComplete, isWeakCompleteness, null);
    }

    public static Automaton build(ScenariosTree tree, int size, boolean isComplete, boolean isWeakCompleteness, PrintWriter modelPrintWriter) {
        CPModel model = new CPModel();
        IntegerVariable[] nodesColorsVars = Choco.makeIntVarArray("Color", tree.nodesCount(), 0, size - 1);
        
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

        return buildAutomatonFromModel(size, tree, nodesColorsVars, model);
    }

    private static Constraint[] getCompleteConstraints(CPModel model, int size, ScenariosTree tree, IntegerVariable[] nodesColorsVars, boolean isWeakCompleteness) {

        Map<String, Map<String, List<Node>>> eventExprToNodes = new TreeMap<String, Map<String, List<Node>>>();

        Map<String, List<MyBooleanExpression>> pairs = tree.getPairsEventExpression();
        for (String event : pairs.keySet()) {
            Map<String, List<Node>> exprMap = new TreeMap<String, List<Node>>();
            eventExprToNodes.put(event, exprMap);
            for (MyBooleanExpression expr : pairs.get(event)) {
                exprMap.put(expr.toString(), new ArrayList<Node>());
            }
        }

        for (Node node : tree.getNodes()) {
            for (Transition t : node.getTransitions()) {
                eventExprToNodes.get(t.getEvent()).get(t.getExpr().toString()).add(node);
            }
        }

        /*
         * for (String event : pairs.keySet()) { for (MyBooleanExpression expr :
         * pairs.get(event)) { System.out.print(event + " " + expr.toString());
         * for (Node node : eventExprToNodes.get(event).get(expr.toString())) {
         * System.out.print(" " + node.getNumber()); } System.out.println(); } }
         */

        int varsCount = tree.getVariablesCount();

        Map<String, Integer> exprSetsCount = new TreeMap<String, Integer>();
        for (MyBooleanExpression expr : tree.getExpressions()) {
            int cnt = expr.getSatisfiabilitySetsCount() * (1 << (varsCount - expr.getVariablesCount()));
            exprSetsCount.put(expr.toString(), cnt);
            //System.out.println(expr.toString() + " " + cnt);
        }

        Map<String, Map<String, IntegerVariable[]>> used = new TreeMap<String, Map<String, IntegerVariable[]>>();
        for (String event : pairs.keySet()) {
            Map<String, IntegerVariable[]> map = new TreeMap<String, IntegerVariable[]>();
            for (MyBooleanExpression expr : pairs.get(event)) {
                String arrayName = "used_" + event + "_" + expr.toString();
                IntegerVariable[] vars = Choco.makeBooleanVarArray(arrayName, size);//, Options.V_NO_DECISION); // ???
                model.addVariables(vars);
                map.put(expr.toString(), vars);
            }
            used.put(event, map);
        }

        ArrayList<Constraint> ans = new ArrayList<Constraint>();

        for (String event : pairs.keySet()) {
            for (MyBooleanExpression expr : pairs.get(event)) {
                List<Node> nodes = eventExprToNodes.get(event).get(expr.toString());
                IntegerVariable[] vars = used.get(event).get(expr.toString());
                for (int color = 0; color < size; color++) {
                    List<Constraint> orClauses = new ArrayList<Constraint>();
                    for (Node node : nodes) {
                        Constraint clause = Choco.eq(nodesColorsVars[node.getNumber()], color);
                        orClauses.add(clause);
                    }

                    Constraint orConstraint = Choco.or(orClauses.toArray(new Constraint[0]));
                    Constraint eqConstraint = Choco.eq(vars[color], 1);
                    Constraint res = Choco.ifOnlyIf(orConstraint, eqConstraint);
                    ans.add(res);
                }
            }
        }

        int totalSetsCount = 1 << varsCount;
        for (String event : pairs.keySet()) {
            for (int color = 0; color < size; color++) {
                List<IntegerVariable> vars = new ArrayList<IntegerVariable>();
                int[] countsArray = new int[pairs.get(event).size()];
                int[] onesArray = new int[pairs.get(event).size()];
                int i = 0;
                for (MyBooleanExpression expr : pairs.get(event)) {
                    IntegerVariable var = used.get(event).get(expr.toString())[color];
                    vars.add(var);
                    onesArray[i] = 1;
                    countsArray[i++] = exprSetsCount.get(expr.toString());
                }
                IntegerVariable[] varsArray = vars.toArray(new IntegerVariable[0]);
                
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

        return ans.toArray(new Constraint[0]);
    }

    private static Constraint[] getAdjacentConstraints(ScenariosTree tree, IntegerVariable[] nodesColorsVars) {
        ArrayList<Constraint> ans = new ArrayList<Constraint>();
        Map<Node, Set<Node>> adjacent = AdjacentCalculator.getAdjacent(tree);

        for (Node node : tree.getNodes()) {
            for (Node other : adjacent.get(node)) {
                if (other.getNumber() < node.getNumber()) {
                    IntegerVariable nodeColor = nodesColorsVars[node.getNumber()];
                    IntegerVariable otherColor = nodesColorsVars[other.getNumber()];
                    ans.add(Choco.neq(nodeColor, otherColor));
                }
            }
        }

        return ans.toArray(new Constraint[0]);
    }

    private static Constraint[] getTransitionsConstraints(int size, ScenariosTree tree,
            IntegerVariable[] nodesColorsVars) {
        List<Constraint> ans = new ArrayList<Constraint>();

        Map<String, IntegerVariable[]> transitionsVars = new HashMap<String, IntegerVariable[]>();
        for (Node node : tree.getNodes()) {
            for (Transition t : node.getTransitions()) {
                String key = t.getEvent() + "[" + t.getExpr().toString() + "]";
                if (!transitionsVars.containsKey(key)) {
                    IntegerVariable[] vars = Choco.makeIntVarArray(key, size, 0, size - 1);
                    transitionsVars.put(key, vars);
                }

                IntegerVariable[] transitionVars = transitionsVars.get(key);
                for (int i = 0; i < size; i++) {
                    Constraint c1 = Choco.eq(nodesColorsVars[node.getNumber()], i);
                    Constraint c2 = Choco.eq(nodesColorsVars[t.getDst().getNumber()], transitionVars[i]);
                    Constraint c = Choco.implies(c1, c2);
                    ans.add(c);
                }
            }
        }

        return ans.toArray(new Constraint[0]);
    }

    private static Automaton buildAutomatonFromModel(int size, ScenariosTree tree, IntegerVariable[] nodesColorsVars,
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
            for (int i = 0; i < tree.nodesCount(); i++) {
                int color = solver.getVar(nodesColorsVars[i]).getVal();
                Node state = ans.getState(color);
                for (Transition t : tree.getNodes().get(i).getTransitions()) {
                    if (!state.hasTransition(t.getEvent(), t.getExpr())) {
                        int childColor = solver.getVar(nodesColorsVars[t.getDst().getNumber()]).getVal();
                        state.addTransition(t.getEvent(), t.getExpr(), t.getActions(), ans.getState(childColor));
                    }
                }
            }
        }

        return ans;
    }
}

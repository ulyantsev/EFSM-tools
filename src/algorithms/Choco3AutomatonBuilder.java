package algorithms;

import bool.MyBooleanExpression;
import choco.Choco;
import solver.constraints.Constraint;
import solver.variables.BoolVar;
import solver.variables.IntVar;
import solver.variables.VariableFactory;
import solver.constraints.IntConstraintFactory;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import solver.Solver;
import structures.Transition;

/**
 * Vladimir Ulyantsev
 * Date: 30.07.13
 * Time: 16:52
 */
public class Choco3AutomatonBuilder {
    public static Automaton build(ScenariosTree tree, int size, boolean isComplete) {
        return build(tree, size, isComplete, false, null);
    }

    public static Automaton build(ScenariosTree tree, int size, boolean isComplete, boolean isWeakCompleteness) {
        return build(tree, size, isComplete, isWeakCompleteness, null);
    }

    public static Automaton build(ScenariosTree tree, int size, boolean isComplete, boolean isWeakCompleteness, PrintWriter modelPrintWriter) {
        Solver solver = new Solver("Automaton builder solver");
        IntVar[] nodesColorsVars = VariableFactory.boundedArray("Color", tree.nodesCount(), 0, size - 1, solver);

        Constraint rootColor = IntConstraintFactory.arithm(nodesColorsVars[0], "=", 0);
        solver.post(rootColor);


        return null;
    }

    private static Constraint[] getCompleteConstraints(Solver solver,
                                                       int size, ScenariosTree tree,
                                                       IntVar[] nodesColorsVars, boolean isWeakCompleteness) {
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

        int varsCount = tree.getVariablesCount();

        Map<String, Integer> exprSetsCount = new TreeMap<String, Integer>();
        for (MyBooleanExpression expr : tree.getExpressions()) {
            int cnt = expr.getSatisfiabilitySetsCount() * (1 << (varsCount - expr.getVariablesCount()));
            exprSetsCount.put(expr.toString(), cnt);
        }

        Map<String, Map<String, BoolVar[]>> used = new TreeMap<String, Map<String, BoolVar[]>>();
        for (String event : pairs.keySet()) {
            Map<String, BoolVar[]> map = new TreeMap<String, BoolVar[]>();
            for (MyBooleanExpression expr : pairs.get(event)) {
                String arrayName = "used_" + event + "_" + expr.toString();
                BoolVar[] vars = VariableFactory.boolArray(arrayName, size, solver);
                map.put(expr.toString(), vars);
            }
            used.put(event, map);
        }

        ArrayList<Constraint> ans = new ArrayList<Constraint>();

        for (String event : pairs.keySet()) {
            for (MyBooleanExpression expr : pairs.get(event)) {
                List<Node> nodes = eventExprToNodes.get(event).get(expr.toString());
                BoolVar[] vars = used.get(event).get(expr.toString());
                for (int color = 0; color < size; color++) {
                    List<Constraint> orClauses = new ArrayList<Constraint>();
                    for (Node node : nodes) {
                        Constraint clause =
                                IntConstraintFactory.arithm(nodesColorsVars[node.getNumber()], "=", color);
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
                List<IntVar> vars = new ArrayList<IntegerVariable>();
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
}

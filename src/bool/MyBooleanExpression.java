package bool;

import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

public class MyBooleanExpression {
    private static Map<String, MyBooleanExpression> expressions = new TreeMap<>();

    public static MyBooleanExpression getTautology() {
        try {
            return get("1");
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }
    
    public static MyBooleanExpression get(String repr) throws ParseException {
        if (expressions.containsKey(repr)) {
            return expressions.get(repr);
        }
        final MyBooleanExpression newExpr = new MyBooleanExpression(repr);
        for (MyBooleanExpression oldExpr : expressions.values()) {
            if (oldExpr.equals(newExpr)) {
                expressions.put(repr, oldExpr);
                return oldExpr;
            }
        }
        expressions.put(repr, newExpr);
        return newExpr;
    }

    private int satisfiabilitySetsCount;
    private final String repr;
    private final String[] variables;
    private final Map<Map<String, Boolean>, Boolean> truthTable;
    private static LinkedHashMap<String, Integer> varToNumber = new LinkedHashMap<>();

    public static int varToNumber(String varName) {
        Integer value = varToNumber.get(varName);
        if (value == null) {
            value = varToNumber.size();
            varToNumber.put(varName, value);
        }

        if (varToNumber.size() >= 26) {
            throw new RuntimeException("The number of variables should not exceed 25.");
        }

        return value;
    }

    public static void registerVariableNames(List<String> varNames) {
        varNames.forEach(MyBooleanExpression::varToNumber);
    }

    private MyBooleanExpression(String expression) throws ParseException {
        repr = expression.replaceAll(" ", "").replaceAll("!", "~");
        final Pattern pattern = Pattern.compile("[()~&|=>+]");
        final HashSet<String> varsSet = new LinkedHashSet<>(Arrays.asList(pattern.split(repr)));
        varsSet.removeAll(Arrays.asList("", "1", "0"));
        variables = varsSet.toArray(new String[varsSet.size()]);

        String shortExpr = repr;
        for (String var : variables) {
            shortExpr = shortExpr.replaceAll(var, "___" + varToNumber(var));
        }
        for (int i = 0; i < varToNumber.size(); i++) {
            shortExpr = shortExpr.replaceAll("___" + i, String.valueOf((char) ('a' + i)));
        }
        
        final BooleanExpression booleanExpression = new BooleanExpression(shortExpr);
        final Map<Map<String, Boolean>, Map<BooleanExpression, Boolean>> truthTable = new TruthTable(booleanExpression)
                .getResults();
        this.truthTable = new HashMap<>();

        for (Map<String, Boolean> map : truthTable.keySet()) {
            final Map<BooleanExpression, Boolean> booleanMap = truthTable.get(map);
            final boolean val = booleanMap.containsValue(true);
            satisfiabilitySetsCount += val ? 1 : 0;
            this.truthTable.put(map, val);
        }
    }

    private List<Map<String, Boolean>> extendForVars(Map<String, Boolean> varAssignment, Set<Integer> varIndices) {
        if (varIndices.isEmpty()) {
            return Collections.singletonList(varAssignment);
        }
        final int index = varIndices.iterator().next();
        varIndices.remove(index);
        final List<Map<String, Boolean>> prevAns = extendForVars(varAssignment, varIndices);
        varIndices.add(index);
        final List<Map<String, Boolean>> ans = new ArrayList<>();
        for (Map<String, Boolean> m : prevAns) {
            final Map<String, Boolean> m0 = new HashMap<>(m);
            final Map<String, Boolean> m1 = new HashMap<>(m);
            m0.put(String.valueOf((char) ('a' + index)), false);
            m1.put(String.valueOf((char) ('a' + index)), true);
            ans.add(m0);
            ans.add(m1);
        }
        return ans;
    }
    
    private List<Map<String, Boolean>> extendForAllVars(Map<String, Boolean> varAssignment) {
        final Set<Integer> remainingNumbers = new TreeSet<>();
        for (int i = 0; i < varToNumber.size(); i++) {
            if (!varAssignment.containsKey(String.valueOf((char) ('a' + i)))) {
                remainingNumbers.add(i);
            }
        }
        return extendForVars(varAssignment, remainingNumbers);
    }
    
    public List<String> getSatVarCombinations() {
        final List<String> combinations = new ArrayList<>();
        for (Map.Entry<Map<String, Boolean>, Boolean> entry : truthTable.entrySet()) {
            //System.out.println(entry);
            if (entry.getValue()) {
                final List<Map<String, Boolean>> varAssignments = extendForAllVars(entry.getKey());
                for (Map<String, Boolean> varAssignment : varAssignments) {
                    final char[] assignment = new char[varToNumber.size()];
                    for (int i = 0; i < varToNumber.size(); i++) {
                        assignment[i] = varAssignment.get(String.valueOf((char) ('a' + i))) ? '1' : '0';
                    }
                    combinations.add(String.valueOf(assignment));
                }
            }
        }
        //System.out.println(this + " " + combinations);
        return combinations;
    }
    
    public String[] getVariables() {
        return variables;
    }

    public int getVariablesCount() {
        return variables.length;
    }

    public int getSatisfiabilitySetsCount() {
        return satisfiabilitySetsCount;
    }

    private boolean hasSolution() {
        return truthTable.containsValue(true);
    }

    private boolean isTautology() {
        return !truthTable.containsValue(false);
    }

    public boolean equals(MyBooleanExpression other) {
        if (other.repr.equals(repr)) {
            return true;
        }

        // possible to rewrite in a smarter way
        try {
            return new MyBooleanExpression("(" + repr + ")=(" + other.repr + ")").isTautology();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<MyBooleanExpression, Boolean> hasSolutionWithRes;

    public boolean hasSolutionWith(MyBooleanExpression other) {
        if (hasSolutionWithRes == null) {
            hasSolutionWithRes = new HashMap<>();
        }

        if (hasSolutionWithRes.containsKey(other)) {
            return hasSolutionWithRes.get(other);
        }

        // possible to rewrite in a smarter way
        try {
            boolean res = new MyBooleanExpression("(" + repr + ")&(" + other.repr + ")").hasSolution();
            hasSolutionWithRes.put(other, res);
            return res;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        return repr;
    }
}

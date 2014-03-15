package bool;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MyBooleanExpression {
    private static Map<String, MyBooleanExpression> expressions = new TreeMap<String, MyBooleanExpression>();

    public static MyBooleanExpression get(String repr) throws ParseException {
        if (expressions.containsKey(repr)) {
            return expressions.get(repr);
        }
        MyBooleanExpression newExpr = new MyBooleanExpression(repr);
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

    private String repr;

    private String[] variables;

    private Map<Map<String, Boolean>, Boolean> truthTable;

    private MyBooleanExpression(String expression) throws ParseException {
        expression = expression.replaceAll(" ", "").replaceAll("!", "~");
        this.repr = expression;
        Pattern pattern = Pattern.compile("[()~&|=>+]");
        String[] vars = pattern.split(expression);

        HashSet<String> varsSet = new HashSet<String>(Arrays.asList(vars));
        varsSet.removeAll(Arrays.asList(new String[] { "", "1", "0" }));
        this.variables = varsSet.toArray(new String[0]);

        assert variables.length < 26;

        String shortExpr = new String(expression);
        for (int i = 0; i < variables.length; i++) {
            shortExpr = shortExpr.replaceAll(variables[i], "___" + i);
        }
        for (int i = 0; i < variables.length; i++) {
            shortExpr = shortExpr.replaceAll("___" + i, "" + (char) ('a' + i));
        }

        //System.out.println(shortExpr);
        
        BooleanExpression booleanExpression = new BooleanExpression(shortExpr);
        Map<Map<String, Boolean>, Map<BooleanExpression, Boolean>> truthTable = new TruthTable(booleanExpression)
                .getResults();
        this.truthTable = new HashMap<Map<String, Boolean>, Boolean>();

        for (Map<String, Boolean> map : truthTable.keySet()) {
            Map<BooleanExpression, Boolean> booleanMap = truthTable.get(map);
            boolean val = booleanMap.containsValue(true);
            satisfiabilitySetsCount += val ? 1 : 0;
            this.truthTable.put(map, val);
        }
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

    public boolean hasSolution() {
        return truthTable.containsValue(true);
    }

    public boolean isTautology() {
        return !truthTable.containsValue(false);
    }

    public boolean equals(MyBooleanExpression other) {
        if (other.repr.equals(this.repr)) {
            return true;
        }

        // можно переписать умнее
        MyBooleanExpression e = null;
        try {
            e = new MyBooleanExpression("(" + this.repr + ")=(" + other.repr + ")");
        } catch (ParseException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        /*
        if (e.isTautology()) {
            System.out.println(e + "    " + e.isTautology());
            System.out.println(e.satisfiabilitySetsCount);
        }
        */
        return e.isTautology();
    }

    private Map<MyBooleanExpression, Boolean> hasSolutionWithRes;

    public boolean hasSolutionWith(MyBooleanExpression other) {
        if (hasSolutionWithRes == null) {
            hasSolutionWithRes = new HashMap<MyBooleanExpression, Boolean>();
        }

        if (hasSolutionWithRes.containsKey(other)) {
            return hasSolutionWithRes.get(other);
        }

        // можно переписать умнее
        MyBooleanExpression e = null;
        try {
            e = new MyBooleanExpression("(" + repr + ")&(" + other.repr + ")");
        } catch (ParseException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        boolean res = e.hasSolution();
        hasSolutionWithRes.put(other, res);
        return res;
    }

    public String toString() {
        return repr;
    }
}

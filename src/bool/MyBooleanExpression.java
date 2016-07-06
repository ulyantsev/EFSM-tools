package bool;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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

        HashSet<String> varsSet = new HashSet<>(Arrays.asList(vars));
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
        this.truthTable = new HashMap<>();

        for (Map<String, Boolean> map : truthTable.keySet()) {
            Map<BooleanExpression, Boolean> booleanMap = truthTable.get(map);
            boolean val = booleanMap.containsValue(true);
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
    
    private List<Map<String, Boolean>> extendForAllVars(Map<String, Boolean> varAssignment, int varNum) {
    	final Set<Integer> remainingNumbers = new TreeSet<>();
    	for (int i = 0; i < varNum; i++) {
    		if (!varAssignment.containsKey(String.valueOf((char) ('a' + i)))) {
    			remainingNumbers.add(i);
    		}
    	}
    	return extendForVars(varAssignment, remainingNumbers);
    }
    
    public List<String> getSatVarCombinations(int varNum) {
    	final List<String> combinations = new ArrayList<>();
    	for (Map.Entry<Map<String, Boolean>, Boolean> entry : truthTable.entrySet()) {
    		if (entry.getValue()) {
    			final List<Map<String, Boolean>> varAssignments = extendForAllVars(entry.getKey(), varNum);
    			for (Map<String, Boolean> varAssignment : varAssignments) {
	    			final char[] assignment = new char[varNum];
	        		for (int i = 0; i < varNum; i++) {
	        			assignment[i] = varAssignment.get(String.valueOf((char) ('a' + i))) ? '1' : '0';
	        		}
	    			combinations.add(String.valueOf(assignment));
    			}
    		}
    	}
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

        // possible to rewrite in a smarter way
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
            hasSolutionWithRes = new HashMap<>();
        }

        if (hasSolutionWithRes.containsKey(other)) {
            return hasSolutionWithRes.get(other);
        }

        // possible to rewrite in a smarter way
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

    @Override
    public String toString() {
        return repr;
    }
}

package scenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import bool.MyBooleanExpression;

public class StringScenario {
	private static String removeVariables(String input, int varNumber) throws ParseException {
		final Pattern p = Pattern.compile("(\\w+)\\s+\\[([^\\[\\]]+)\\]");
		final StringBuilder sb = new StringBuilder();
		final Matcher m = p.matcher(input);
		int lastPos = 0;
		while (m.find()) {
			final String event = m.group(1);
			sb.append(input.substring(lastPos, m.start()));
			final List<String> expansion = new ArrayList<>();
			final MyBooleanExpression expr = MyBooleanExpression.get(m.group(2));
			final List<String> varAssignments = expr.getSatVarCombinations(varNumber);
			for (String varAssignment : varAssignments) {
				expansion.add(event + varAssignment);
			}
			lastPos = m.end();
			String strToAppend = String.join("|", expansion) + "[1]";
			sb.append(strToAppend);
		}
		sb.append(input.substring(lastPos, input.length()));
		return sb.toString();
	}
	
	public static List<StringScenario> loadScenarios(String filepath) throws ParseException, FileNotFoundException {
		return loadScenarios(filepath, -1);
	}
	
	/*
	 * varNumber = -1 for no variable removal
	 */
    public static List<StringScenario> loadScenarios(String filepath, int varNumber) throws ParseException, FileNotFoundException {
        List<StringScenario> ans = new ArrayList<>();
        
        try (Scanner in = new Scanner(new File(filepath))) {
	        String inp = "";
	        while (in.hasNextLine()) {
	            String s = in.nextLine().trim();
	            if (varNumber != -1) {
	            	s = removeVariables(s, varNumber);
	            }
	            if (inp.isEmpty() && s.isEmpty()) {
	                continue;
	            }
	
	            if (inp.isEmpty()) {
	                inp = s;
	            } else {
	                ans.add(new StringScenario(inp, s));
	                inp = "";
	            }
	        }
        }

        return ans;
    }
    
    boolean isPositive;
    
    private List<List<String>> events = new ArrayList<>();
    
    private List<MyBooleanExpression> expressions = new ArrayList<>();
    
    private List<StringActions> actions = new ArrayList<>();
    
    public StringScenario(boolean isPositive,
                          List<String> events,
                          List<MyBooleanExpression> expressions,
                          List<StringActions> actions) {
    	this.isPositive = isPositive;
    	
    	if (events.size() != expressions.size() || events.size() != actions.size()) {
    		throw new RuntimeException("Events, expressions, actions sizes mismatch: " + 
    									events.size() + " " + expressions.size() + " " + actions.size());
    	}

    	this.events = events.stream().map(e -> Collections.singletonList(e)).collect(Collectors.toList());
    	this.expressions = new ArrayList<>(expressions);
    	this.actions = new ArrayList<>(actions);
    }
    
    private List<String> splitEvent(String event) {
    	return Arrays.asList(event.split("\\|"));
    }
    
    public StringScenario(String input, String output) throws ParseException {
        String[] events = input.split(";");
        String[] actions = (output + " ").split(";");
        if (actions.length != events.length) {
            throw new ParseException("events length " + events.length + " != actions length " + actions.length + ": [" + input + "] / [" + output + "]", 0);
        }
         
        int n = actions.length;
        
        for (int i = 0; i < n; i++) {
            MyBooleanExpression expr;
            
            if (events[i].contains("[")) {
                String[] p = events[i].split("\\[");
                this.events.add(splitEvent(p[0].trim()));
                expr = MyBooleanExpression.get(p[1].replace(']', ' '));
            } else {
                this.events.add(splitEvent(events[i].trim()));
                expr = MyBooleanExpression.get("1");            
            }
            
            this.expressions.add(expr);
            
            this.actions.add(new StringActions(actions[i]));
        }
        
        this.isPositive = true;
    }
    
    public int size() {
        return events.size();
    }
    
    public List<String> getEvents(int pos) {
        return events.get(pos);
    }
    
    public MyBooleanExpression getExpr(int pos) {
        return expressions.get(pos);
    }
    
    public StringActions getActions(int pos) {
        return actions.get(pos);
    }
    
    /*
     * Since events are now event vectors (to support multiple transitions from parents to children in the scenario tree),
     * the correct way to display a normal event is without "[", "]".
     */
    private static String eventListToString(List<String> eventList) {
    	return eventList.size() == 1 ? eventList.get(0) : eventList.toString();
    }
    
    public String toString() {
        String inp = "";
        String out = "";
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                inp += "; ";
                out += "; ";
            }
            inp += eventListToString(events.get(i)) + "[" + expressions.get(i).toString() + "]";
            out += actions.get(i).toString();
        }
        return inp + "\n" + out;
    }
    
    // TODO
    public String toJSON() {
    	String ans =
    		"{" + "'type': " + (isPositive ? "'positive'" : "'negative'") + ",\n" +
    		" 'scenario': [";
    	for (int pos = 0; pos < size(); pos++) {
    		if (pos > 0) {
    			ans += ",\n              ";
    		}
    		ans += "{'event': '" + events.get(pos) + "', 'guard': '" + expressions.get(pos) + "', 'actions': '" + actions.get(pos) + "'}";
    	}
    	ans += "             ]\n}\n";
    	
    	return ans;
    }
}

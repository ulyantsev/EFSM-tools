package scenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import bool.MyBooleanExpression;
import actions.StringActions;


public class StringScenario {
    
    public static List<StringScenario> loadScenarios(String filepath) throws ParseException, FileNotFoundException {
        List<StringScenario> ans = new ArrayList<StringScenario>();
        
        Scanner in = new Scanner(new File(filepath));

        String inp = "";
        while (in.hasNextLine()) {
            String s = in.nextLine().trim();
            if (inp.equals("") && s.equals("")) {
                continue;
            }

            if (inp.equals("")) {
                inp = s;
            } else {
                ans.add(new StringScenario(inp, s));
                inp = "";
            }
        }

        in.close();
        return ans;
    }
    
    boolean isPositive;
    
    private ArrayList<String> events = new ArrayList<String>();
    
    private ArrayList<MyBooleanExpression> expressions = new ArrayList<MyBooleanExpression>();
    
    private ArrayList<StringActions> actions = new ArrayList<StringActions>();
    
    public StringScenario(boolean isPositive, ArrayList<String> events, ArrayList<MyBooleanExpression> expressions, ArrayList<StringActions> actions) {
    	this.isPositive = isPositive;
    	
    	if (events.size() != expressions.size() || events.size() != actions.size()) {
    		throw new RuntimeException("Events, expressions, actions sizes mismatch: " + 
    									events.size() + " " + expressions.size() + " " + actions.size());
    	}

    	this.events = new ArrayList<String>(events);
    	this.expressions = new ArrayList<MyBooleanExpression>(expressions);
    	this.actions = new ArrayList<StringActions>(actions);
    }
    
    public StringScenario(String input, String output) throws ParseException {
        String[] events = input.split(";");
        String[] actions = (output + " ").split(";");
        if (actions.length != events.length) {
            throw new ParseException("events length " + events.length + " != actions length " + actions.length, 0);
        }
         
        int n = actions.length;
        
        for (int i = 0; i < n; i++) {
            MyBooleanExpression expr;
            
            if (events[i].contains("[")) {
                String[] p = events[i].split("\\[");
                this.events.add(p[0].trim());
                expr = MyBooleanExpression.get(p[1].replace(']', ' '));
            } else {
                this.events.add(events[i].trim());
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
    
    public String getEvent(int pos) {
        return events.get(pos);
    }
    
    public MyBooleanExpression getExpr(int pos) {
        return expressions.get(pos);
    }
    
    public StringActions getActions(int pos) {
        return actions.get(pos);
    }
    
    public String toString() {
        String inp = "";
        String out = "";
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                inp += "; ";
                out += "; ";
            }
            inp += events.get(i) + "[" + expressions.get(i).toString() + "]";
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

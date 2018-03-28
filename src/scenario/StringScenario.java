package scenario;

import bool.MyBooleanExpression;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringScenario {
    private List<List<String>> events = new ArrayList<>();
    private List<MyBooleanExpression> expressions = new ArrayList<>();
    private List<StringActions> actions = new ArrayList<>();

    @Override
    public String toString() {
        final StringBuilder inputs = new StringBuilder();
        final StringBuilder outputs = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                inputs.append("; ");
                outputs.append("; ");
            }
            inputs.append(eventListToString(events.get(i))).append("[").append(expressions.get(i).toString())
                    .append("]");
            outputs.append(actions.get(i).toString());
        }
        return inputs + "\n" + outputs;
    }

    public static List<StringScenario> loadScenarios(String filepath, boolean removeVars)
            throws ParseException, IOException {
        final List<StringScenario> ans = new ArrayList<>();

        try (BufferedReader in = new BufferedReader(new FileReader(filepath))) {
            String inp = "";
            String line;
            while ((line = in.readLine()) != null) {
                String s = line.trim();
                if (removeVars) {
                    s = removeVariables(s);
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
    
    public StringScenario(List<String> events, List<MyBooleanExpression> expressions, List<StringActions> actions) {
        if (events.size() != expressions.size() || events.size() != actions.size()) {
            throw new RuntimeException("Events, expressions, actions sizes mismatch: " + 
                                        events.size() + " " + expressions.size() + " " + actions.size());
        }

        this.events = events.stream().map(Collections::singletonList).collect(Collectors.toList());
        this.expressions = new ArrayList<>(expressions);
        this.actions = new ArrayList<>(actions);
    }
    
    private static List<String> splitEvent(String event) {
        return Arrays.asList(event.split("\\|"));
    }
    
    public StringScenario(String input, String output) throws ParseException {
        final String[] events = input.split(";");
        final String[] actions = (output + " ").split(";");
        if (actions.length != events.length) {
            throw new ParseException("events length " + events.length + " != actions length " + actions.length
                    + ": [" + input + "] / [" + output + "]", 0);
        }

        for (int i = 0; i < actions.length; i++) {
            MyBooleanExpression expr;
            
            if (events[i].contains("[")) {
                final String[] p = events[i].split("\\[");
                this.events.add(splitEvent(p[0].trim()));
                expr = MyBooleanExpression.get(p[1].replace(']', ' '));
            } else {
                this.events.add(splitEvent(events[i].trim()));
                expr = MyBooleanExpression.get("1");            
            }
            
            expressions.add(expr);
            this.actions.add(new StringActions(actions[i]));
        }
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

    public static String removeVariables(String input) throws ParseException {
        final Pattern p = Pattern.compile("(\\w+)(\\s)*+\\[([^\\[\\]]+)\\]");
        final StringBuilder sb = new StringBuilder();
        final Matcher m = p.matcher(input);
        int lastPos = 0;
        while (m.find()) {
            final String event = m.group(1);
            sb.append(input, lastPos, m.start());
            final MyBooleanExpression expr = MyBooleanExpression.get(m.group(3));
            final List<String> expansion = expr.getSatVarCombinations().stream()
                    .map(varAssignment -> event + varAssignment)
                    .collect(Collectors.toList());
            lastPos = m.end();
            sb.append(String.join("|", expansion)).append("[1]");
        }
        sb.append(input, lastPos, input.length());
        return sb.toString();
    }
}

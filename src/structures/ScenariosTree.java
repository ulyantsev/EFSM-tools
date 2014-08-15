package structures;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import actions.StringActions;
import bool.MyBooleanExpression;
import scenario.StringScenario;

public class ScenariosTree {
    private Node root;

    private List<Node> nodes;

    public ScenariosTree() {
        this.root = new Node(0);
        this.nodes = new ArrayList<Node>();
        this.nodes.add(root);
    }

    public Node getRoot() {
        return root;
    }

    public void load(String filepath) throws FileNotFoundException, ParseException {
        for (StringScenario scenario : StringScenario.loadScenarios(filepath)) {
            addScenario(scenario);
        }
    }

    private void addScenario(StringScenario scenario) throws ParseException {
        Node node = root;
        for (int i = 0; i < scenario.size(); i++) {
            addTransition(node, scenario.getEvent(i), scenario.getExpr(i), scenario.getActions(i));
            node = node.getDst(scenario.getEvent(i), scenario.getExpr(i));
        }
    }

    private void addTransition(Node src, String event, MyBooleanExpression expr, StringActions actions)
            throws ParseException {
        if (src.hasTransition(event, expr)) {
            Transition t = src.getTransition(event, expr);
            if (!t.getActions().equals(actions)) {
                throw new ParseException("bad transition add in node " + src.getNumber() + ": " + t.getActions()
                        + " != " + actions, 0);
            }
        } else {
            Node dst = new Node(nodes.size());
            nodes.add(dst);
            src.addTransition(event, expr, actions, dst);
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public int nodesCount() {
        return nodes.size();
    }

    public String[] getEvents() {
        Set<String> events = new HashSet<String>();
        for (Node node : nodes) {
            for (Transition transition : node.getTransitions()) {
                events.add(transition.getEvent());
            }
        }
        return events.toArray(new String[events.size()]);
    }
    
    public List<String> getActions() {
        Set<String> actions = new TreeSet<>();
        for (Node node : nodes) {
            for (Transition transition : node.getTransitions()) {
            	for (String action : transition.getActions().getActions()) {
            		actions.add(action);
            	}
            }
        }
        return new ArrayList<>(actions);
    }
    
    public int getEventsCount() {
        return getEvents().length;
    }
        
    public String[] getVariables() {
        Set<String> variables = new HashSet<String>();
        for (Node node : nodes) {
            for (Transition transition : node.getTransitions()) {
                String[] transitionVars = transition.getExpr().getVariables();
                variables.addAll(Arrays.asList(transitionVars));
            }
        }
        return variables.toArray(new String[variables.size()]);
    }

    public int getVariablesCount() {
        return getVariables().length;
    }
    
    public Map<String, List<MyBooleanExpression>> getPairsEventExpression() {
        Map<String, List<MyBooleanExpression>> ans = new HashMap<String, List<MyBooleanExpression>>();
        for (Node node : nodes) {
            for (Transition transition : node.getTransitions()) {
                String event = transition.getEvent();
                MyBooleanExpression expr = transition.getExpr();
                if (ans.containsKey(event)) {
                    if (!ans.get(event).contains(expr)) {
                        ans.get(event).add(expr);
                    }
                } else {
                    ArrayList<MyBooleanExpression> exprList = new ArrayList<MyBooleanExpression>();
                    exprList.add(expr);
                    ans.put(event, exprList);
                }
            }
        }
                
        return ans;
    }
    
    public Collection<MyBooleanExpression> getExpressions() {
        List<MyBooleanExpression> ans = new ArrayList<MyBooleanExpression>();
        for (Node node : this.nodes) {
            for (Transition t : node.getTransitions()) {
                if (!ans.contains(t.getExpr())) {
                    ans.add(t.getExpr());
                }
            }
        }
        return ans;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# generated file, don't try to modify\n");
        sb.append("# command: dot -Tpng <filename> > tree.png\n");
        sb.append("digraph ScenariosTree {\n    node [shape = circle];\n");

        for (Node node : nodes) {
            for (Transition t : node.getTransitions()) {
                sb.append("    " + t.getSrc().getNumber() + " -> " + t.getDst().getNumber());
                sb.append(" [label = \"" + t.getEvent() + " [" + t.getExpr().toString() + "] ("
                        + t.getActions().toString() + ") \"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

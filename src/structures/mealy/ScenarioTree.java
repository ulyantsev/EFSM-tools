package structures.mealy;

import bool.MyBooleanExpression;
import scenario.StringActions;
import scenario.StringScenario;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class ScenarioTree {
    private final MealyNode root;
    private final List<MealyNode> nodes;

    public ScenarioTree() {
        this.root = new MealyNode(0);
        this.nodes = new ArrayList<>();
        this.nodes.add(root);
    }

    public MealyNode root() {
        return root;
    }

    public void load(String filepath) throws FileNotFoundException, ParseException {
    	load(filepath, -1);
    }
    
    /*
     * varNumber = -1 for no variable removal
     */
    public void load(String filepath, int varNumber) throws FileNotFoundException, ParseException {
        for (StringScenario scenario : StringScenario.loadScenarios(filepath, varNumber)) {
            addScenario(scenario);
        }
    }

    public void addScenario(StringScenario scenario) throws ParseException {
        MealyNode node = root;
        for (int i = 0; i < scenario.size(); i++) {
            addTransitions(node, scenario.getEvents(i), scenario.getExpr(i), scenario.getActions(i));
            node = node.dst(scenario.getEvents(i).get(0), scenario.getExpr(i));
        }
    }

    /*
     * If events.size() > 1, will add multiple edges towards the same destination.
     */
    private void addTransitions(MealyNode src, List<String> events, MyBooleanExpression expr,
                                StringActions actions) throws ParseException {
    	if (events.isEmpty()) {
            throw new AssertionError();
        }
    	MealyNode dst = null;
    	for (String e : events) {
	        if (src.hasTransition(e, expr)) {
	            final MealyTransition t = src.transition(e, expr);
	            if (!t.actions().equals(actions)) {
	                throw new ParseException("bad transition add in node "
	                		+ src.number() + ": " + t.actions()
	                        + " != " + actions, 0);
	            }
	        } else {
	        	if (dst == null) {
	        		dst = new MealyNode(nodes.size());
	        		nodes.add(dst);
	        	}
	            src.addTransition(e, expr, actions, dst);
	        }
    	}
    }

    public List<MealyNode> nodes() {
        return nodes;
    }

    public int nodeCount() {
        return nodes.size();
    }

    public String[] events() {
        final Set<String> events = new HashSet<>();
        for (MealyNode node : nodes) {
            events.addAll(node.transitions().stream().map(MealyTransition::event).collect(Collectors.toList()));
        }
        return events.toArray(new String[events.size()]);
    }
    
    public List<String> actions() {
        final Set<String> actions = new TreeSet<>();
        for (MealyNode node : nodes) {
            for (MealyTransition transition : node.transitions()) {
            	for (String action : transition.actions().getActions()) {
            		actions.add(action);
            	}
            }
        }
        return new ArrayList<>(actions);
    }

    public String[] variables() {
        final Set<String> variables = new HashSet<>();
        for (MealyNode node : nodes) {
            for (MealyTransition transition : node.transitions()) {
                variables.addAll(Arrays.asList(transition.expr().getVariables()));
            }
        }
        return variables.toArray(new String[variables.size()]);
    }

    public int variableCount() {
        return variables().length;
    }
    
    public Map<String, List<MyBooleanExpression>> pairsEventExpression() {
        final Map<String, List<MyBooleanExpression>> ans = new HashMap<>();
        for (MealyNode node : nodes) {
            for (MealyTransition transition : node.transitions()) {
                final String event = transition.event();
                final MyBooleanExpression expr = transition.expr();
                if (ans.containsKey(event)) {
                    if (!ans.get(event).contains(expr)) {
                        ans.get(event).add(expr);
                    }
                } else {
                    ans.put(event, new ArrayList<>(Collections.singletonList(expr)));
                }
            }
        }
                
        return ans;
    }
    
    public Collection<MyBooleanExpression> expressions() {
        final List<MyBooleanExpression> ans = new ArrayList<>();
        for (MealyNode node : nodes) {
            for (MealyTransition t : node.transitions()) {
                if (!ans.contains(t.expr())) {
                    ans.add(t.expr());
                }
            }
        }
        return ans;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("# generated file, don't try to modify\n");
        sb.append("# command: dot -Tpng <filename> > tree.png\n");
        sb.append("digraph ScenariosTree {\n    node [shape = circle];\n");

        for (MealyNode node : nodes) {
            for (MealyTransition t : node.transitions()) {
                sb.append("    " + t.src().number() + " -> " + t.dst().number());
                sb.append(" [label = \"" + t.event() + " [" + t.expr().toString() + "] ("
                        + t.actions().toString() + ") \"];\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }
}

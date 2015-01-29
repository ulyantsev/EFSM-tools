package structures;

import java.util.ArrayList;
import java.util.List;

import actions.StringActions;
import bool.MyBooleanExpression;

import scenario.StringScenario;

public class Automaton {
    private final Node startState;
    private final List<Node> states;

    public Automaton(int statesCount) {
        this.startState = new Node(0);
        this.states = new ArrayList<>();
        this.states.add(startState);
        for (int i = 1; i < statesCount; i++) {
            this.states.add(new Node(i));
        }
    }

    public Node getStartState() {
        return startState;
    }

    public Node getState(int i) {
        return states.get(i);
    }

    public List<Node> getStates() {
        return states;
    }

    public int statesCount() {
        return states.size();
    }

    public void addTransition(Node state, Transition transition) {
        state.addTransition(transition.getEvent(), transition.getExpr(), transition.getActions(), transition.getDst());
    }
    
    private Node getNextNode(Node node, String event, MyBooleanExpression expr) {
        for (Transition tr : node.getTransitions()) {
            if (tr.getEvent().equals(event) && tr.getExpr() == expr) {
                return tr.getDst();
            }
        }
        return null;
    }

    private StringActions getNextActions(Node node, String event, MyBooleanExpression expr) {
        for (Transition tr : node.getTransitions()) {
            if (tr.getEvent().equals(event) && tr.getExpr() == expr) {
                return tr.getActions();
            }
        }
        return null;        
    }
    
    private Node getNext(Node node, String event, MyBooleanExpression expr, StringActions actions) {
        for (Transition tr : node.getTransitions()) {
            boolean eventsEq = tr.getEvent().equals(event);
            boolean exprEq = tr.getExpr() == expr;
            boolean actionsEq = tr.getActions().equals(actions);                                                        
            if (eventsEq && exprEq && actionsEq) {
                return tr.getDst();
            }
        }
        return null;
    }
    
    public boolean isCompliesWithScenario(StringScenario scenario) {
        Node node = startState;
        for (int pos = 0; pos < scenario.size(); pos++) {
            node = getNext(node, scenario.getEvent(pos), scenario.getExpr(pos), scenario.getActions(pos));
            if (node == null) {
                return false;
            }
        }
        return true;
    }

    public int calcMissedActions(StringScenario scenario) {
        Node node = startState;
        int missed = 0;
        for (int pos = 0; pos < scenario.size(); pos++) {
            StringActions nextActions = getNextActions(node, scenario.getEvent(pos), scenario.getExpr(pos));
            node = getNextNode(node, scenario.getEvent(pos), scenario.getExpr(pos));
            if (node == null) {
                return missed + scenario.size() - pos;
            }
            if (!scenario.getActions(pos).equals(nextActions)) {
                missed++;
            }
        }
        return missed;        
    }

    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("# generated file, don't try to modify\n"
        	+ "# command: dot -Tpng <filename> > tree.png\n"
        	+ "digraph Automaton {\n"
        	+ "    node [shape = circle];\n"
        	+ "    0 [style = \"bold\"];\n");

        for (Node state : states) {
            for (Transition t : state.getTransitions()) {
                sb.append("    " + t.getSrc().getNumber() + " -> " + t.getDst().getNumber());
                sb.append(" [label = \"" + t.getEvent() + " [" + t.getExpr().toString()
                	+ "] (" + t.getActions().toString() + ") \"];\n");
            }
        }

        sb.append("}");
        return sb.toString();
    }
}

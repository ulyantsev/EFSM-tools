package structures.mealy;

import bool.MyBooleanExpression;
import scenario.StringActions;
import scenario.StringScenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MealyAutomaton {
    private final MealyNode startState;
    private final List<MealyNode> states;

    public MealyAutomaton(int statesCount) {
        this.startState = new MealyNode(0);
        this.states = new ArrayList<>();
        this.states.add(startState);
        for (int i = 1; i < statesCount; i++) {
            this.states.add(new MealyNode(i));
        }
    }

    public MealyNode startState() {
        return startState;
    }

    public MealyNode state(int i) {
        return states.get(i);
    }

    public List<MealyNode> states() {
        return states;
    }

    public int stateCount() {
        return states.size();
    }

    public void addTransition(MealyNode state, MealyTransition transition) {
        state.addTransition(transition.event(), transition.expr(), transition.actions(), transition.dst());
    }

    private MealyNode nextNode(MealyNode node, String event, MyBooleanExpression expr) {
        for (MealyTransition tr : node.transitions()) {
            if (tr.event().equals(event) && tr.expr() == expr) {
                return tr.dst();
            }
        }
        return null;
    }

    private StringActions nextActions(MealyNode node, String event, MyBooleanExpression expr) {
        for (MealyTransition tr : node.transitions()) {
            if (tr.event().equals(event) && tr.expr() == expr) {
                return tr.actions();
            }
        }
        return null;        
    }
    
    private MealyNode next(MealyNode node, String event, MyBooleanExpression expr, StringActions actions) {
        for (MealyTransition tr : node.transitions()) {
            boolean eventsEq = tr.event().equals(event);
            boolean exprEq = tr.expr() == expr;
            boolean actionsEq = tr.actions().equals(actions);                                                        
            if (eventsEq && exprEq && actionsEq) {
                return tr.dst();
            }
        }
        return null;
    }
    
    public boolean compliesWith(StringScenario scenario) {
        MealyNode node = startState;
        for (int pos = 0; pos < scenario.size(); pos++) {
            List<MealyNode> newNodes = new ArrayList<>();
            // multi-edge support
            for (String e : scenario.getEvents(pos)) {
                MealyNode newNode = next(node, e, scenario.getExpr(pos), scenario.getActions(pos));
                if (newNode == null) {
                    return false;
                }
                newNodes.add(newNode);
            }
            node = newNodes.get(0);
            if (new HashSet<>(newNodes).size() > 1) {
                return false;
            }
        }
        return true;
    }

    public int calcMissedActions(StringScenario scenario) {
        MealyNode node = startState;
        int missed = 0;
        for (int pos = 0; pos < scenario.size(); pos++) {
            StringActions nextActions = nextActions(node, scenario.getEvents(pos).get(0), scenario.getExpr(pos));
            node = nextNode(node, scenario.getEvents(pos).get(0), scenario.getExpr(pos));
            if (node == null) {
                return missed + scenario.size() - pos;
            }
            if (!scenario.getActions(pos).equals(nextActions)) {
                missed++;
            }
        }
        return missed;        
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# generated file, don't try to modify\n"
            + "# command: dot -Tpng <filename> > tree.png\n"
            + "digraph Automaton {\n"
            + "    node [shape = circle];\n"
            + "    0 [style = \"bold\"];\n");

        for (MealyNode state : states) {
            for (MealyTransition t : state.transitions()) {
                sb.append("    " + t.src().number() + " -> " + t.dst().number());
                sb.append(" [label = \"" + t.event() + " [" + t.expr().toString()
                    + "] (" + t.actions().toString() + ") \"];\n");
            }
        }

        sb.append("}");
        return sb.toString();
    }
}

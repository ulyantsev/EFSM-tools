package algorithms;

import bool.MyBooleanExpression;
import structures.mealy.MealyAutomaton;
import structures.mealy.MealyNode;
import structures.mealy.MealyTransition;

import java.util.*;

public class AutomatonCompletenessChecker {
    public static String checkCompleteness(MealyAutomaton automaton) {
        for (MealyNode node : automaton.states()) {
            final Map<String, Set<String>> eventVars = new TreeMap<>();
            for (MealyTransition t : node.transitions()) {
                String event = t.event();
                if (!eventVars.containsKey(event)) {
                    eventVars.put(event, new TreeSet<>());
                }
                eventVars.get(event).addAll(Arrays.asList(t.expr().getVariables()));
            }

            final Map<String, Integer> eventSetsCount = new TreeMap<>();
            for (MealyTransition t : node.transitions()) {
                String event = t.event();
                MyBooleanExpression expr = t.expr();
                if (!eventSetsCount.containsKey(event)) {
                    eventSetsCount.put(event, 0);
                }

                int coefficient = 1 << (eventVars.get(event).size() - expr.getVariablesCount());
                int setsCount = expr.getSatisfiabilitySetsCount() * coefficient;
                eventSetsCount.put(event, eventSetsCount.get(event) + setsCount);
            }

            for (String event : eventSetsCount.keySet()) {
                if (1 << eventVars.get(event).size() != eventSetsCount.get(event)) {
                    return "INCOMPLETE\nNode " + node.number() + "\nEvent " + event;
                }
            }
        }
        return "COMPLETE";
    }
}

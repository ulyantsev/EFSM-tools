package tools;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import bool.MyBooleanExpression;

import structures.Automaton;
import structures.Node;
import structures.Transition;

public class AutomatonCompletenessChecker {
	public static String checkCompleteness(Automaton automaton) {
		
		for (Node node : automaton.getStates()) {
			Map<String, Set<String>> eventVars = new TreeMap<>();
			for (Transition t : node.getTransitions()) {
				String event = t.getEvent();
				if (!eventVars.containsKey(event)) {
					eventVars.put(event, new TreeSet<>());
				}
				eventVars.get(event).addAll(Arrays.asList(t.getExpr().getVariables()));				
			}

			Map<String, Integer> eventSetsCount = new TreeMap<>();
			for (Transition t : node.getTransitions()) {
				String event = t.getEvent();
				MyBooleanExpression expr = t.getExpr();
				if (!eventSetsCount.containsKey(event)) {
					eventSetsCount.put(event, 0);
				}
				
				int coefficient = 1 << (eventVars.get(event).size() - expr.getVariablesCount());
				int setsCount = expr.getSatisfiabilitySetsCount() * coefficient;
				eventSetsCount.put(event, eventSetsCount.get(event) + setsCount);
			}
			
			
			for (String event : eventSetsCount.keySet()) {
				if (1 << eventVars.get(event).size() != eventSetsCount.get(event)) {
					return "INCOMPLETE\nNode " + node.getNumber() + "\nEvent " + event;
				}
			}
		}
		return "COMPLETE";
	}
}

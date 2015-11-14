package algorithms;

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
		for (Node node : automaton.states()) {
			Map<String, Set<String>> eventVars = new TreeMap<>();
			for (Transition t : node.transitions()) {
				String event = t.event();
				if (!eventVars.containsKey(event)) {
					eventVars.put(event, new TreeSet<>());
				}
				eventVars.get(event).addAll(Arrays.asList(t.expr().getVariables()));				
			}

			Map<String, Integer> eventSetsCount = new TreeMap<>();
			for (Transition t : node.transitions()) {
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

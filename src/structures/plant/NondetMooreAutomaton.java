package structures.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import main.plant.apros.Parameter;
import scenario.StringActions;
import scenario.StringScenario;

public class NondetMooreAutomaton {
    private final List<Boolean> isInitial = new ArrayList<>();
    private final List<MooreNode> states = new ArrayList<>();
    
    // optional, for pretty output
    private Map<String, String> actionDescriptions = Collections.emptyMap();
    
    public void setActionDescriptions(Map<String, String> actionDescriptions) {
    	this.actionDescriptions = actionDescriptions;
    }
    
    // optional, for NuSMV output conversion
    private List<Pair<String, Parameter>> actionThresholds = Collections.emptyList();
    
    public void setActionThresholds(List<Pair<String, Parameter>> actionThresholds) {
    	this.actionThresholds = actionThresholds;
    }
    
    // optional, for NuSMV input conversion
    private List<Pair<String, Parameter>> eventThresholds = Collections.emptyList();
    
    public void setEventThresholds(List<Pair<String, Parameter>> eventThresholds) {
    	this.eventThresholds = eventThresholds;
    }
    
    public static NondetMooreAutomaton readGV(String filename) throws FileNotFoundException {
		final Map<String, List<String>> actionRelation = new LinkedHashMap<>();
		final Map<String, List<Pair<Integer, String>>> transitionRelation = new LinkedHashMap<>();
		actionRelation.put("init", new ArrayList<>());
		transitionRelation.put("init", new ArrayList<>());
		final Set<String> events = new LinkedHashSet<>();
		final Set<String> actions = new LinkedHashSet<>();
		final Set<Integer> initial = new LinkedHashSet<>();
		
		try (Scanner sc = new Scanner(new File(filename))) {
			while (sc.hasNextLine()) {
				final String line = sc.nextLine();
				final String tokens[] = line.split(" +");
				if (!line.contains(";")) {
					continue;
				}
				if (line.contains("->")) {
					final String from = tokens[1];
					final Integer to = Integer.parseInt(tokens[3].replaceAll(";", ""));
					if (from.equals("init")) {
						initial.add(to);
					} else {
						final String event = tokens[6].replaceAll("[;\\]\"]", "");
						transitionRelation.get(from).add(Pair.of(to, event));
						events.add(event);
					}
				} else {
					final String from = tokens[1];
					transitionRelation.put(from, new ArrayList<>());
					if (from.equals("init") || from.equals("node")) {
						continue;
					}
					final List<String> theseActions = Arrays.asList(line.split("\"")[1].split(":")[1].trim().split(", "));
					actionRelation.put(from, theseActions);
					actions.addAll(theseActions);
				}
			}
		}
		
		int maxState = 0;
		for (List<Pair<Integer, String>> list : transitionRelation.values()) {
			for (Pair<Integer, String> p : list) {
				maxState = Math.max(maxState, p.getLeft());
			}
		}
		final List<Boolean> initialVector = new ArrayList<>();
		final List<StringActions> actionVector = new ArrayList<>();
		for (int i = 0; i <= maxState; i++) {
			initialVector.add(initial.contains(i));
			actionVector.add(new StringActions(String.join(", ", actionRelation.get(i + ""))));
		}
		
		final NondetMooreAutomaton a = new NondetMooreAutomaton(maxState + 1, actionVector, initialVector);
		for (int i = 0; i <= maxState; i++) {
			for (Pair<Integer, String> p : transitionRelation.get(i + "")) {
				a.state(i).addTransition(p.getRight(), a.state(p.getLeft()));
			}
		}
		return a;
	}
	
    public NondetMooreAutomaton(List<MooreNode> states, List<Boolean> isStart) {
    	this.states.addAll(states);
        this.isInitial.addAll(isStart);
    }
    
    public NondetMooreAutomaton(int statesCount, List<StringActions> actions, List<Boolean> isInitial) {
        for (int i = 0; i < statesCount; i++) {
            states.add(new MooreNode(i, actions.get(i)));
        }
        this.isInitial.addAll(isInitial);
    }

    public boolean isInitialState(int index) {
        return isInitial.get(index);
    }
    
    public List<Integer> initialStates() {
    	final List<Integer> result = new ArrayList<>();
    	for (int i = 0; i < states.size(); i++) {
    		if (isInitialState(i)) {
    			result.add(i);
    		}
    	}
        return result;
    }

    public MooreNode state(int i) {
        return states.get(i);
    }

    public List<MooreNode> states() {
        return states;
    }

    public int stateCount() {
        return states.size();
    }

    public void addTransition(MooreNode state, MooreTransition transition) {
        state.addTransition(transition);
    }
    
    public void removeTransition(MooreNode state, MooreTransition transition) {
        state.removeTransition(transition);
    }

    public String toString(Map<String, String> colorRules) {
    	final StringBuilder sb = new StringBuilder();
    	sb.append("# generated file; view: dot -Tpng <filename> > filename.png\n"
        	+ "digraph Automaton {\n");
    	
    	final String initNodes = String.join(", ", initialStates().stream().map(s -> "init" + s).collect(Collectors.toList()));
    	
		sb.append("    " + initNodes + " [shape=point, width=0.01, height=0.01, label=\"\", color=white];\n");
		sb.append("    node [shape=circle];\n");
    	for (int i = 0; i < states.size(); i++) {
    		final MooreNode state = states.get(i);
            String color = "";
            for (String action : state.actions().getActions()) {
            	final String col = colorRules.get(action);
            	if (col != null) {
            		color = " style=filled fillcolor=\"" + col + "\"";
            	}
            }
            
    		sb.append("    " + state.number() + " [label=\""
    				+ state.toString(actionDescriptions) + "\"" + color + "]" + ";\n");
    		if (isInitial.get(i)) {
    			sb.append("    init" + state.number() + " -> " + state.number() + ";\n");
    		}
    	}
    	
        for (MooreNode state : states) {
            for (MooreTransition t : state.transitions()) {
            	sb.append("    " + t.src().number() + " -> " + t.dst().number()
                		+ " [label=\" " + t.event() + " \"];\n");
            }
        }

        sb.append("}");
        return sb.toString();
    }
    
    @Override
    public String toString() {
    	return toString(Collections.emptyMap());
    }

    private static void nusmvEventDescriptions(int[] arr, int index, StringBuilder result,
    		List<Pair<String, Parameter>> thresholds, List<String> events) {
		if (index == arr.length) {
			final String event = "input_A" + Arrays.toString(arr).replaceAll("[,\\[\\] ]", "");
			if (!events.contains(event)) {
				return;
			}
			final List<String> conditions = new ArrayList<>();
			for (int i = 0; i < arr.length; i++) {
				final String paramName = thresholds.get(i).getLeft();
				final Parameter param = thresholds.get(i).getRight();
				conditions.add(param.nusmvCondition("CONT_INPUT_" + paramName, arr[i]));
			}
			result.append("    " + event + " := " + String.join(" & ", conditions) + ";\n");
		} else {
			final int intervalNum = thresholds.get(index).getRight().valueCount();
			for (int i = 0; i < intervalNum; i++) {
				arr[index] = i;
				nusmvEventDescriptions(arr, index + 1, result, thresholds, events);
			}
		}
	}
    
    public String toNuSMVString(List<String> events, List<String> actions) {
    	events = events.stream().map(s -> "input_" + s).collect(Collectors.toList());
    	final String inputLine = String.join(", ",
    			eventThresholds.stream().map(t -> "CONT_INPUT_" + t.getKey())
    			.collect(Collectors.toList()));
    	final StringBuilder sb = new StringBuilder();
    	sb.append("MODULE main()\n");
    	sb.append("VAR\n");
    	sb.append("    plant: PLANT(" + inputLine + ");\n");
		for (Pair<String, Parameter> entry : eventThresholds) {
			final String paramName = entry.getLeft();
			final Parameter param = entry.getRight();
			sb.append("    CONT_INPUT_" + paramName + ": " + param.nusmvType()
					+ ";\n");
		}
    	
    	sb.append("\n");
    	sb.append("MODULE PLANT(" + inputLine + ")\n");
    	sb.append("VAR\n");
    	sb.append("    state: 0.." + (stateCount() - 1) + ";\n");    	
    	sb.append("INIT\n");
    	sb.append("    state in { " + initialStates().toString().replace("[", "").replace("]", "") + " }\n");
    	sb.append("TRANS\n");
    	// if the output is known, then the next state is constrained, otherwise it is free
    	sb.append("    (next(" + String.join(") | next(", events) + ")) -> \n");
    	final List<String> stateConstraints = new ArrayList<>();
    	for (int i = 0; i < stateCount(); i++) {
        	final List<String> options = new ArrayList<>();
    		for (String event : events) {
    			final List<Integer> destinations = new ArrayList<>();
    			for (MooreTransition t : states.get(i).transitions()) {
        			if (("input_" + t.event()).equals(event)) {
        				destinations.add(t.dst().number());
        			}
        		}
    			Collections.sort(destinations);
    			options.add("next(" + event + ") & next(state) in { "
    					+ destinations.toString().replace("[", "").replace("]", "") + " }");
    		}
    		stateConstraints.add("state = " + i + " -> (\n      " + String.join("\n    | ", options));
    	}
    	sb.append("    (" + String.join("\n    )) & (", stateConstraints) + "))\n");
    	sb.append("DEFINE\n");
    	for (String action : actions) {
    		final List<String> properStates = new ArrayList<>();
    		for (int i = 0; i < stateCount(); i++) {
    			if (ArrayUtils.contains(states.get(i).actions().getActions(), action)) {
    				properStates.add(String.valueOf(i));
    			}
    		}
    		final String condition = properStates.isEmpty()
    				? "FALSE"
    				: ("state in { " + String.join(", ", properStates) + " }");
    		final String comment = actionDescriptions.containsKey(action)
    				? (" -- " + actionDescriptions.get(action)) : "";
    		sb.append("    output_" + action + " := " + condition + ";" + comment + "\n");
    	}

    	// output conversion to continuous values
    	for (Pair<String, Parameter> entry : actionThresholds) {
    		final String paramName = entry.getKey();
    		final Parameter param = entry.getValue();
    		sb.append("    CONT_" + paramName + " := case\n");
    		for (int i = 0; i < param.valueCount(); i++) {
    			sb.append("        output_" + paramName + i + ": "
    					+ param.nusmvInterval(i) + ";\n");
    		}
    		sb.append("    esac;\n");
    	}
    	// input conversion to discrete values
		nusmvEventDescriptions(new int[eventThresholds.size()], 0, sb, eventThresholds, events);

    	return sb.toString();
    }
    
    public boolean isCompliantWithScenarios(List<StringScenario> scenarios, boolean positive) {
    	for (StringScenario sc : scenarios) {
        	boolean[] curStates = new boolean[states.size()];
        	final StringActions firstActions = sc.getActions(0);
    		for (int i = 0; i < states.size(); i++) {
    			if (isInitialState(i) && states.get(i).actions().setEquals(firstActions)) {
    				curStates[i] = true;
    			}
    		}
    		for (int i = 1; i < sc.size(); i++) {
    			final String event = sc.getEvents(i).get(0);
    			final StringActions actions = sc.getActions(i);
    			boolean[] newStates = new boolean[states.size()];
    			for (int j = 0; j < states.size(); j++) {
    				if (curStates[j]) {
    					for (MooreNode dst : states.get(j).allDst(event)) {
    						if (dst.actions().setEquals(actions)) {
    							newStates[dst.number()] = true;
    						}
    					}
    				}
    			}
    			curStates = newStates;
    		}
    		final boolean passed = ArrayUtils.contains(curStates, true);
    		if (passed != positive) {
    			return false;
    		}
    	}
    	return true;
    }
    
    public NondetMooreAutomaton copy() {
    	final List<StringActions> actions = new ArrayList<>();
    	for (MooreNode state : states) {
			actions.add(state.actions());
    	}

		final NondetMooreAutomaton copy = new NondetMooreAutomaton(states.size(), actions,
				new ArrayList<>(this.isInitial));

		for (MooreNode state : states) {
			final MooreNode src = copy.state(state.number());
			for (MooreTransition t : state.transitions()) {
				src.addTransition(t.event(), copy.state(t.dst().number()));
			}
		}
		
		copy.actionDescriptions = actionDescriptions;
		copy.actionThresholds = actionThresholds;
		copy.eventThresholds = eventThresholds;

		return copy;
    }
    
    public void removeDeadlocks() {
    	Map<MooreNode, Set<MooreTransition>> reversedTransitions = null;
    	final Set<MooreNode> allDeadlockStates = new HashSet<>();
    	while (true) {
	    	final Set<MooreNode> deadlockStates = new HashSet<>();
	    	// initial deadlock states
	    	for (MooreNode state : states) {
	    		if (state.transitions().isEmpty()) {
	    			deadlockStates.add(state);
	    		}
	    	}
	    	if (!allDeadlockStates.addAll(deadlockStates)) {
	    		return;
	    	}
	    	final Deque<MooreNode> unprocessedNodes = new ArrayDeque<>(deadlockStates);

	    	if (reversedTransitions == null) {
		    	reversedTransitions = new HashMap<>();
		    	for (MooreNode state : states) {
		    		reversedTransitions.put(state, new HashSet<>());
		    	}
		    	for (MooreNode state : states) {
		    		for (MooreTransition t : state.transitions()) {
		    			reversedTransitions.get(t.dst()).add(t);
		    		}
		    	}
	    	}
	    	while (!unprocessedNodes.isEmpty()) {
	    		final MooreNode node = unprocessedNodes.pollFirst();
	    		final List<MooreTransition> trans = new ArrayList<>(reversedTransitions.get(node));
	    		for (MooreTransition t : trans) {
	    			t.src().removeTransition(t);
	    		}
	    		for (MooreTransition t : trans) {
	    			if (t.src().transitionsCount() == 0) {
	    				deadlockStates.add(t.src());
	    				if (!allDeadlockStates.add(t.src())) {
	    					unprocessedNodes.add(t.src());
	    				}
	    			}
	    		}
	    	}
	    	
	    	for (MooreNode node : deadlockStates) {
	    		isInitial.set(node.number(), false);
	    	}
    	}
    }
}

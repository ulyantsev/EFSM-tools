package structures.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import main.plant.apros.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import main.plant.apros.Parameter;
import main.plant.apros.TraceModelGenerator;
import scenario.StringActions;
import scenario.StringScenario;

public class NondetMooreAutomaton {
    private final List<Boolean> isInitial = new ArrayList<>();
    private final List<MooreNode> states = new ArrayList<>();
    
    private Set<MooreTransition> unsupportedTransitions = new HashSet<>();

    public Set<MooreTransition> unsupportedTransitions() {
        return unsupportedTransitions;
    }

    public double transitionFraction(Predicate<MooreTransition> p) {
        int matched = 0;
        int all = 0;
        for (MooreNode state : states) {
            for (MooreTransition t : state.transitions()) {
                all++;
                if (p.test(t)) {
                    matched++;
                }
            }
        }
        return (double) matched / all;
    }

    public double unsupportedTransitionFraction() {
        return transitionFraction(unsupportedTransitions::contains);
    }

    public double loopFraction() {
        return transitionFraction(t -> t.dst() == t.src());
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
	
    public NondetMooreAutomaton(List<MooreNode> states, List<Boolean> isInitial) {
    	this.states.addAll(states);
        this.isInitial.addAll(isInitial);
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

    public String toString(Map<String, String> colorRules, Optional<Configuration> conf) {
    	final StringBuilder sb = new StringBuilder();
    	sb.append("# generated file; view: dot -Tpng <filename> > filename.png\n"
        	+ "digraph Automaton {\n");

        final Map<String, String> actionDescriptions = conf.isPresent()
                ? conf.get().extendedActionDescriptions() : new HashMap<>();

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
        return toString(Collections.emptyMap(), Optional.empty());
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
    
    public String toNuSMVString(List<String> events, List<String> actions,
                                List<List<Integer>> generatedStateSets,
                                Optional<Configuration> conf) {
    	final List<String> unmodifiedEvents = events;
    	events = events.stream().map(s -> "input_" + s).collect(Collectors.toList());
    	final List<Pair<String, Parameter>> eventThresholds = conf.isPresent()
                ? conf.get().eventThresholds() : new ArrayList<>();
        final List<Pair<String, Parameter>> actionThresholds = conf.isPresent()
                ? conf.get().actionThresholds() : new ArrayList<>();
        final Map<String, String> actionDescriptions = conf.isPresent()
                ? conf.get().extendedActionDescriptions() : new HashMap<>();
        final String inputLine = String.join(", ",
    			eventThresholds.stream().map(t -> "CONT_INPUT_" + t.getKey())
    			.collect(Collectors.toList()));
    	final StringBuilder sb = new StringBuilder();
    	/*sb.append("MODULE main()\n");
    	sb.append("VAR\n");
    	sb.append("    plant: PLANT(" + inputLine + ");\n");
		for (Pair<String, Parameter> entry : eventThresholds) {
			final String paramName = entry.getLeft();
			final Parameter param = entry.getRight();
			sb.append("    CONT_INPUT_" + paramName + ": " + param.nusmvType()
					+ ";\n");
		}
    	
    	sb.append("\n");*/
    	sb.append("MODULE PLANT(" + inputLine + ")\n");
    	sb.append("VAR\n");
    	sb.append("    unsupported: boolean;\n");
    	sb.append("    loop_executed: boolean;\n");
    	sb.append("    state: 0.." + (stateCount() - 1) + ";\n");    	
    	sb.append("INIT\n");
    	sb.append("    state in " + TraceModelGenerator.expressWithIntervals(initialStates()) + "\n");
    	generatedStateSets.add(initialStates());
    	sb.append("TRANS\n");
    	
    	final List<String> stateConstraints = new ArrayList<>();
    	for (int i = 0; i < stateCount(); i++) {
        	final List<String> options = new ArrayList<>();
        	final Map<List<Integer>, Set<String>> map = new LinkedHashMap<>();
    		for (String event : events) {
    			final List<Integer> destinations = new ArrayList<>();
    			for (MooreTransition t : states.get(i).transitions()) {
        			if (("input_" + t.event()).equals(event)) {
        				destinations.add(t.dst().number());
        			}
        		}
    			Collections.sort(destinations);
    			Set<String> correspondingEvents = map.get(destinations);
    			if (correspondingEvents == null) {
    				correspondingEvents = new TreeSet<>();
    				map.put(destinations, correspondingEvents);
    			}
    			correspondingEvents.add("next(" + event + ")");
    		}
        	final Set<Integer> allSuccStates = new TreeSet<>();
        	for (MooreTransition t : states.get(i).transitions()) {
        		allSuccStates.add(t.dst().number());
        	}
        	{
            	// if the input is unknown, then the choice for the next state is wide
	        	final List<Integer> allSuccStatesList = new ArrayList<>(allSuccStates);
	        	Set<String> correspondingEvents = map.get(allSuccStatesList);
				if (correspondingEvents == null) {
					correspondingEvents = new TreeSet<>();
					map.put(allSuccStatesList, correspondingEvents);
				}
				correspondingEvents.add("!next(known_input)");
        	}
    		for (Map.Entry<List<Integer>, Set<String>> entry : map.entrySet()) {
    			final List<Integer> destinations = entry.getKey();
    			final Set<String> correspondingEvents = entry.getValue();
    			
    			options.add("(" + String.join(" | ", correspondingEvents) + ") & next(state) in "
    					+ TraceModelGenerator.expressWithIntervals(destinations));
    	    	generatedStateSets.add(destinations);
    		}

    		stateConstraints.add("state = " + i + " -> (\n      " + String.join("\n    | ", options));
    	}
    	sb.append("    (" + String.join("\n    )) & (", stateConstraints) + "))\n");
    	
    	// marking that there was a transition unsupported by traces
    	sb.append("ASSIGN\n");
    	sb.append("    init(unsupported) := FALSE;\n");
    	final List<String> unsupported = new ArrayList<>();
    	unsupported.add("unsupported | !next(known_input)");
    	for (String e : unmodifiedEvents) {
    		final Set<Integer> sourceStates = new TreeSet<>();
    		for (int i = 0; i < stateCount(); i++) {
    			for (MooreTransition t : states.get(i).transitions()) {
    				if (t.event().equals(e) && unsupportedTransitions.contains(t)) {
    					sourceStates.add(i);
    					break;
    				}
    			}
    		}
    		if (!sourceStates.isEmpty()) {
    			unsupported.add("input_" + e + " & state in "
    					+ TraceModelGenerator.expressWithIntervals(sourceStates));
    			generatedStateSets.add(new ArrayList<>(sourceStates));
    		}	
    	}
    	
    	sb.append("    next(unsupported) := " + String.join("\n        | ", unsupported) + ";\n");
    	sb.append("    init(loop_executed) := FALSE;\n");
    	sb.append("    next(loop_executed) := state = next(state);\n");
        sb.append("DEFINE\n");
    	sb.append("    known_input := " + String.join(" | ", events) + ";\n");

    	for (String action : actions) {
    		final List<Integer> properStates = new ArrayList<>();
    		for (int i = 0; i < stateCount(); i++) {
    			if (ArrayUtils.contains(states.get(i).actions().getActions(), action)) {
    				properStates.add(i);
    			}
    		}
    		final String condition = properStates.isEmpty()
    				? "FALSE"
    				: ("state in " + TraceModelGenerator.expressWithIntervals(properStates));
    		generatedStateSets.add(properStates);
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
    
    public boolean isCompliantWithScenarios(List<StringScenario> scenarios, boolean positive,
                                            boolean markUnsupportedTransitions) {
    	final Set<MooreTransition> supported = new HashSet<>();

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
    			final boolean[] newStates = new boolean[states.size()];
    			for (int j = 0; j < states.size(); j++) {
    				if (curStates[j]) {
                        for (MooreTransition t : states.get(j).transitions()) {
                            if (t.event().equals(event) && t.dst().actions().setEquals(actions)) {
                                newStates[t.dst().number()] = true;
                                supported.add(t);
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

        if (markUnsupportedTransitions) {
            for (MooreNode state : states) {
                for (MooreTransition t : state.transitions()) {
                    if (!supported.contains(t)) {
                        unsupportedTransitions.add(t);
                    }
                }
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
                final MooreTransition tNew
                        = new MooreTransition(src, copy.state(t.dst().number()), t.event());
                src.addTransition(tNew);
                if (unsupportedTransitions.contains(t)) {
                    copy.unsupportedTransitions.add(tNew);
                }
            }
        }

		return copy;
    }
    
    public NondetMooreAutomaton swapStates(int[] permutation) {
		final NondetMooreAutomaton copy = copy();
		
		for (int i = 0; i < isInitial.size(); i++) {
			copy.isInitial.set(i, isInitial.get(permutation[i]));
		}
		
		copy.states.clear();
		for (int i = 0; i < states.size(); i++) {
			copy.states.add(new MooreNode(i, states.get(permutation[i]).actions()));
		}
	
		for (MooreNode state : states) {
			final MooreNode src = copy.state(permutation[state.number()]);
			for (MooreTransition t : state.transitions()) {
				final MooreNode dst = copy.state(permutation[t.dst().number()]);				
				final MooreTransition tNew = new MooreTransition(src, dst, t.event());
				src.addTransition(tNew);
				if (unsupportedTransitions.contains(t)) {
					copy.unsupportedTransitions.add(tNew);
				}
			}
		}
		
		return copy;
    }

    public NondetMooreAutomaton simplify() {
        final NondetMooreAutomaton copy = copy();

        for (MooreNode state : states) {
            final Set<Integer> destinations = state.transitions().stream()
                    .map(MooreTransition::dst).map(MooreNode::number)
                    .collect(Collectors.toCollection(TreeSet::new));
            final MooreNode copyState = copy.state(state.number());
            new HashSet<>(copyState.transitions()).forEach(copyState::removeTransition);
            for (Integer dst : destinations) {
                copyState.addTransition(" ", copy.state(dst));
            }
        }

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

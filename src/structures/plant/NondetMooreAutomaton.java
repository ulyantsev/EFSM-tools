package structures.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import scenario.StringActions;
import scenario.StringScenario;

public class NondetMooreAutomaton {
    private final List<Boolean> isStart = new ArrayList<>();
    private final List<MooreNode> states = new ArrayList<>();

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
	
    public NondetMooreAutomaton(int statesCount, List<StringActions> actions, List<Boolean> isStart) {
        for (int i = 0; i < statesCount; i++) {
            states.add(new MooreNode(i, actions.get(i)));
        }
        this.isStart.addAll(isStart);
    }

    public boolean isStartState(int index) {
        return isStart.get(index);
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
        state.addTransition(transition.event(), transition.dst());
    }
    
    public void removeTransition(MooreNode state, MooreTransition transition) {
        state.removeTransition(transition);
    }
    
    @Override
    public String toString() {
    	final StringBuilder sb = new StringBuilder();
    	sb.append("# generated file\n"
        	+ "# command: dot -Tpng <filename> > filename.png\n"
        	+ "digraph Automaton {\n");
    	
		sb.append("    init [shape = circle, width=0.1, height=0.1, label=\" \"];\n");
		sb.append("    node [fixedsize=true, width=1.8, height=1.8];\n");
    	for (int i = 0; i < states.size(); i++) {
    		final MooreNode state = states.get(i);
    		sb.append("    " + state.number() + " [label = \"" + state + "\"] [shape=circle]" + ";\n");
    		if (isStart.get(i)) {
    			sb.append("    init -> " + state.number() + ";\n");
    		}
    	}
    	
        for (MooreNode state : states) {
            for (MooreTransition t : state.transitions()) {
                sb.append("    " + t.src().number() + " -> " + t.dst().number()
                		+ " [label = \"" + t.event() + "\"];\n");
            }
        }

        sb.append("}");
        return sb.toString();
    }
    
    private boolean recursiveScenarioCompliance(MooreNode scenarioNode, MooreNode automatonNode) {
    	if (!scenarioNode.actions().setEquals(automatonNode.actions())) {
    		return false;
    	}
    	if (scenarioNode.transitions().isEmpty()) {
    		return true;
    	}
    	final MooreTransition scenarioTransition = scenarioNode.transitions().iterator().next();
    	final String scenarioEvent = scenarioTransition.event();
    	final MooreNode scenarioDst = scenarioTransition.dst();
    	for (MooreTransition automatonTransition : automatonNode.transitions()) {
    		if (automatonTransition.event().equals(scenarioEvent)) {
    			if (recursiveScenarioCompliance(scenarioDst, automatonTransition.dst())) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public boolean isCompliantWithScenarios(PositivePlantScenarioForest forest) {
    	for (MooreNode root : forest.roots()) {
    		boolean complies = false;
    		for (int i = 0; i < states.size(); i++) {
    			if (isStartState(i) && recursiveScenarioCompliance(root, states.get(i))) {
    				complies = true;
    				break;
    			}
    		}
    		if (!complies) {
    			return false;
    		}
    	}
		return true;
    }
    
    public boolean isCompliantWithNegativeScenarios(List<StringScenario> scenarios) {
    	for (StringScenario sc : scenarios) {
        	boolean[] curStates = new boolean[states.size()];
        	final StringActions firstActions = sc.getActions(0);
    		for (int i = 0; i < states.size(); i++) {
    			if (isStartState(i) && states.get(i).actions().setEquals(firstActions)) {
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
    		if (Arrays.asList(curStates).contains(true)) {
    			return false;
    		}
    	}
    	return true;
    }
}

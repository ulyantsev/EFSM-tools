package structures.plant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scenario.StringActions;
import scenario.StringScenario;

public class NondetMooreAutomaton {
    private final List<Boolean> isStart = new ArrayList<>();
    private final List<MooreNode> states = new ArrayList<>();

    public NondetMooreAutomaton(int statesCount, List<StringActions> actions, List<Boolean> isStart) {
        for (int i = 0; i < statesCount; i++) {
            states.add(new MooreNode(i, actions.get(i)));
            this.isStart.add(isStart.get(i));
        }
    }

    public boolean isStartState(int index) {
        return isStart.get(index);
    }

    public MooreNode getState(int i) {
        return states.get(i);
    }

    public List<MooreNode> getStates() {
        return states;
    }

    public int statesCount() {
        return states.size();
    }

    public void addTransition(MooreNode state, MooreTransition transition) {
        state.addTransition(transition.getEvent(), transition.getDst());
    }
    
    public void removeTransition(MooreNode state, MooreTransition transition) {
        state.removeTransition(transition);
    }

    @Override
    public String toString() {
    	StringBuilder sb = new StringBuilder();
    	sb.append("# generated file\n"
        	+ "# command: dot -Tpng <filename> > filename.png\n"
        	+ "digraph Automaton {\n");
    	
		sb.append("    init [shape = circle] [size=0.2] [label=\" \"];\n");
    	for (int i = 0; i < states.size(); i++) {
    		final MooreNode state = states.get(i);
    		sb.append("    " + state.getNumber() + " [label = \"" + state + "\"] [shape=circle]" + ";\n");
    		if (isStart.get(i)) {
    			sb.append("    init -> " + state.getNumber() + ";\n");
    		}
    	}
    	
        for (MooreNode state : states) {
            for (MooreTransition t : state.getTransitions()) {
                sb.append("    " + t.getSrc().getNumber() + " -> " + t.getDst().getNumber()
                		+ " [label = \"" + t.getEvent() + "\"];\n");
            }
        }

        sb.append("}");
        return sb.toString();
    }
    
    private boolean recursiveScenarioCompliance(MooreNode scenarioNode, MooreNode automatonNode) {
    	if (!scenarioNode.getActions().setEquals(automatonNode.getActions())) {
    		return false;
    	}
    	if (scenarioNode.getTransitions().isEmpty()) {
    		return true;
    	}
    	final MooreTransition scenarioTransition = scenarioNode.getTransitions().iterator().next();
    	final String scenarioEvent = scenarioTransition.getEvent();
    	final MooreNode scenarioDst = scenarioTransition.getDst();
    	for (MooreTransition automatonTransition : automatonNode.getTransitions()) {
    		if (automatonTransition.getEvent().equals(scenarioEvent)) {
    			if (recursiveScenarioCompliance(scenarioDst, automatonTransition.getDst())) {
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    public boolean isCompliantWithScenarios(PositivePlantScenarioForest forest) {
    	for (MooreNode root : forest.getRoots()) {
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
    			if (isStartState(i) && states.get(i).getActions().setEquals(firstActions)) {
    				curStates[i] = true;
    			}
    		}
    		for (int i = 1; i < sc.size(); i++) {
    			final String event = sc.getEvents(i).get(0);
    			final StringActions actions = sc.getActions(i);
    			boolean[] newStates = new boolean[states.size()];
    			for (int j = 0; j < states.size(); j++) {
    				if (curStates[j]) {
    					for (MooreNode dst : states.get(j).getAllDst(event)) {
    						if (dst.getActions().setEquals(actions)) {
    							newStates[dst.getNumber()] = true;
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

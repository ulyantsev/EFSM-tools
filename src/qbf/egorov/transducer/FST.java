package qbf.egorov.transducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import structures.Automaton;
import structures.Node;

public class FST {
	private int initialState;
	private final int stateNumber;
	private Transition[][] states;
	
	private final String[] setOfInputs;
	private final String[] setOfOutputs;

	public FST(Automaton ulyantsevAutomaton, Set<String> events, Set<String> actions, int numberOfStates) {
		this.stateNumber = numberOfStates;
		initialState = ulyantsevAutomaton.getStartState().getNumber();
		
		List<List<Transition>> transitions = new ArrayList<>();
		for (int i = 0; i < numberOfStates; i++) {
			transitions.add(new ArrayList<>());
		}
		
		for (Node state : ulyantsevAutomaton.getStates()) {
			for (structures.Transition t : state.getTransitions()) {
				Transition tr = new Transition(t.getEvent(), t.getActions().size(), t.getDst().getNumber());
				tr.setOutput(t.getActions().getActions());
				transitions.get(state.getNumber()).add(tr);
			}
		}
		
		states = new Transition[stateNumber][];
		
		for (int i = 0; i < transitions.size(); i++) {
			states[i] = new Transition[transitions.get(i).size()];
			for (int j = 0; j < states[i].length; j++) {
				states[i][j] = transitions.get(i).get(j);
			}
		}
		
		this.setOfInputs = events.toArray(new String[0]);
		this.setOfOutputs = actions.toArray(new String[0]);
	}
	
	public String[] getSetOfInputs() {
		return setOfInputs;
	}
	
	public String[] getSetOfOutputs() {
		return setOfOutputs;
	}
	
	public int getNumberOfStates() {
		return stateNumber;
	}

    public Transition[][] getStates() {
        return states;
    }

    public int getInitialState() {
        return initialState;
    }
    
    /**
     * Get all transitions count
     * @return transition count
     */
	public int getTransitionsCount() {
		return Arrays.stream(states).mapToInt(s -> s.length).sum();
	}

    /**
     * Get reached transitions count
     * @return reached transitions count
     */
    public int getUsedTransitionsCount() {
        return getUsedTransitionsCount(initialState, new boolean[states.length]);
    }

    private int getUsedTransitionsCount(int state, boolean[] vizited) {
        vizited[state] = true;
        int res = states[state].length;
        for (Transition t : states[state]) {
            if (!vizited[t.getNewState()]) {
                res += getUsedTransitionsCount(t.getNewState(), vizited);
            }
        }
        return res;
    }

    public Transition getTransition(int state, String input) {
    	for (Transition t : states[state]) {
    		if (t.getInput().equals(input)) {
    			return t;
    		}
    	}
    	return null;
    }
	
	@Override
	public String toString() {
		String a = stateNumber + " " + setOfInputs.length + "\n";
		for (int i = 0; i < stateNumber; i++) {
			a += 1;
		}
		a += "\n";
		for (int i = 0; i < stateNumber; i++) {
			for (int j = 0; j < states[i].length; j++) {
				StringBuilder output = new StringBuilder();
				
				if (states[i][j].getOutput() == null) {
					output = null;
				} else {
					if (states[i][j].getOutput().length > 0) {
						for (String action : states[i][j].getOutput()) {
							output.append(action);
						}
					}
				}
				a += "(" + i + ", " + j + ") - > " + "(" + states[i][j].getNewState() + ", " + output + ")\n";
			}
		}
		return a;
	}
}

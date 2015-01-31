package qbf.egorov.transducer.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

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
				events.add(t.getEvent());
				for (String action : t.getActions().getActions()) {
					actions.add(action);
				}
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
	
	public String[] transform(String[] inputSequence) {
		ArrayList<String> list = new ArrayList<String>();
		int currentState = initialState;
		for (String s : inputSequence) {
			boolean found = false;
			for (Transition t : states[currentState]) {
				if (t.accepts(s)) {
					for (String s1 : t.getOutput()) {
						list.add(s1);
					}
					currentState = t.getNewState();
					found = true;
					t.markUsed();
					break;
				}
			}
			if (!found) {
				if (list.size() > 0) {
					return list.toArray(new String[0]);
				}
				return null;
			}
		}
		return list.toArray(new String[0]);
	}
	
    public boolean validateNegativeTest(String[] inputSequence) {
        if (ArrayUtils.isEmpty(inputSequence)) {
            throw new IllegalArgumentException("Unexpected inputSequence");
        }
        int currentState = initialState;
        Transition lastTransition = null;
        for (String s : inputSequence) {
            lastTransition = null;
            for (Transition t : states[currentState]) {
                if (t.accepts(s)) {
                    currentState = t.getNewState();
                    lastTransition = t;
                    break;
                }
            }
            if (lastTransition == null) {
                break;
            }
        }    
        if (lastTransition == null) {
            return true;
        } else {
            lastTransition.setUsedByNegativeTest(true);
            return false;
        }
    }
    
    public void unmarkAllTransitions() {
        for (Transition[] s : states) {
            for (Transition t : s) {
                t.markUnused();
                t.setUsedByNegativeTest(false);
                t.setUsedByVerifier(false);
                t.setVerified(false);
            }
        }
    }
    
    public Long computeStringHash() {
        return Digest.RSHash(stringForHashing());
    }
    
    public Transition getTransition(int state, String input) {
    	for (Transition t : states[state]) {
    		if (t.getInput().equals(input)) {
    			return t;
    		}
    	}
    	return null;
    }
    
    public void transformUsedTransitions(int[] newId1, int[] newId2, FST originalFST) {
    	int oldId2[] = new int[states.length];
    	Arrays.fill(oldId2, -1);
    	for (int i = 0; i < newId2.length; i++) {
    		if (newId2[i] != -1) {
    			oldId2[newId2[i]] = i;
    		}
    	}
    	
    	int f12[] = new int[states.length];
    	Arrays.fill(f12, -1);
    	for (int i = 0; i < f12.length; i++) {
    		if (newId1[i] == -1) {
    		} else {
    			f12[i] = oldId2[newId1[i]];
    		}
    	}
    	
    	unmarkAllTransitions();
    	for (int i = 0; i < states.length; i++) {
    		if (f12[i] != -1) {
    			for (Transition t : originalFST.states[i]) {
    				Transition transition = getTransition(f12[i], t.getInput());
    				transition.used = t.used;
    				transition.setOutput(t.getOutput());
    			}
    		}
    	}

    }

	public String stringForHashing() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < stateNumber; i++) {
			for (int j = 0; j < states[i].length; j++) {
				Transition t = states[i][j];
				sb.append(i);
				sb.append(t.getInput());
				sb.append(t.getNewState());
				sb.append(t.getOutputSize());
			}
		}
		return sb.toString();
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
	
	public String printWithUsed() {
		String a = stateNumber + " " + setOfInputs.length + "\n";
		for (int i = 0; i < stateNumber; i++) {
			a += 1;
		}
		a += "\n";
		for (int i = 0; i < stateNumber; i++) {
			for (int j = 0; j < states[i].length; j++) {
				String output = "";

				if (states[i][j].getOutput() == null) {
					output = null;
				} else {
					output = (states[i][j].getOutput().length == 0 ? "" : states[i][j].getOutput()[0]) + " (" + states[i][j].getOutputSize() + ")";
				}
				a += "(" + i + ", " + j + ") - > " + "(" + states[i][j].getNewState() + ", " + output + "; used=" + states[i][j].used + ")\n";
			}
		}
		return a;
	}
}

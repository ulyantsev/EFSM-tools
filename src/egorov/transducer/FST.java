package egorov.transducer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import structures.Automaton;
import structures.Node;

public class FST {
	private int initialState;
	private final Transition[][] states;
	
	public FST(Automaton ulyantsevAutomaton) {
		states = new Transition[ulyantsevAutomaton.statesCount()][];
		initialState = ulyantsevAutomaton.getStartState().getNumber();
		
		final List<List<Transition>> transitions = new ArrayList<>();
		for (int i = 0; i < states.length; i++) {
			transitions.add(new ArrayList<>());
		}
		
		for (Node state : ulyantsevAutomaton.getStates()) {
			for (structures.Transition t : state.getTransitions()) {
				final Transition tr = new Transition(t.getEvent(), t.getDst().getNumber(), t.getActions().getActions());
				transitions.get(state.getNumber()).add(tr);
			}
		}
		
		for (int i = 0; i < states.length; i++) {
			states[i] = transitions.get(i).toArray(new Transition[transitions.get(i).size()]);
		}
	}
	
    public Transition[][] states() {
        return states;
    }

    public int initialState() {
        return initialState;
    }
    
	@Override
	public String toString() {
		return initialState + " " + Arrays.deepToString(states);
	}
}

package algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import qbf.reduction.Verifier;
import structures.Automaton;
import structures.Node;
import structures.Transition;
import actions.StringActions;
import algorithms.FormulaBuilder.EventExpressionPair;

public class AutomatonCompleter {
	private final Verifier verifier;
	private final Automaton automaton;
	private final int colorSize;
	private final List<EventExpressionPair> efPairs;
	private final List<StringActions> preparedActions = new ArrayList<>();
	
	/*
	 * The automaton should be verified!
	 */
	public AutomatonCompleter(Verifier verifier, Automaton automaton, List<EventExpressionPair> efPairs, List<String> actions) {
		this.verifier = verifier;
		this.automaton = automaton;
		colorSize = automaton.statesCount();
		this.efPairs = efPairs;
		
		// prepare all action combinations (will be used while trying to enforce FSM completeness)
		final int actionsNum = actions.size();
		assert actionsNum <= 20;
		final int maxI = 1 << actionsNum;
		for (int i = 0; i < maxI; i++) {
			final List<String> sequence = new ArrayList<>();
			for (int j = 0; j < actionsNum; j++) {
				if (((i >> j) & 1) == 1) {
					sequence.add(actions.get(j));
				}
			}
			preparedActions.add(new StringActions(String.join(",", sequence)));
		}
	}
	
	public static Set<Pair<Integer, EventExpressionPair>> missingTransitions(Automaton automaton, List<EventExpressionPair> efPairs) {
		final List<Pair<Integer, EventExpressionPair>> missing = new ArrayList<>();
		for (Node s : automaton.getStates()) {
			for (EventExpressionPair p : efPairs) {
				if (s.getTransition(p.event, p.expression) == null) {
					missing.add(Pair.of(s.getNumber(), p));
				}
			}
		}
		return new HashSet<>(missing);
	}
	
	public void ensureCompleteness() throws AutomatonFound {
		ensureCompleteness(missingTransitions(automaton, efPairs));
	}
	
	private void ensureCompleteness(Set<Pair<Integer, EventExpressionPair>> missingTransitions) throws AutomatonFound {
		if (missingTransitions.isEmpty()) {
			throw new AutomatonFound(automaton);
		}
		
		for (Pair<Integer, EventExpressionPair> missing : new HashSet<>(missingTransitions)) {
			missingTransitions.remove(missing);
			int stateFrom = missing.getLeft();
			EventExpressionPair p = missing.getRight();
			
			for (StringActions actions : preparedActions) {
				for (int dst = 0; dst < colorSize; dst++) {
					structures.Transition autoT = new Transition(automaton.getState(stateFrom),
							automaton.getState(dst), p.event, p.expression, actions);
					automaton.addTransition(automaton.getState(stateFrom), autoT);
					if (verifier.verify(automaton)) {
						ensureCompleteness(missingTransitions);
					}
					automaton.getState(stateFrom).removeTransition(autoT);
				}
			}
			missingTransitions.add(missing);
		}
	}
}

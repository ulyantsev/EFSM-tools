package algorithms;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import qbf.reduction.Verifier;
import structures.Automaton;
import structures.Node;
import structures.Transition;
import actions.StringActions;
import bool.MyBooleanExpression;

public class AutomatonCompleter {
	private final Verifier verifier;
	private final Automaton automaton;
	private final int colorSize;
	private final List<String> events;
	private final List<StringActions> preparedActions = new ArrayList<>();
	private final long finishTime;
	
	/*
	 * The automaton should be verified!
	 */
	public AutomatonCompleter(Verifier verifier, Automaton automaton, List<String> events,
			List<String> actions, long finishTime) {
		this.verifier = verifier;
		this.automaton = automaton;
		colorSize = automaton.statesCount();
		this.events = events;
		this.finishTime = finishTime;
		
		// prepare all action combinations (will be used while trying to enforce FSM completeness)
		final int actionsNum = actions.size();
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
		preparedActions.sort((a1, a2) ->
			Integer.compare(a1.getActions().length, a2.getActions().length)
		);
		for (StringActions a : preparedActions) {
			System.out.println(a);
		}
	}
	
	private List<Pair<Integer, String>> missingTransitions() {
		final List<Pair<Integer, String>> missing = new ArrayList<>();
		for (Node s : automaton.getStates()) {
			for (String e : events) {
				if (s.getTransition(e, MyBooleanExpression.getTautology()) == null) {
					missing.add(Pair.of(s.getNumber(), e));
				}
			}
		}
		return missing;
	}
	
	public void ensureCompleteness() throws AutomatonFound, TimeLimitExceeded {
		ensureCompleteness(missingTransitions());
	}
	
	private void ensureCompleteness(List<Pair<Integer, String>> missingTransitions)
			throws AutomatonFound, TimeLimitExceeded {
		if (System.currentTimeMillis() > finishTime) {
			throw new TimeLimitExceeded();
		}
		if (!verifier.verify(automaton)) {
			return;
		}
		if (missingTransitions.isEmpty()) {
			throw new AutomatonFound(automaton);
		}
		
		final Pair<Integer, String> missing = missingTransitions.get(missingTransitions.size() - 1);
		missingTransitions.remove(missingTransitions.size() - 1);
		
		final Node stateFrom = automaton.getState(missing.getLeft());
		final String e = missing.getRight();
		
		for (StringActions actions : preparedActions) {
			for (int dst = 0; dst < colorSize; dst++) {
				Transition autoT = new Transition(stateFrom,
						automaton.getState(dst), e, MyBooleanExpression.getTautology(), actions);
				automaton.addTransition(stateFrom, autoT);
				ensureCompleteness(missingTransitions);
				stateFrom.removeTransition(autoT);
			}
		}
		
		missingTransitions.add(missing);
	}
}

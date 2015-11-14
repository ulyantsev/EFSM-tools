package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import egorov.verifier.Verifier;
import scenario.StringActions;
import structures.Automaton;
import structures.Node;
import structures.Transition;
import algorithms.exception.AutomatonFoundException;
import algorithms.exception.TimeLimitExceededException;
import bool.MyBooleanExpression;

public class AutomatonCompleter {
	private final Verifier verifier;
	private final Automaton automaton;
	private final int colorSize;
	private final List<String> events;
	private final List<StringActions> preparedActions;
	private final long finishTime;
	private final CompletenessType completenessType;
	
	public enum CompletenessType {
		NORMAL, // usual completeness
		NO_DEAD_ENDS // no dead ends - resolves LTL semantics problems
	}
	
	/*
	 * The automaton should be verified!
	 */
	public AutomatonCompleter(Verifier verifier, Automaton automaton, List<String> events,
			List<String> actions, long finishTime, CompletenessType type) {
		this.verifier = verifier;
		this.automaton = automaton;
		colorSize = automaton.stateCount();
		this.events = events;
		this.finishTime = finishTime;
		this.completenessType = type;
		
		// prepare all action combinations (will be used while trying to enforce FSM completeness)
		preparedActions = prepareActions(actions);
	}
	
	private static List<StringActions> prepareActions(List<String> actions) {
		final List<StringActions> prepared = new ArrayList<>();
		final int maxI = 1 << actions.size();
		for (int i = 0; i < maxI; i++) {
			final List<String> sequence = new ArrayList<>();
			for (int j = 0; j < actions.size(); j++) {
				if (((i >> j) & 1) == 1) {
					sequence.add(actions.get(j));
				}
			}
			prepared.add(new StringActions(String.join(",", sequence)));
		}
		prepared.sort((a1, a2) ->
			Integer.compare(a1.getActions().length, a2.getActions().length)
		);
		return prepared;
	}
	
	/*
	 * NORMAL mode: usual missing transitions
	 * NO_DEAD_ENDS: missing transitions such that there is some transition from the
	 *    source state are not considered as missing
	 */
	private List<Pair<Integer, String>> missingTransitions() {
		final List<Pair<Integer, String>> missing = new ArrayList<>();
		for (Node s : automaton.states()) {
			if (completenessType == CompletenessType.NO_DEAD_ENDS && !s.transitions().isEmpty()) {
				continue;
			}
			for (String e : events) {
				if (!s.hasTransition(e, MyBooleanExpression.getTautology())) {
					missing.add(Pair.of(s.number(), e));
				}
			}
		}
		return missing;
	}
	
	public void ensureCompleteness() throws AutomatonFoundException, TimeLimitExceededException {
		ensureCompleteness(missingTransitions());
	}
	
	private void ensureCompleteness(List<Pair<Integer, String>> missingTransitions)
			throws AutomatonFoundException, TimeLimitExceededException {
		if (System.currentTimeMillis() > finishTime) {
			throw new TimeLimitExceededException();
		}
		if (!verifier.verify(automaton)) {
			return;
		}
		if (missingTransitions.isEmpty()) {
			throw new AutomatonFoundException(automaton);
		}
		
		final Pair<Integer, String> missing = missingTransitions.get(missingTransitions.size() - 1);
		missingTransitions.remove(missingTransitions.size() - 1);
		
		final Node stateFrom = automaton.state(missing.getLeft());
		final String e = missing.getRight();
		
		for (StringActions actions : preparedActions) {
			for (int dst = 0; dst < colorSize; dst++) {
				Transition autoT = new Transition(stateFrom,
						automaton.state(dst), e, MyBooleanExpression.getTautology(), actions);
				automaton.addTransition(stateFrom, autoT);
				ensureCompleteness(missingTransitions);
				stateFrom.removeTransition(autoT);
			}
		}
		
		missingTransitions.add(missing);
	}
}

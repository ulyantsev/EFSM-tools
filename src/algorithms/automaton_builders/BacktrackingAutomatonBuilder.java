package algorithms.automaton_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import sat_solving.SolverResult;
import sat_solving.SolverResult.SolverResults;
import scenario.StringActions;
import scenario.StringScenario;
import structures.Automaton;
import structures.Node;
import structures.ScenarioTree;
import structures.Transition;
import algorithms.AutomatonCompleter;
import algorithms.AutomatonCompleter.CompletenessType;
import algorithms.exception.AutomatonFoundException;
import algorithms.exception.TimeLimitExceededException;
import bool.MyBooleanExpression;
import egorov.Verifier;
import egorov.ltl.grammar.LtlNode;

public class BacktrackingAutomatonBuilder {
	private static abstract class TraverseState {
		protected final int colorSize;
		protected final List<String> events;
		protected final List<String> actions;
		protected final long finishTime;
		protected final Automaton automaton;
		protected final int[] coloring;
		protected final Verifier verifier;
		protected CompletenessType completenessType;

		/*
		 * auxiliary list for search space reduction
		 */
		protected final int[] incomingTransitionNumbers;
		
		public TraverseState(int colorSize, List<String> events, List<String> actions, long finishTime,
				int[] coloring, Verifier verifier, CompletenessType completenessType) {
			this.colorSize = colorSize;
			this.events = events;
			this.actions = actions;
			this.finishTime = finishTime;
			this.coloring = coloring;
			this.verifier = verifier;
			this.completenessType = completenessType;
			automaton = new Automaton(colorSize);
			incomingTransitionNumbers = new int[colorSize];
		}
		
		protected void checkTimeLimit() throws TimeLimitExceededException {
			if (System.currentTimeMillis() > finishTime) {
				throw new TimeLimitExceededException();
			}
		}
		
		protected boolean verify() {
			return verifier.verify(automaton);
		}
		
		public abstract void backtracking() throws AutomatonFoundException, TimeLimitExceededException;
	}
	
	private static class OrdinaryTraverseState extends TraverseState {
		private List<Transition> frontier;
		
		public OrdinaryTraverseState(ScenarioTree tree, Verifier verifier, int colorSize, long finishTime,
				List<String> events, List<String> actions, CompletenessType completenessType) {
			super(colorSize, events, actions, finishTime, new int[tree.nodesCount()],
					verifier, completenessType);
			frontier = new ArrayList<>(tree.getRoot().getTransitions());
		}
		
		/*
		 * Returns whether the automaton is consistent with scenarios.
		 */
		private boolean findNewFrontier() {
			final List<Transition> finalFrontier = new ArrayList<>();
			final List<Transition> currentFrontier = new ArrayList<>();
			currentFrontier.addAll(frontier);
			while (!currentFrontier.isEmpty()) {
				final Transition t = currentFrontier.get(currentFrontier.size() - 1);
				currentFrontier.remove(currentFrontier.size() - 1);
				final int stateFrom = coloring[t.getSrc().getNumber()];
				final Transition autoT = automaton.getState(stateFrom)
						.getTransition(t.getEvent(), t.getExpr());
				if (autoT == null) {
					finalFrontier.add(t);
				} else if (autoT.getActions().equals(t.getActions())) {
					currentFrontier.addAll(t.getDst().getTransitions());
					coloring[t.getDst().getNumber()] = autoT.getDst().getNumber();
				} else {
					return false;
				}
			}
			frontier = finalFrontier;
			return true;
		}

		@Override
		public void backtracking() throws AutomatonFoundException, TimeLimitExceededException {
			checkTimeLimit();
			
			Transition t = frontier.get(0);
			// further edges should be added from this state:
			final Node stateFrom = automaton.getState(coloring[t.getSrc().getNumber()]);
			final String event = t.getEvent();
			final MyBooleanExpression expression = t.getExpr();
			final StringActions stringActions = t.getActions();
			assert stateFrom.getTransition(event, expression) == null;
			for (int dst = 0; dst < colorSize; dst++) {
				if (dst > 1 && incomingTransitionNumbers[dst - 1] == 0) {
					break;
					// this is done to reduce repeated checks (similar to BFS constraints)
				}
				
				Transition autoT = new Transition(stateFrom,
						automaton.getState(dst), event, expression, stringActions);
				automaton.addTransition(stateFrom, autoT);
				incomingTransitionNumbers[dst]++;
				final List<Transition> frontierBackup = frontier;
				
				if (findNewFrontier() && verify()) {
					if (frontier.isEmpty()) {
						new AutomatonCompleter(verifier, automaton, events,
								actions, finishTime, completenessType).ensureCompleteness();
					} else {
						backtracking();
					}
				}
				
				frontier = frontierBackup;
				stateFrom.removeTransition(autoT);
				incomingTransitionNumbers[dst]--;
			}
		}
	}
	
	private static class TraverseStateWithMultiEdges extends TraverseState {
		private List<List<Transition>> frontier;

		public TraverseStateWithMultiEdges(ScenarioTree tree, Verifier verifier, int colorSize, long finishTime,
				List<String> events, List<String> actions, CompletenessType completenessType) {
			super(colorSize, events, actions, finishTime, new int[tree.nodesCount()],
					verifier, completenessType);
			frontier = new ArrayList<>(groupByDst(tree.getRoot().getTransitions()));
		}
		
		/*
		 * Returns whether the automaton is consistent with scenarios.
		 */
		private boolean findNewFrontier() {
			final List<List<Transition>> finalFrontier = new ArrayList<>();
			final List<List<Transition>> currentFrontier = new ArrayList<>();
			currentFrontier.addAll(frontier);
			while (!currentFrontier.isEmpty()) {
				final List<Transition> tList = currentFrontier.get(currentFrontier.size() - 1);
				currentFrontier.remove(currentFrontier.size() - 1);
				final int stateFrom = coloring[tList.get(0).getSrc().getNumber()];
				final List<Transition> transitions = new ArrayList<>();
				boolean wasNull = false;
				boolean wasProper = false;
				for (Transition t : tList) {
					final Transition autoT = automaton.getState(stateFrom)
							.getTransition(t.getEvent(), tautology());
					if (autoT != null && !autoT.getActions().equals(t.getActions())) {
						return false;
					}
					wasNull |= autoT == null;
					wasProper |= autoT != null;
					transitions.add(autoT);
				}
				if (wasNull && wasProper) {
					return false;
				} else if (wasProper) {
					final int autoDst = transitions.get(0).getDst().getNumber();
					if (!transitions.stream().allMatch(t -> t.getDst().getNumber() == autoDst)) {
						return false;
					}
					final Node scDst = tList.get(0).getDst();
					currentFrontier.addAll(groupByDst(scDst.getTransitions()));
					coloring[scDst.getNumber()] = autoDst;
				} else {
					finalFrontier.add(tList);
				}
			}
			frontier = finalFrontier;
			return true;
		}

		@Override
		public void backtracking() throws AutomatonFoundException, TimeLimitExceededException {
			checkTimeLimit();
			
			final List<Transition> tList = frontier.get(0);
			// further edges should be added from this state:
			final Node stateFrom = automaton.getState(coloring[tList.get(0).getSrc().getNumber()]);
			final StringActions stringActions = tList.get(0).getActions();
			for (int dst = 0; dst < colorSize; dst++) {
				if (dst > 1 && incomingTransitionNumbers[dst - 1] == 0) {
					break;
					// this is done to reduce repeated checks
				}
				
				final List<Transition> addedTransitions = new ArrayList<>();
				for (Transition t : tList) {
					Transition autoT = new Transition(stateFrom,
							automaton.getState(dst), t.getEvent(), tautology(), stringActions);
					addedTransitions.add(autoT);
					if (automaton.getState(stateFrom.getNumber()).hasTransition(t.getEvent(), tautology())) {
						throw new AssertionError();
					}
					automaton.addTransition(stateFrom, autoT);
				}
				incomingTransitionNumbers[dst]++;
				
				final List<List<Transition>> frontierBackup = frontier;
				
				if (findNewFrontier() && verify()) {
					if (frontier.isEmpty()) {
						new AutomatonCompleter(verifier, automaton, events,
								actions, finishTime, completenessType).ensureCompleteness();
					} else {
						backtracking();
					}
				}
				
				frontier = frontierBackup;
				addedTransitions.forEach(stateFrom::removeTransition);
				incomingTransitionNumbers[dst]--;
			}
		}
	}
	
	private static class TraverseStateWithCoverageAndWeakCompleteness extends TraverseState {
		private final Map<String, List<String>> eventExtensions;
		private List<List<Transition>> frontier;
		private final List<String> eventNames;

		public TraverseStateWithCoverageAndWeakCompleteness(ScenarioTree tree, int colorSize, long finishTime,
				List<String> events, List<String> eventNames, int variables) {
			super(colorSize, events, null, finishTime, new int[tree.nodesCount()], null, null);
			frontier = new ArrayList<>(groupByDst(tree.getRoot().getTransitions()));
			this.eventNames = eventNames;
			this.eventExtensions = eventExtensions(events, eventNames, variables);
		}
		
		/*
		 * Returns whether the automaton is consistent with scenarios.
		 */
		private boolean findNewFrontier() {
			final List<List<Transition>> finalFrontier = new ArrayList<>();
			final List<List<Transition>> currentFrontier = new ArrayList<>();
			currentFrontier.addAll(frontier);
			while (!currentFrontier.isEmpty()) {
				final List<Transition> tList = currentFrontier.get(currentFrontier.size() - 1);
				currentFrontier.remove(currentFrontier.size() - 1);
				final int stateFrom = coloring[tList.get(0).getSrc().getNumber()];
				final List<Transition> transitions = new ArrayList<>();
				boolean wasNull = false;
				boolean wasProper = false;
				for (Transition t : tList) {
					final Transition autoT = automaton.getState(stateFrom)
							.getTransition(t.getEvent(), tautology());
					if (autoT != null && !autoT.getActions().equals(t.getActions())) {
						return false;
					}
					wasNull |= autoT == null;
					wasProper |= autoT != null;
					transitions.add(autoT);
				}
				if (wasNull && wasProper) {
					return false;
				} else if (wasProper) {
					final int autoDst = transitions.get(0).getDst().getNumber();
					if (!transitions.stream().allMatch(t -> t.getDst().getNumber() == autoDst)) {
						return false;
					}
					final Node scDst = tList.get(0).getDst();
					currentFrontier.addAll(groupByDst(scDst.getTransitions()));
					coloring[scDst.getNumber()] = autoDst;
				} else {
					finalFrontier.add(tList);
				}
			}
			frontier = finalFrontier;
			return true;
		}

		@Override
		public void backtracking() throws AutomatonFoundException, TimeLimitExceededException {
			checkTimeLimit();
			
			final List<Transition> tList = frontier.get(0);
			// further edges should be added from this state:
			final Node stateFrom = automaton.getState(coloring[tList.get(0).getSrc().getNumber()]);
			final StringActions stringActions = tList.get(0).getActions();
			for (int dst = 0; dst < colorSize; dst++) {
				if (dst > 1 && incomingTransitionNumbers[dst - 1] == 0) {
					break;
					// this is done to reduce repeated checks
				}
				
				final List<Transition> addedTransitions = new ArrayList<>();
				for (Transition t : tList) {
					Transition autoT = new Transition(stateFrom,
							automaton.getState(dst), t.getEvent(),
							tautology(), stringActions);
					addedTransitions.add(autoT);
					automaton.addTransition(stateFrom, autoT);
				}
				incomingTransitionNumbers[dst]++;
				
				final List<List<Transition>> frontierBackup = frontier;
				
				if (findNewFrontier()) {
					if (frontier.isEmpty()) {
						// check weak completeness
						if (isWeakComplete(automaton, eventNames, eventExtensions)) {
							throw new AutomatonFoundException(automaton);
						} // else do nothing
					} else {
						backtracking();
					}
				}
				
				frontier = frontierBackup;
				addedTransitions.forEach(stateFrom::removeTransition);
				incomingTransitionNumbers[dst]--;
			}
		}
	}
	
	private static class TraverseStateWithErrors extends TraverseState {
		private final Map<String, List<String>> eventExtensions;
		private List<FrontierElement> frontier = new ArrayList<>();
		private final List<String> eventNames;
		private final int errorNumber;
		private final int[][] coloring; // overrides
		private final List<StringScenario> scenarios;

		private class FrontierElement {
			final int scenarioIndex;
			final int scenarioPosition;
			
			FrontierElement(int scenarioIndex, int scenarioPosition) {
				this.scenarioIndex = scenarioIndex;
				this.scenarioPosition = scenarioPosition;
			}
			
			int coloring() {
				return coloring[scenarioIndex][scenarioPosition];
			}
			
			void setColoring(int value) {
				coloring[scenarioIndex][scenarioPosition] = value;
			}
			
			StringScenario scenario() {
				return scenarios.get(scenarioIndex);
			}
			
			List<String> events() {
				return scenario().getEvents(scenarioPosition);
			}

			@Override
			public String toString() {
				return "(" + scenarioIndex + ", " + scenarioPosition + ")";
			}
			
			FrontierElement advance() {
				final int newPos = scenarioPosition + 1;
				return scenario().size() > newPos
						? new FrontierElement(scenarioIndex, newPos)
						: null;
			}
		}
		
		public TraverseStateWithErrors(List<StringScenario> scenarios, int colorSize, long finishTime,
				List<String> events, List<String> eventNames, int variables, int errorNumber) {
			super(colorSize, events, null, finishTime, null, null, null);
			for (int i = 0; i < scenarios.size(); i++) {
				frontier.add(new FrontierElement(i, 0));
			}
			this.eventNames = eventNames;
			this.eventExtensions = eventExtensions(events, eventNames, variables);
			this.errorNumber = errorNumber;
			coloring = new int[scenarios.size()][];
			for (int i = 0; i < scenarios.size(); i++) {
				coloring[i] = new int[scenarios.get(i).size()];
			}
			this.scenarios = scenarios;
		}

		/*
		 * Returns whether the automaton is consistent with scenarios (assuming there is a number of possible errors).
		 */
		private boolean findNewFrontier() {
			final List<FrontierElement> newFrontier = new ArrayList<>();
			for (FrontierElement elem : frontier) {
				FrontierElement cur = elem;
				while (true) {
					final int stateFrom = cur.coloring();
					final List<Transition> transitions = new ArrayList<>();
					boolean wasNull = false;
					boolean wasProper = false;
					for (String event : cur.events()) {
						final Transition autoT = automaton.getState(stateFrom)
								.getTransition(event, tautology());
						wasNull |= autoT == null;
						wasProper |= autoT != null;
						transitions.add(autoT);
					}
					if (wasNull && wasProper) {
						return false;
					} else if (wasProper) {
						final int autoDst = transitions.get(0).getDst().getNumber();
						if (!transitions.stream().allMatch(t -> t.getDst().getNumber() == autoDst)) {
							return false;
						}
						final FrontierElement newElem = cur.advance();
						if (newElem != null) {
							cur.setColoring(autoDst);
							cur = newElem;
						} else {
							// the frontier becomes smaller
							break;
						}
					} else {
						// the frontier retains its size
						newFrontier.add(cur);
						break;
					}
				}
			}
			System.out.println(frontier);
			frontier = newFrontier;
			return label();
		}

		private static StringActions mode(List<StringActions> list) {
			if (list.isEmpty()) {
				throw new AssertionError();
			}
			final Map<StringActions, Integer> map = new HashMap<>();
			int max = 0;
			StringActions ans = null;
			for (StringActions elem : list) {
				if (map.containsKey(elem)) {
					int count = map.get(elem) + 1;
					map.put(elem, count);
					if (count > max) {
						max = count;
						ans = elem;
					}
				} else {
					map.put(elem, 1);
				}
			}
			return ans;
		}
		
		/*
		 * Returns whether the labeling was successful regarding the number of possible errors.
		 */
		private boolean label() {
			final Map<Transition, List<StringActions>> actionOccurrencies = new HashMap<>();
			for (int i = 0; i < colorSize; i++) {
				for (Transition t : automaton.getState(i).getTransitions()) {
					actionOccurrencies.put(t, new ArrayList<>());
				}
			}
			for (StringScenario sc : scenarios) {
				Node state = automaton.getStartState();
				for (int i = 0; i < sc.size(); i++) {
					final Transition t = state.getTransition(sc.getEvents(i).get(0), tautology());
					if (t == null) {
						break;
					}
					for (String e : sc.getEvents(i)) {
						actionOccurrencies.get(state.getTransition(e, tautology())).add(sc.getActions(i));
					}
					state = t.getDst();
				}
			}
			System.out.println(actionOccurrencies);
			int madeErrors = 0;
			for (int i = 0; i < colorSize; i++) {
				for (Transition t : new ArrayList<>(automaton.getState(i).getTransitions())) {
					final List<StringActions> actions = actionOccurrencies.get(t);
					final StringActions mode = mode(actions);
					automaton.getState(i).removeTransition(t);
					automaton.getState(i).addTransition(t.getEvent(), t.getExpr(), mode, t.getDst());
					for (StringActions a : actions) {
						if (!a.equals(mode)) {
							madeErrors++;
						}
					}
				}
			}
			return madeErrors <= errorNumber;
		}
		
		@Override
		public void backtracking() throws AutomatonFoundException, TimeLimitExceededException {
			checkTimeLimit();
			
			final FrontierElement elem = frontier.get(0);
			final Node stateFrom = automaton.getState(elem.coloring());
			for (int dst = 0; dst < colorSize; dst++) {
				if (dst > 1 && incomingTransitionNumbers[dst - 1] == 0) {
					break;
					// this is done to reduce repeated checks
				}
				
				final List<Transition> addedTransitions = new ArrayList<>();
				for (String event : elem.events()) {
					final Transition autoT = new Transition(stateFrom, automaton.getState(dst),
							event, tautology(), new StringActions(""));
					addedTransitions.add(autoT);
					automaton.addTransition(stateFrom, autoT);
				}
				System.out.println("adding " + addedTransitions);
				incomingTransitionNumbers[dst]++;
				
				final List<FrontierElement> frontierBackup = frontier;
				
				if (findNewFrontier()) {
					System.out.println(frontier.size());
					if (frontier.isEmpty()) {
						// check weak completeness
						if (isWeakComplete(automaton, eventNames, eventExtensions)) {
							throw new AutomatonFoundException(automaton);
						} // else do nothing
					} else {
						backtracking();
					}
				}
				
				frontier = frontierBackup;
				addedTransitions.forEach(stateFrom::removeTransition);
				incomingTransitionNumbers[dst]--;
			}
		}
	}
	
	private static Map<String, List<String>> eventExtensions(List<String> events, List<String> eventNames, int variables) {
		final Map<String, List<String>> eventExtensions = new HashMap<>();
		for (String eventName : eventNames) {
			eventExtensions.put(eventName, events.stream().filter(e -> e.substring(0, e.length() - variables)
					.equals(eventName)).collect(Collectors.toList()));
		}
		return eventExtensions;
	}
	
	private static MyBooleanExpression tautology() {
		return MyBooleanExpression.getTautology();
	}
	
	private static boolean isWeakComplete(Automaton automaton, List<String> eventNames, Map<String, List<String>> eventExtensions) {
		for (int i = 0; i < automaton.statesCount(); i++) {
			for (String initialEvent : eventNames) {
				// is there at least one transition with this initialEvent from this state?
				boolean hasTransition = false;
				boolean allTransitions = true;
				for (String e : eventExtensions.get(initialEvent)) {
					if (automaton.getState(i).hasTransition(e, MyBooleanExpression.getTautology())) {
						hasTransition = true;
					} else {
						allTransitions = false;
					}
				}
				if (hasTransition && !allTransitions) {
					return false;
				}
			}
		}
		return true;
	}
	
	/*
	 * For multi-edges obtained in the case of variables.
	 */
	private static Collection<List<Transition>> groupByDst(Collection<Transition> transitions) {
		final Map<Integer, List<Transition>> transitionGroups = new TreeMap<>();
		for (Transition t : transitions) {
			final int num = t.getDst().getNumber();
			if (!transitionGroups.containsKey(num)) {
				transitionGroups.put(num, new ArrayList<>());
			}
			transitionGroups.get(num).add(t);
		}
		
		return transitionGroups.values();
	}
	
	public static Optional<Automaton> build(Logger logger, ScenarioTree tree, int size,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, Verifier verifier,
			long finishTime, CompletenessType completenessType, int variables,
			boolean ensureCoverageAndWeakCompleteness, List<String> eventNames,
			int errorNumber, List<StringScenario> scenarios) throws IOException {
		try {
			if (errorNumber > 0) {
				// for Vladimir's comparison
				new TraverseStateWithErrors(scenarios, size, finishTime, events, eventNames,
						variables, errorNumber).backtracking();
			} else if (ensureCoverageAndWeakCompleteness) {
				// for Vladimir's comparison
				new TraverseStateWithCoverageAndWeakCompleteness(tree, size, finishTime,
						events, eventNames, variables).backtracking();
			} else if (variables == 0) {
				new OrdinaryTraverseState(tree, verifier, size, finishTime, events, actions,
						completenessType).backtracking();
			} else {
				// for Daniil's instances
				new TraverseStateWithMultiEdges(tree, verifier, size, finishTime, events, actions,
						completenessType).backtracking();
			}
		} catch (AutomatonFoundException e) {
			return Optional.of(e.automaton);
		} catch (TimeLimitExceededException e) {
			logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN.");
			return Optional.empty();
		}
		logger.info(new SolverResult(SolverResults.UNSAT).toString());
		return Optional.empty();
	}
}

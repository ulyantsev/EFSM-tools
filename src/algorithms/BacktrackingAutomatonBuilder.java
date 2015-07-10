package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.SolverResult;
import qbf.reduction.SolverResult.SolverResults;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
import algorithms.AutomatonCompleter.CompletenessType;
import bool.MyBooleanExpression;

public class BacktrackingAutomatonBuilder {
	private static class TraverseState {
		private final int colorSize;
		private final List<String> events;
		private final List<String> actions;
		private final long finishTime;
		private final CompletenessType completenessType;
		
		private final Automaton automaton;
		private int[] coloring;
		private List<Transition> frontier;
		
		/*
		 * auxiliary list for search space reduction
		 */
		private final int[] incomingTransitionNumbers;
		
		private final Verifier verifier;
		
		public TraverseState(ScenariosTree tree, Verifier verifier, int colorSize, long finishTime,
				List<String> events, List<String> actions, CompletenessType completenessType) {
			this.colorSize = colorSize;
			this.automaton = new Automaton(colorSize);
			this.coloring = new int[tree.nodesCount()];
			frontier = new ArrayList<>(tree.getRoot().getTransitions());
			this.verifier = verifier;
			this.finishTime = finishTime;
			incomingTransitionNumbers = new int[colorSize];
			this.events = events;
			this.actions = actions;
			this.completenessType = completenessType;
		}
		
		private boolean verify() {
			return verifier.verify(automaton);
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
				final structures.Transition autoT = automaton.getState(stateFrom)
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

		public void backtracking() throws AutomatonFound, TimeLimitExceeded {
			if (System.currentTimeMillis() > finishTime) {
				throw new TimeLimitExceeded();
			}
			
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
	
	private static class TraverseStateWithMultiEdges {
		private final int colorSize;
		private final List<String> events;
		private final List<String> actions;
		private final long finishTime;
		private final CompletenessType completenessType;
		
		private final Automaton automaton;
		private int[] coloring;
		private List<List<Transition>> frontier;
				
		/*
		 * auxiliary list for search space reduction
		 */
		private final int[] incomingTransitionNumbers;
		
		private final Verifier verifier;
		
		/*
		 * For multi-edges obtained in the case of variables.
		 */
		private Collection<List<Transition>> groupByDst(Collection<Transition> transitions) {
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
		
		public TraverseStateWithMultiEdges(ScenariosTree tree, Verifier verifier, int colorSize, long finishTime,
				List<String> events, List<String> actions, CompletenessType completenessType) {
			this.colorSize = colorSize;
			this.automaton = new Automaton(colorSize);
			this.coloring = new int[tree.nodesCount()];
			frontier = new ArrayList<>(groupByDst(tree.getRoot().getTransitions()));
			this.verifier = verifier;
			this.finishTime = finishTime;
			incomingTransitionNumbers = new int[colorSize];
			this.events = events;
			this.actions = actions;
			this.completenessType = completenessType;
		}
		
		private boolean verify() {
			return verifier.verify(automaton);
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
				int autoDst = -1;
				for (Transition t : tList) {
					final structures.Transition autoT = automaton.getState(stateFrom)
							.getTransition(t.getEvent(), MyBooleanExpression.getTautology());
					if (autoT != null) {
						if (!autoT.getActions().equals(t.getActions())) {
							return false;
						}
						if (autoDst != -1 && autoDst != autoT.getDst().getNumber()) {
							return false;
						}
						autoDst = autoT.getDst().getNumber();
					} else if (autoDst != -1) {
						return false;
					}
				}
				if (autoDst != -1) {
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

		public void backtracking() throws AutomatonFound, TimeLimitExceeded {
			/*for (StringScenario s : sc) {
				if (!automaton.isCompliantWithScenario(s)) {
					System.out.println(automaton);
					throw new AssertionError();
				}
			}*/
			
			if (System.currentTimeMillis() > finishTime) {
				throw new TimeLimitExceeded();
			}
			
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
							MyBooleanExpression.getTautology(), stringActions);
					addedTransitions.add(autoT);
					if (automaton.getState(stateFrom.getNumber()).hasTransition(t.getEvent(), MyBooleanExpression.getTautology())) {
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
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int size,
			String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<String> events, List<String> actions, Verifier verifier,
			long finishTime, CompletenessType completenessType, int variables) throws IOException {
		try {
			if (variables == 1) {
				new TraverseState(tree, verifier, size, finishTime, events, actions,
						completenessType).backtracking();
			} else {
				// for Daniil's instances
				new TraverseStateWithMultiEdges(tree, verifier, size, finishTime, events, actions,
						completenessType).backtracking();
			}
		} catch (AutomatonFound e) {
			return Optional.of(e.automaton);
		} catch (TimeLimitExceeded e) {
			logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN.");
			return Optional.empty();
		}
		logger.info(new SolverResult(SolverResults.UNSAT).toString());
		return Optional.empty();
	}
}

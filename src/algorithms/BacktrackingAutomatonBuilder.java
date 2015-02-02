package algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
import algorithms.FormulaBuilder.EventExpressionPair;
import bool.MyBooleanExpression;

public class BacktrackingAutomatonBuilder {
	private static class AutomatonFound extends Exception {
		public final Automaton automaton;

		public AutomatonFound(Automaton automaton) {
			this.automaton = automaton;
		}
	}
	
	private static class TimeLimitExceeded extends Exception {
	}
	
	private static class TraverseState {
		private final int colorSize;
		private final boolean searchComplete;
		private final List<EventExpressionPair> efPairs;
		private final List<StringActions> preparedActions = new ArrayList<>();
		
		private final Automaton automaton;
		private int[] coloring;
		private List<Transition> frontier;
		
		/*
		 * auxiliary list for search space reduction
		 */
		private final int[] incomingTransitionNumbers;
		
		private final Verifier verifier;
		private final int timeoutSec;
		private final long startTime = System.currentTimeMillis();
		
		public TraverseState(ScenariosTree tree, Verifier verifier, int colorSize, int timeoutSec, boolean searchComplete,
				List<EventExpressionPair> efPairs, List<String> actions) {
			this.colorSize = colorSize;
			this.automaton = new Automaton(colorSize);
			this.coloring = new int[tree.nodesCount()];
			Arrays.fill(coloring, -1);
			frontier = new ArrayList<>(tree.getRoot().getTransitions());
			coloring[tree.getRoot().getNumber()] = 0;
			this.verifier = verifier;
			this.timeoutSec = timeoutSec;
			incomingTransitionNumbers = new int[colorSize];
			this.searchComplete = searchComplete;
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
		
		private boolean verify() {
			return verifier.verify(automaton);
		}
		
		private boolean findNewFrontier() {
			// proper sorting of the frontier is required to prevent losing feasible solutions
			// due to the BFS constraint
			final List<Transition> finalFrontier = new ArrayList<>();
			final List<Transition> currentFrontier = new ArrayList<>();
			currentFrontier.addAll(frontier);
			final int[] newColoring = coloring.clone();
			while (!currentFrontier.isEmpty()) {
				final Transition t = currentFrontier.get(currentFrontier.size() - 1);
				currentFrontier.remove(currentFrontier.size() - 1);
				final int stateFrom = newColoring[t.getSrc().getNumber()];
				final structures.Transition autoT = automaton.getState(stateFrom)
						.getTransition(t.getEvent(), t.getExpr());
				if (autoT == null) {
					finalFrontier.add(t);
				} else if (autoT.getActions().equals(t.getActions())) {
					currentFrontier.addAll(t.getDst().getTransitions());
					newColoring[t.getDst().getNumber()] = autoT.getDst().getNumber();
				} else {
					return false;
				}
			}
			frontier = finalFrontier;
			coloring = newColoring;
			return true;
		}

		private Set<Pair<Integer, EventExpressionPair>> missingTransitions() {
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
		
		private void checkTime() throws TimeLimitExceeded {
			if ((System.currentTimeMillis() - startTime) > timeoutSec * 1000) {
				throw new TimeLimitExceeded();
			}
		}
		
		private void ensureCompleteness(Set<Pair<Integer, EventExpressionPair>> missingTransitions) throws AutomatonFound, TimeLimitExceeded {
			if (missingTransitions.isEmpty()) {
				throw new AutomatonFound(automaton);
			}
			checkTime();
			
			for (Pair<Integer, EventExpressionPair> missing : new HashSet<>(missingTransitions)) {
				missingTransitions.remove(missing);
				int stateFrom = missing.getLeft();
				EventExpressionPair p = missing.getRight();
				
				for (StringActions actions : preparedActions) {
					for (int dst = 0; dst < colorSize; dst++) {
						structures.Transition autoT = new Transition(automaton.getState(stateFrom),
								automaton.getState(dst), p.event, p.expression, actions);
						automaton.addTransition(automaton.getState(stateFrom), autoT);
						if (verify()) {
							ensureCompleteness(missingTransitions);
						}
						automaton.getState(stateFrom).removeTransition(autoT);
					}
				}
				missingTransitions.add(missing);
			}
		}
		
		public void backtracking() throws AutomatonFound, TimeLimitExceeded {
			checkTime();
			for (Transition t : frontier) {
				// further edges should be added from this state:
				int stateFrom = coloring[t.getSrc().getNumber()];
				final String event = t.getEvent();
				final MyBooleanExpression expression = t.getExpr();
				final StringActions actions = t.getActions();
				assert automaton.getState(stateFrom).getTransition(event, expression) == null;
				for (int dst = 0; dst < colorSize; dst++) {
					if (dst > 1 && incomingTransitionNumbers[dst - 1] == 0) {
						break;
						// this is done to reduce repeated checks (similar to BFS constraints)
					}
					
					structures.Transition autoT = new Transition(automaton.getState(stateFrom),
							automaton.getState(dst), event, expression, actions);
					automaton.addTransition(automaton.getState(stateFrom), autoT);
					incomingTransitionNumbers[automaton.getState(dst).getNumber()]++;
					final int[] coloringBackup = coloring;
					final List<Transition> frontierBackup = frontier;
					
					if (findNewFrontier() && verify()) {
						if (frontier.isEmpty()) {
							if (searchComplete) {
								ensureCompleteness(missingTransitions());
							} else {
								throw new AutomatonFound(automaton);
							}
						} else {
							backtracking();
						}
					}
					
					coloring = coloringBackup;
					frontier = frontierBackup;
					automaton.getState(stateFrom).removeTransition(autoT);
					incomingTransitionNumbers[automaton.getState(dst).getNumber()]--;
				}
			}
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, boolean complete,
			int timeoutSeconds, String resultFilePath, String ltlFilePath, List<LtlNode> formulae,
			List<EventExpressionPair> efPairs, List<String> actions) throws IOException {
		try {
			new TraverseState(tree, new Verifier(colorSize, logger, ltlFilePath, EventExpressionPair.getEvents(efPairs), actions),
					colorSize, timeoutSeconds, complete, efPairs, actions).backtracking();
		} catch (AutomatonFound e) {
			return Optional.of(e.automaton);
		} catch (TimeLimitExceeded e) {
			logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN.");
			return Optional.empty();
		}
		return Optional.empty();
	}
}

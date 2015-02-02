package algorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
import algorithms.FormulaBuilder.EventExpressionPair;
import bool.MyBooleanExpression;

public class BacktrackingAutomatonBuilder {
	private static class TraverseState {
		private final int colorSize;
		private final boolean searchComplete;
		private final List<EventExpressionPair> efPairs;
		private final List<String> actions;
		private final long finishTime;
		
		private final Automaton automaton;
		private int[] coloring;
		private List<Transition> frontier;
		
		/*
		 * auxiliary list for search space reduction
		 */
		private final int[] incomingTransitionNumbers;
		
		private final Verifier verifier;
		
		public TraverseState(ScenariosTree tree, Verifier verifier, int colorSize, long finishTime, boolean searchComplete,
				List<EventExpressionPair> efPairs, List<String> actions) {
			this.colorSize = colorSize;
			this.automaton = new Automaton(colorSize);
			this.coloring = new int[tree.nodesCount()];
			Arrays.fill(coloring, -1);
			frontier = new ArrayList<>(tree.getRoot().getTransitions());
			coloring[tree.getRoot().getNumber()] = 0;
			this.verifier = verifier;
			this.finishTime = finishTime;
			incomingTransitionNumbers = new int[colorSize];
			this.searchComplete = searchComplete;
			this.efPairs = efPairs;
			this.actions = actions;
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

		public void backtracking() throws AutomatonFound, TimeLimitExceeded {
			if (System.currentTimeMillis() > finishTime) {
				throw new TimeLimitExceeded();
			}
			
			for (Transition t : frontier) {
				// further edges should be added from this state:
				int stateFrom = coloring[t.getSrc().getNumber()];
				final String event = t.getEvent();
				final MyBooleanExpression expression = t.getExpr();
				final StringActions stringActions = t.getActions();
				assert automaton.getState(stateFrom).getTransition(event, expression) == null;
				for (int dst = 0; dst < colorSize; dst++) {
					if (dst > 1 && incomingTransitionNumbers[dst - 1] == 0) {
						break;
						// this is done to reduce repeated checks (similar to BFS constraints)
					}
					
					structures.Transition autoT = new Transition(automaton.getState(stateFrom),
							automaton.getState(dst), event, expression, stringActions);
					automaton.addTransition(automaton.getState(stateFrom), autoT);
					incomingTransitionNumbers[automaton.getState(dst).getNumber()]++;
					final int[] coloringBackup = coloring;
					final List<Transition> frontierBackup = frontier;
					
					if (findNewFrontier() && verify()) {
						if (frontier.isEmpty()) {
							if (searchComplete) {
								new AutomatonCompleter(verifier, automaton, efPairs, actions, finishTime).ensureCompleteness();
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
		long finishTime = System.currentTimeMillis() + timeoutSeconds * 1000;
		try {
			new TraverseState(tree, new Verifier(colorSize, logger, ltlFilePath, EventExpressionPair.getEvents(efPairs), actions),
					colorSize, finishTime, complete, efPairs, actions).backtracking();
		} catch (AutomatonFound e) {
			return Optional.of(e.automaton);
		} catch (TimeLimitExceeded e) {
			logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN.");
			return Optional.empty();
		}
		return Optional.empty();
	}
}

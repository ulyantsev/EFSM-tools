package algorithms;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import qbf.ltl.LtlNode;
import qbf.reduction.Verifier;
import structures.Automaton;
import structures.ScenariosTree;
import structures.Transition;
import actions.StringActions;
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
		private final Automaton automaton;
		private int[] coloring;
		private Set<Transition> frontier;
		private final Verifier verifier;
		private final int timeoutSec;
		private final long startTime = System.currentTimeMillis();
		
		public TraverseState(ScenariosTree tree, Verifier verifier, int colorSize, int timeoutSec) {
			this.colorSize = colorSize;
			this.automaton = new Automaton(colorSize);
			this.coloring = new int[tree.nodesCount()];
			Arrays.fill(coloring, -1);
			frontier = new LinkedHashSet<>(tree.getRoot().getTransitions());
			coloring[tree.getRoot().getNumber()] = 0;
			this.verifier = verifier;
			this.timeoutSec = timeoutSec;
		}
		
		private boolean ensureBfs() {
			return true;
			// TODO
		}
		
		private boolean verify() throws IOException {
			System.out.println(automaton);
			boolean verified = verifier.verify(automaton);
			System.out.println(verified);
			return verified;
		}
		
		private boolean findNewFrontier() {
			final Set<Transition> finalFrontier = new LinkedHashSet<>();
			final Set<Transition> currentFrontier = new LinkedHashSet<>(frontier);
			int[] newColoring = coloring.clone();
			while (!currentFrontier.isEmpty()) {
				final Transition t = currentFrontier.iterator().next();
				currentFrontier.remove(t);
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
		
		public void backtracking() throws AutomatonFound, TimeLimitExceeded, IOException {
			System.out.println(Arrays.toString(coloring));
			if ((System.currentTimeMillis() - startTime) > timeoutSec * 1000) {
				throw new TimeLimitExceeded();
			}
			for (Transition t : frontier) {
				// further edges should be added from this state:
				int stateFrom = coloring[t.getSrc().getNumber()];
				final String event = t.getEvent();
				final MyBooleanExpression expression = t.getExpr();
				final StringActions actions = t.getActions();
				assert automaton.getState(stateFrom).getTransition(event, expression) == null;
				for (int dst = 0; dst < colorSize; dst++) {
					structures.Transition autoT = new Transition(automaton.getState(stateFrom),
							automaton.getState(dst), event, expression, actions);
					automaton.addTransition(automaton.getState(stateFrom), autoT);
					if (ensureBfs()) {
						final int[] coloringBackup = coloring;
						final Set<Transition> frontierBackup = frontier;
						boolean compliant = findNewFrontier();
						
						if (compliant && verify()) {
							if (frontier.isEmpty()) {
								throw new AutomatonFound(automaton);
							}
							backtracking();
						}
						
						coloring = coloringBackup;
						frontier = frontierBackup;
					}
					automaton.getState(stateFrom).removeTransition(autoT);
				}
			}
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, boolean complete,
			int timeoutSeconds, String resultFilePath, String ltlFilePath, List<LtlNode> formulae) throws IOException {
		
		// TODO fix the problem with the verifier
		
		try {
			new TraverseState(tree, new Verifier(colorSize, logger, ltlFilePath), colorSize, timeoutSeconds).backtracking();
		} catch (AutomatonFound e) {
			return Optional.of(e.automaton);
		} catch (TimeLimitExceeded e) {
			logger.info("TOTAL TIME LIMIT EXCEEDED, ANSWER IS UNKNOWN.");
			return Optional.empty();
		}
		return Optional.empty();
	}
}

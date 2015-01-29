package algorithms;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import actions.StringActions;
import bool.MyBooleanExpression;
import qbf.ltl.LtlNode;
import structures.Automaton;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;

public class BacktrackingAutomatonBuilder {
	private static class AutomatonFound extends Exception {
		public final Automaton automaton;

		public AutomatonFound(Automaton automaton) {
			this.automaton = automaton;
		}
	}
	
	private static class TraverseState {
		private final ScenariosTree tree;
		private final int colorSize;
		private Automaton automaton;
		private final int[] coloring;
		private final Set<Integer> frontier = new TreeSet<>();
		
		public TraverseState(ScenariosTree tree, int colorSize) {
			this.tree = tree;
			this.colorSize = colorSize;
			this.automaton = new Automaton(colorSize);
			this.coloring = new int[tree.nodesCount()];
			Arrays.fill(coloring, -1);
			frontier.add(tree.getRoot().getNumber());
		}
		
		private boolean ensureBfs() {
			return true;
			// TODO
		}
		
		private boolean verify() {
			return true;
			// TODO
		}
		
		public void dfs() throws AutomatonFound {
			for (Integer nodeIndex : frontier) {
				Node n = tree.getNodes().get(nodeIndex);
				// further edges should be added from this state:
				int stateFrom = coloring[nodeIndex];
				for (Transition t : n.getTransitions()) {
					String event = t.getEvent();
					MyBooleanExpression expression = t.getExpr();
					StringActions actions = t.getActions();
					assert automaton.getState(stateFrom).getTransition(event, expression) == null;
					for (int dst = 0; dst < colorSize; dst++) {
						automaton.addTransition(automaton.getState(stateFrom),
								new Transition(automaton.getState(stateFrom),
								automaton.getState(dst), event, expression, actions));
						if (!ensureBfs() || !verify()) {
							continue;
						}
						
						// find new frontier or reject fsm
						dfs();
						// restore coloring, frontier and fsm
					}
				}
			}
		}
	}
	
	public static Optional<Automaton> build(Logger logger, ScenariosTree tree, int colorSize, boolean complete,
			int timeoutSeconds, String resultFilePath, String ltlFilePath, List<LtlNode> formulae) throws IOException {
		// TODO
		
		try {
			new TraverseState(tree, colorSize).dfs();
		} catch (AutomatonFound e) {
			return Optional.of(e.automaton);
		}
		// recursive
		// layer = uncovered layer
		// keep current scenario coloring
		// step:
		//	select transitions from the layer
		//  sort them according to some criterion
		// 	add them in the ways which retain BFS enumeration
		//  probably verify (not f)
		
		return Optional.empty();
	}
}

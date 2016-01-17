package algorithms.formula_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import structures.NegativeNode;
import structures.NegativeScenarioTree;
import structures.Node;
import structures.ScenarioTree;
import structures.Transition;
import algorithms.AdjacencyCalculator;
import bnf_formulae.BooleanVariable;

/**
 * (c) Igor Buzhinsky
 */

public class FastAutomatonFormulaBuilder {
	private final int colorSize;
	private final List<String> events;
	private final List<String> actions;
	private final ScenarioTree positiveTree;
	private final NegativeScenarioTree negativeTree;
	private final boolean complete;
	
	private final List<BooleanVariable> vars = new ArrayList<>();
	
	public FastAutomatonFormulaBuilder(int colorSize, ScenarioTree positiveForest,
			NegativeScenarioTree negativeTree,
			List<String> events, List<String> actions, boolean complete) {
		this.colorSize = colorSize;
		this.events = events;
		this.actions = actions;
		this.positiveTree = positiveForest;
		this.negativeTree = negativeTree;
		this.complete = complete;
	}
	
	public static BooleanVariable xVar(int node, int color) {
		return BooleanVariable.byName("x", node, color).get();
	}
	
	public static BooleanVariable yVar(int from, int to, String event) {
		return BooleanVariable.byName("y", from, to, event).get();
	}
	
	public static BooleanVariable zVar(int from, String action, String event) {
		return BooleanVariable.byName("z", from, action, event).get();
	}
	
	public static BooleanVariable xxVar(int node, int color) {
		return BooleanVariable.byName("xx", node, color).get();
	}
	
	private void addPositiveVars() {
		for (int color = 0; color < colorSize; color++) {
			// scenario vars
			for (Node node : positiveTree.nodes()) {
				vars.add(BooleanVariable.getOrCreate("x", node.number(), color));
			}
			
			for (String e : events) {
				// transition variables y_color_childColor_event_formula
				for (int childColor = 0; childColor < colorSize; childColor++) {
					vars.add(BooleanVariable.getOrCreate("y", color, childColor, e));
				}
				// action variables z_color_action_event_formula
				for (String action : actions) {
					vars.add(BooleanVariable.getOrCreate("z", color, action, e));
				}
			}
		}
	}
	
	private void addNegativeVars() {
		for (Node node : negativeTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xx", node.number(), color));
			}
		}
	}
	
	/*
	 * Each node has at least one color
	 */
	private void eachNodeHasColorConstraints(List<int[]> constraints) {
		for (Node node : positiveTree.nodes()) {
			final int[] constraint = new int[colorSize];
			for (int color = 0; color < colorSize; color++) {
				constraint[color] = xVar(node.number(), color).number;
			}
			constraints.add(constraint);
		}
	}

	private void eachNodeHasOnlyColorConstraints(List<int[]> constraints) {
		for (Node node : positiveTree.nodes()) {
			for (int color1 = 0; color1 < colorSize; color1++) {
				final BooleanVariable v1 = xVar(node.number(), color1);
				for (int color2 = 0; color2 < color1; color2++) {
					final BooleanVariable v2 = xVar(node.number(), color2);					
					constraints.add(new int[] {
							-v1.number,
							-v2.number
					});
				}
			}
		}
	}
	
	/*
	 * REQUIRED since we account for action order
	 */
	private void consistencyConstraints(List<int[]> constraints) {
		final Map<Node, Set<Node>> adjacent = AdjacencyCalculator.getAdjacent(positiveTree);
		final int[][] varNums = new int[positiveTree.nodes().size()][colorSize];
		for (Node node : positiveTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				varNums[node.number()][color] = xVar(node.number(), color).number;
			}
		}
		
		for (Node node : positiveTree.nodes()) {
			final int nodeNum = node.number();
			for (Node other : adjacent.get(node)) {
				if (other.number() < node.number()) {
					for (int color = 0; color < colorSize; color++) {
						constraints.add(new int[] {
								-varNums[nodeNum][color],
								-varNums[other.number()][color],
						});
					}
				}
			}
		}
	}
	
	private void transitionConstraints(List<int[]> constraints) {
		for (Node node : positiveTree.nodes()) {
			for (Transition t : node.transitions()) {
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					final BooleanVariable nodeVar = xVar(node.number(), nodeColor);
					for (int childColor = 0; childColor < colorSize; childColor++) {
						final BooleanVariable childVar = xVar(t.dst().number(), childColor);
						final BooleanVariable relationVar = yVar(nodeColor, childColor, t.event());
						constraints.add(new int[] {
								relationVar.number,
								-nodeVar.number,
								-childVar.number
						});
						constraints.add(new int[] {
								-relationVar.number,
								-nodeVar.number,
								childVar.number
						});
					}
				}
			}
		}
	}
	
	private void notMoreThanOneEdgeConstraints(List<int[]> constraints) {
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String e : events) {
				for (int i2 = 0; i2 < colorSize; i2++) {
					for (int i3 = 0; i3 < i2; i3++) {
						constraints.add(new int[] {
								-yVar(i1, i2, e).number,
								-yVar(i1, i3, e).number
						});
					}
				}
			}
		}
	}
	
	private void scenarioActionConstraints(List<int[]> constraints) {
		for (Node node : positiveTree.nodes()) {
			for (Transition t : node.transitions()) {
				final List<String> actionSequence = Arrays.asList(t.actions().getActions());
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					for (String action : actions) {
						constraints.add(new int[] {
								-xVar(node.number(), nodeColor).number,
								(actionSequence.contains(action) ? 1 : -1)
									* zVar(nodeColor, action, t.event()).number
						});
					}
				}
			}
		}
	}
	
	private void eventCompletenessConstraints(List<int[]> constraints) {
		for (int i1 = 0; i1 < colorSize; i1++) {
			if (complete) {
				for (String e : events) {
					final int[] constraint = new int[colorSize];
					for (int i2 = 0; i2 < colorSize; i2++) {
						constraint[i2] = yVar(i1, i2, e).number;
					}
					constraints.add(constraint);
				}
			} else {
				final int[] constraint = new int[colorSize * events.size()];
				int pos = 0;
				for (String e : events) {
					for (int i2 = 0; i2 < colorSize; i2++) {
						constraint[pos++] = yVar(i1, i2, e).number;
					}
				}
				constraints.add(constraint);
			}
		}
	}
	
	private void negativeScenarioBasis(List<int[]> constraints) {
		constraints.add(new int[] {
				xxVar(negativeTree.getRoot().number(), 0).number
		});
	}
	
	private void negativeScenarioPropagation(List<int[]> constraints) {
		final int[] xxParent = new int[colorSize];
		final int[][] actionEq = new int[colorSize][actions.size()];
		final int[] xxChild = new int[colorSize];

		for (NegativeNode node : negativeTree.nodes()) {
			boolean xxParentFilled = false;
			for (Transition edge : node.transitions()) {
				final NegativeNode childNode = (NegativeNode) edge.dst();
				if (!negativeTree.processChild(childNode)) {
					continue;
				}
				if (!xxParentFilled) {
					for (int color = 0; color < colorSize; color++) {
						xxParent[color] = xxVar(node.number(), color).number;
					}
					xxParentFilled = true;
				}
				final String event = edge.event();
				final List<String> actionList = Arrays.asList(edge.actions().getActions());
				for (int color = 0; color < colorSize; color++) {
					for (int i = 0; i < actions.size(); i++) {
						final String action = actions.get(i);
						final int sign = actionList.contains(action) ? 1 : -1;
						// we actually need the negation, this -sign:
						actionEq[color][i] = -sign * zVar(color, action, event).number;
					}
				}
				for (int color = 0; color < colorSize; color++) {
					xxChild[color] = xxVar(childNode.number(), color).number;
				}
				for (int color1 = 0; color1 < colorSize; color1++) {
					for (int color2 = 0; color2 < colorSize; color2++) {
						final int[] constraint = new int[actions.size() + 3];
						System.arraycopy(actionEq[color1], 0, constraint, 0, actions.size());
						constraint[actions.size() + 0] = -xxParent[color1];
						constraint[actions.size() + 1] = -yVar(color1, color2, event).number;
						constraint[actions.size() + 2] = xxChild[color2];
						constraints.add(constraint);
					}
				}
			}
		}
	}
	
	private void negativeScenarioTermination(List<int[]> constraints) {
		for (NegativeNode node : negativeTree.nodes()) {
			final int nodeNumber = node.number();
			if (node.strongInvalid()) {
				for (int color = 0; color < colorSize; color++) {
					constraints.add(new int[] {
							-xxVar(nodeNumber, color).number
					});
				}
			} else {
				for (NegativeNode loop : node.loops()) {
					final int loopNumber = loop.number();
					for (int color = 0; color < colorSize; color++) {
						constraints.add(new int[] {
								-xxVar(nodeNumber, color).number,
								-xxVar(loopNumber, color).number
						});
					}
				}
			}
		}
	}
	
	public void createVars() {
		addPositiveVars();
		addNegativeVars();
	}
	
	public List<int[]> positiveConstraints() {
		final List<int[]> constraints = new ArrayList<>();
		constraints.add(new int[] { xVar(0, 0).number });
		transitionConstraints(constraints);
		eventCompletenessConstraints(constraints);
		notMoreThanOneEdgeConstraints(constraints);
		consistencyConstraints(constraints);
		eachNodeHasColorConstraints(constraints);
		eachNodeHasOnlyColorConstraints(constraints);
		scenarioActionConstraints(constraints);
		return constraints;
	}
	
	public List<int[]> negativeConstraints() {
		final List<int[]> constraints = new ArrayList<>();
		negativeScenarioBasis(constraints);
		negativeScenarioPropagation(constraints);
		negativeScenarioTermination(constraints);
		return constraints;
	}
}

package formula_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import structures.mealy.NegativeMealyNode;
import structures.mealy.NegativeScenarioTree;
import structures.mealy.MealyNode;
import structures.mealy.ScenarioTree;
import structures.mealy.MealyTransition;
import algorithms.AdjacencyCalculator;
import bnf_formulae.BooleanVariable;

public class MealyFormulaBuilder extends FastFormulaBuilder {
	private final ScenarioTree positiveTree;
	private final NegativeScenarioTree negativeTree;
	private final NegativeScenarioTree globalNegativeTree;
	
	public MealyFormulaBuilder(int colorSize, ScenarioTree positiveForest,
                               NegativeScenarioTree negativeTree, NegativeScenarioTree globalNegativeTree,
                               List<String> events, List<String> actions, boolean complete, boolean bfsConstraints) {
		super(colorSize, events, actions, true, complete, bfsConstraints);
		this.positiveTree = positiveForest;
		this.negativeTree = negativeTree;
		this.globalNegativeTree = globalNegativeTree;
	}

	public static BooleanVariable zVar(int from, int action, int event) {
		return BooleanVariable.byName("z", from, action, event).get();
	}
	
	private void addPositiveVars() {
		for (int color = 0; color < colorSize; color++) {
			// scenario vars
			for (MealyNode node : positiveTree.nodes()) {
				vars.add(BooleanVariable.getOrCreate("x", node.number(), color));
			}
			
			for (int ei = 0; ei < events.size(); ei++) {
				// transition variables y_color_childColor_event_formula
				for (int childColor = 0; childColor < colorSize; childColor++) {
					vars.add(BooleanVariable.getOrCreate("y", color, childColor, ei));
				}
				// action variables z_color_action_event_formula
				for (int ai = 0; ai < actions.size(); ai++) {
					vars.add(BooleanVariable.getOrCreate("z", color, ai, ei));
				}
			}
		}
        addBFSVars();
	}
	
	private void addNegativeVars() {
		for (MealyNode node : negativeTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xx", node.number(), color));
			}
		}
		for (MealyNode node : globalNegativeTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xxg", node.number(), color));
			}
		}
	}
	
	/*
	 * Each scenario node has at least one color
	 */
	private void eachNodeHasColorConstraints(List<int[]> constraints) {
		for (MealyNode node : positiveTree.nodes()) {
			final int[] constraint = new int[colorSize];
			for (int color = 0; color < colorSize; color++) {
				constraint[color] = xVar(node.number(), color).number;
			}
			constraints.add(constraint);
		}
	}

    /*
     * Each scenario node has at most one color
     */
	private void eachNodeHasOnlyColorConstraints(List<int[]> constraints) {
		for (MealyNode node : positiveTree.nodes()) {
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
		final Map<MealyNode, Set<MealyNode>> adjacent = AdjacencyCalculator.getAdjacent(positiveTree);
		final int[][] varNums = new int[positiveTree.nodes().size()][colorSize];
		for (MealyNode node : positiveTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				varNums[node.number()][color] = xVar(node.number(), color).number;
			}
		}
		
		for (MealyNode node : positiveTree.nodes()) {
			final int nodeNum = node.number();
			for (MealyNode other : adjacent.get(node)) {
				if (other.number() < node.number()) {
					for (int color = 0; color < colorSize; color++) {
						constraints.add(new int[] { -varNums[nodeNum][color], -varNums[other.number()][color] });
					}
				}
			}
		}
	}
	
	private void transitionConstraints(List<int[]> constraints) {
		for (MealyNode node : positiveTree.nodes()) {
			for (MealyTransition t : node.transitions()) {
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					final BooleanVariable nodeVar = xVar(node.number(), nodeColor);
					for (int childColor = 0; childColor < colorSize; childColor++) {
						final BooleanVariable childVar = xVar(t.dst().number(), childColor);
						final BooleanVariable relationVar = yVar(nodeColor, childColor,
								eventIndices.get(t.event()));
						constraints.add(new int[] { relationVar.number, -nodeVar.number, -childVar.number });
						constraints.add(new int[] { -relationVar.number, -nodeVar.number, childVar.number });
					}
				}
			}
		}
	}

	private void scenarioActionConstraints(List<int[]> constraints) {
		for (MealyNode node : positiveTree.nodes()) {
			for (MealyTransition t : node.transitions()) {
				final List<String> actionSequence = Arrays.asList(t.actions().getActions());
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					for (int ai = 0; ai < actions.size(); ai++) {
						final String action = actions.get(ai);
						constraints.add(new int[] {
								-xVar(node.number(), nodeColor).number,
								(actionSequence.contains(action) ? 1 : -1)
									* zVar(nodeColor, ai, eventIndices.get(t.event())).number
						});
					}
				}
			}
		}
	}

	// negative constraints
	
	private boolean negativeBasisAdded = false;
	
	private void negativeScenarioBasis(List<int[]> constraints) {
		if (!negativeBasisAdded) {
			for (int i = 0; i < colorSize; i++) {
				constraints.add(new int[] { (i == 0 ? 1 : -1) * xxVar(0, i, false).number });
			}
		}
	}
	
	private void globalNegativeScenarioBasis(List<int[]> constraints) {
		if (!negativeBasisAdded) {
			for (int i = 0; i < colorSize; i++) {
				constraints.add(new int[] { xxVar(0, i, true).number });
			}
		}
	}
	
    private final Set<NegativeMealyNode> unprocessedChildren = new HashSet<>();
	
	private void negativeScenarioPropagation(List<int[]> constraints, boolean isGlobal) {
		final int[] xxParent = new int[colorSize];
		final int[][] actionEq = new int[colorSize][actions.size()];
		final int[] xxChild = new int[colorSize];

		final NegativeScenarioTree tree =
				isGlobal ? globalNegativeTree : negativeTree;
		
		for (NegativeMealyNode node : tree.nodes()) {
			boolean xxParentFilled = false;
			for (MealyTransition edge : node.transitions()) {
				final NegativeMealyNode childNode = (NegativeMealyNode) edge.dst();
				if (unprocessedChildren.add(childNode)) {
					/*if (!isGlobal) {
						// not more than one color for a usual negative node
						for (int color1 = 0; color1 < colorSize; color1++) {
							final BooleanVariable v1 = xxVar(node.number(), color1, isGlobal);
							for (int color2 = 0; color2 < color1; color2++) {
								final BooleanVariable v2 = xxVar(node.number(), color2, isGlobal);					
								constraints.add(new int[] {
										-v1.number,
										-v2.number
								});
							}
						}
					}*/
					
					if (!xxParentFilled) {
						for (int color = 0; color < colorSize; color++) {
							xxParent[color] = xxVar(node.number(), color, isGlobal).number;
						}
						xxParentFilled = true;
					}
					final String event = edge.event();
					final List<String> actionList = Arrays.asList(edge.actions().getActions());
					for (int color = 0; color < colorSize; color++) {
						for (int ai = 0; ai < actions.size(); ai++) {
							final String action = actions.get(ai);
							final int sign = actionList.contains(action) ? 1 : -1;
							// we actually need the negation, this -sign:
							actionEq[color][ai] = -sign * zVar(color, ai, eventIndices.get(event)).number;
						}
					}
					for (int color = 0; color < colorSize; color++) {
						xxChild[color] = xxVar(childNode.number(), color, isGlobal).number;
					}
					for (int color1 = 0; color1 < colorSize; color1++) {
						for (int color2 = 0; color2 < colorSize; color2++) {
							final int[] constraint = new int[actions.size() + 3];
							System.arraycopy(actionEq[color1], 0, constraint, 0, actions.size());
							constraint[actions.size() + 0] = -xxParent[color1];
							constraint[actions.size() + 1] = -yVar(color1, color2,
									eventIndices.get(event)).number;
							constraint[actions.size() + 2] = xxChild[color2];
							constraints.add(constraint);
						}
					}
				}
			}
		}
	}
	
	private final Set<NegativeMealyNode> processedTerminalNodes = new HashSet<>();
	private final Set<Pair<NegativeMealyNode, NegativeMealyNode>> processedLoops = new HashSet<>();
	
	private void negativeScenarioTermination(List<int[]> constraints, boolean isGlobal) {
		final NegativeScenarioTree tree =
				isGlobal ? globalNegativeTree : negativeTree;
		
		for (NegativeMealyNode node : tree.nodes()) {
			final int nodeNumber = node.number();
			if (node.strongInvalid()) {
				if (processedTerminalNodes.add(node)) {
					for (int color = 0; color < colorSize; color++) {
						constraints.add(new int[] {
								-xxVar(nodeNumber, color, isGlobal).number
						});
					}
				}
			} else {
				for (NegativeMealyNode loop : node.loops()) {
					if (processedLoops.add(Pair.of(node, loop))) {
						final int loopNumber = loop.number();
						for (int color = 0; color < colorSize; color++) {
							constraints.add(new int[] {
									-xxVar(nodeNumber, color, isGlobal).number,
									-xxVar(loopNumber, color, isGlobal).number
							});
						}
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
		addBFSConstraints(constraints);
		return constraints;
	}
	
	public List<int[]> negativeConstraints() {
		final List<int[]> constraints = new ArrayList<>();
		negativeScenarioBasis(constraints);
		globalNegativeScenarioBasis(constraints);
		negativeBasisAdded = true;
		for (boolean isGlobal : Arrays.asList(false, true)) {
			negativeScenarioPropagation(constraints, isGlobal);
			negativeScenarioTermination(constraints, isGlobal);
		}
		return constraints;
	}
}

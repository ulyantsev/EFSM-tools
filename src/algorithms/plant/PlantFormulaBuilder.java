package algorithms.plant;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import algorithms.formula_builders.FastFormulaBuilder;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NegativePlantScenarioForest;
import structures.plant.PositivePlantScenarioForest;
import bnf_formulae.BooleanVariable;

public class PlantFormulaBuilder extends FastFormulaBuilder {
	private final PositivePlantScenarioForest positiveForest;
	private final NegativePlantScenarioForest negativeForest;

    /**
	 * Special forest (actually, tree) for G(...) specifications, which are processed separately
	 */
	private final NegativePlantScenarioForest globalNegativeForest;

	public PlantFormulaBuilder(int colorSize, PositivePlantScenarioForest positiveForest,
			NegativePlantScenarioForest negativeForest, NegativePlantScenarioForest globalNegativeForest,
			List<String> events, List<String> actions, boolean deterministic, boolean bfsConstraints,
            boolean complete) {
        super(colorSize, events, actions, deterministic, complete, bfsConstraints);
		this.positiveForest = positiveForest;
		this.negativeForest = negativeForest;
		this.globalNegativeForest = globalNegativeForest;
	}

	public BooleanVariable yVar(int from, int to, String event) {
		return yVar(from, to, eventIndices.get(event));
	}

	public BooleanVariable zVar(int state, String action) {
		return zVar(state, actionIndices.get(action));
	}
	
	public static BooleanVariable zVar(int state, int action) {
		return BooleanVariable.byName("z", state, action).get();
	}
	
	private void addPositiveVars() {
		for (int color = 0; color < colorSize; color++) {
			// scenario vars
			for (MooreNode node : positiveForest.nodes()) {
				vars.add(BooleanVariable.getOrCreate("x", node.number(), color));
			}
			// transition vars
			for (int ei = 0; ei < events.size(); ei++) {
				for (int childColor = 0; childColor < colorSize; childColor++) {
					vars.add(BooleanVariable.getOrCreate("y", color, childColor, ei));
				}
			}
			// action vars
			for (int ai = 0; ai < actions.size(); ai++) {
				vars.add(BooleanVariable.getOrCreate("z", color, ai));
			}
		}
        addBFSVars();
	}
	
	private void addNegativeVars() {
		for (MooreNode node : negativeForest.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xx", node.number(), color));
			}
		}
		for (MooreNode node : globalNegativeForest.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xxg", node.number(), color));
			}
		}
	}
	
	/*
	 * Each scenario node has at least one color
	 */
	private void eachNodeHasColorConstraints(List<int[]> constraints) {
		for (MooreNode node : positiveForest.nodes()) {
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
		for (MooreNode node : positiveForest.nodes()) {
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
	
	private void transitionConstraints(List<int[]> constraints) {
		for (MooreNode node : positiveForest.nodes()) {
			for (MooreTransition t : node.transitions()) {
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
                        if (deterministic) {
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
	}
	
	private void scenarioActionConstraints(List<int[]> constraints) {
		for (MooreNode node : positiveForest.nodes()) {
			final List<String> actionSequence = Arrays.asList(node.actions().getActions());
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				for (int ai = 0; ai < actions.size(); ai++) {
					final String action = actions.get(ai);
					constraints.add(new int[] {
							-xVar(node.number(), nodeColor).number,
							(actionSequence.contains(action) ? 1 : -1)
								* zVar(nodeColor, ai).number
					});
				}
			}
		}
	}
	
	private void negativeScenarioBasis(List<int[]> constraints) {
		for (MooreNode negRoot : negativeForest.roots()) {
			if (!negativeForest.processRoot(negRoot)) {
				continue;
			}
			for (MooreNode root : positiveForest.roots()) {
				if (root.actions().setEquals(negRoot.actions())) {
					for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
						constraints.add(new int[] {
								-xVar(root.number(), nodeColor).number,
								xxVar(negRoot.number(), nodeColor, false).number
						});
					}
				}
			}
		}
	}
	
	private void globalNegativeScenarioBasis(List<int[]> constraints) {
		if (globalNegativeForest.roots().size() > 1) {
			throw new AssertionError();
		}
		for (MooreNode root : globalNegativeForest.roots()) {
			if (!globalNegativeForest.processRoot(root)) {
				continue;
			}
			// the global negative root is colored in all colors
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(new int[] {
						xxVar(root.number(), nodeColor, true).number
				});
			}
		}
	}
	
	private void negativeScenarioPropagation(List<int[]> constraints, boolean isGlobal) {
		final NegativePlantScenarioForest forest =
				isGlobal ? globalNegativeForest : negativeForest;
		final int[] xxParent = new int[colorSize];
		final int[][] actionEq = new int[colorSize][actions.size()];
		final int[] xxChild = new int[colorSize];

		for (MooreNode node : forest.nodes()) {
			boolean xxParentFilled = false;
			for (MooreTransition edge : node.transitions()) {
				final MooreNode childNode = edge.dst();
				if (!forest.processChild(childNode)) {
					continue;
				}
				if (!xxParentFilled) {
					for (int color = 0; color < colorSize; color++) {
						xxParent[color] = xxVar(node.number(), color, isGlobal).number;
					}
					xxParentFilled = true;
				}
				final String event = edge.event();
				final List<String> actionList = Arrays.asList(childNode.actions().getActions());
				for (int color = 0; color < colorSize; color++) {
					for (int i = 0; i < actions.size(); i++) {
						final String action = actions.get(i);
						final int sign = actionList.contains(action) ? 1 : -1;
						// we actually need the negation, thus -sign:
						actionEq[color][i] = -sign * zVar(color, action).number;
					}
				}
				for (int color = 0; color < colorSize; color++) {
					xxChild[color] = xxVar(childNode.number(), color, isGlobal).number;
				}
				for (int color1 = 0; color1 < colorSize; color1++) {
					for (int color2 = 0; color2 < colorSize; color2++) {
						final int[] constraint = new int[actions.size() + 3];
						System.arraycopy(actionEq[color2], 0, constraint, 0, actions.size());
						constraint[actions.size() + 0] = -xxParent[color1];
						constraint[actions.size() + 1] = -yVar(color1, color2, event).number;
						constraint[actions.size() + 2] = xxChild[color2];
						constraints.add(constraint);
					}
				}
			}
		}
	}
	
	private void negativeScenarioTermination(List<int[]> constraints, boolean isGlobal) {
		final NegativePlantScenarioForest forest = isGlobal ? globalNegativeForest : negativeForest;
		for (MooreNode node : forest.terminalNodes()) {
			if (!forest.processTerminalNode(node)) {
				continue;
			}
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(new int[] {
						-xxVar(node.number(), nodeColor, isGlobal).number
				});
			}
		}
	}
	
	public void createVars() {
		addPositiveVars();
		addNegativeVars();
	}
	
	public List<int[]> positiveConstraints() {
		final List<int[]> constraints = new ArrayList<>();
        if (deterministic) {
            // only one initial state
            for (MooreNode root : positiveForest.roots()) {
                constraints.add(new int[] { xVar(root.number(), 0).number });
            }
        } else {
            // first node is always an initial state (but probably there are more)
            constraints.add(new int[] { xVar(0, 0).number });
        }
		transitionConstraints(constraints);
		eventCompletenessConstraints(constraints);
		eachNodeHasColorConstraints(constraints);
		eachNodeHasOnlyColorConstraints(constraints);
		scenarioActionConstraints(constraints);
        if (deterministic) {
            notMoreThanOneEdgeConstraints(constraints);
            addBFSConstraints(constraints);
        }
		return constraints;
	}
	
	public List<int[]> negativeConstraints() {
		final List<int[]> constraints = new ArrayList<>();
		negativeScenarioBasis(constraints);
		globalNegativeScenarioBasis(constraints);
		for (boolean isGlobal : Arrays.asList(false, true)) {
			negativeScenarioPropagation(constraints, isGlobal);
			negativeScenarioTermination(constraints, isGlobal);
		}
		return constraints;
	}
}

package algorithms.formula_builders;

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
	private final boolean bfsConstraints;
	
	private final List<BooleanVariable> vars = new ArrayList<>();
	
	private final NegativeScenarioTree globalNegativeTree;
	
	public FastAutomatonFormulaBuilder(int colorSize, ScenarioTree positiveForest,
			NegativeScenarioTree negativeTree, NegativeScenarioTree globalNegativeTree,
			List<String> events, List<String> actions, boolean complete, boolean bfsConstraints) {
		this.colorSize = colorSize;
		this.events = events;
		this.actions = actions;
		this.positiveTree = positiveForest;
		this.negativeTree = negativeTree;
		this.complete = complete;
		this.bfsConstraints = bfsConstraints;
		this.globalNegativeTree = globalNegativeTree;
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
	
	public static BooleanVariable xxVar(int node, int color, boolean isGlobal) {
		return BooleanVariable.byName(isGlobal ? "xxg" : "xx", node, color).get();
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
		if (bfsConstraints) {
			addBFSVars();
		}
	}
	
	private void addNegativeVars() {
		for (Node node : negativeTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xx", node.number(), color));
			}
		}
		for (Node node : globalNegativeTree.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(BooleanVariable.getOrCreate("xxg", node.number(), color));
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
	
	// negative constraints
	
	private boolean negativeBasisAdded = false;
	
	private void negativeScenarioBasis(List<int[]> constraints) {
		if (!negativeBasisAdded) {
			for (int i = 0; i < colorSize; i++) {
				constraints.add(new int[] {
						(i == 0 ? 1 : -1) * xxVar(0, i, false).number
				});
			}
		}
	}
	
	private void globalNegativeScenarioBasis(List<int[]> constraints) {
		if (!negativeBasisAdded) {
			for (int i = 0; i < colorSize; i++) {
				constraints.add(new int[] {
						xxVar(0, i, true).number
				});
			}
		}
	}
	
    private final Set<NegativeNode> unprocessedChildren = new HashSet<>();
	
	private void negativeScenarioPropagation(List<int[]> constraints, boolean isGlobal) {
		final int[] xxParent = new int[colorSize];
		final int[][] actionEq = new int[colorSize][actions.size()];
		final int[] xxChild = new int[colorSize];

		final NegativeScenarioTree tree =
				isGlobal ? globalNegativeTree : negativeTree;
		
		for (NegativeNode node : tree.nodes()) {
			boolean xxParentFilled = false;
			for (Transition edge : node.transitions()) {
				final NegativeNode childNode = (NegativeNode) edge.dst();
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
						for (int i = 0; i < actions.size(); i++) {
							final String action = actions.get(i);
							final int sign = actionList.contains(action) ? 1 : -1;
							// we actually need the negation, this -sign:
							actionEq[color][i] = -sign * zVar(color, action, event).number;
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
							constraint[actions.size() + 1] = -yVar(color1, color2, event).number;
							constraint[actions.size() + 2] = xxChild[color2];
							constraints.add(constraint);
						}
					}
				}
			}
		}
	}
	
	private final Set<NegativeNode> processedTerminalNodes = new HashSet<>();
	private final Set<Pair<NegativeNode, NegativeNode>> processedLoops = new HashSet<>();
	
	private void negativeScenarioTermination(List<int[]> constraints, boolean isGlobal) {
		final NegativeScenarioTree tree =
				isGlobal ? globalNegativeTree : negativeTree;
		
		for (NegativeNode node : tree.nodes()) {
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
				for (NegativeNode loop : node.loops()) {
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
		if (bfsConstraints) {
			parentConstraints(constraints);
			pDefinitions(constraints);
			tDefinitions(constraints);
			childrenOrderConstraints(constraints);
		}
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
	
	// BFS constraints
	
	private BooleanVariable pVar(int j, int i) {
		return BooleanVariable.byName("p", j, i).get();
	}
	
	private BooleanVariable tVar(int i, int j) {
		return BooleanVariable.byName("t", i, j).get();
	}
	
	private BooleanVariable mVar(String event, int i, int j) {
		return BooleanVariable.byName("m", event, i, j).get();
	}
	
	private void addBFSVars() {
		// p_ji, t_ij
		for (int i = 0; i < colorSize; i++) {
			for (int j = i + 1; j < colorSize; j++) {
				vars.add(BooleanVariable.getOrCreate("p", j, i));
				vars.add(BooleanVariable.getOrCreate("t", i, j));
			}
		}
		if (events.size() > 2) {
			// m_efij
			for (String e : events) {
				for (int i = 0; i < colorSize; i++) {
					for (int j = i + 1; j < colorSize; j++) {
						vars.add(BooleanVariable.getOrCreate("m", e, i, j));
					}
				}
			}
		}
	}
	
	private void parentConstraints(List<int[]> constraints) {
		for (int j = 1; j < colorSize; j++) {
			final int[] options = new int[j];
			for (int i = 0; i < j; i++) {
				options[i] = pVar(j, i).number;
			}
			constraints.add(options);
		}
		
		for (int k = 0; k < colorSize; k++) {
			for (int i = k + 1; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					constraints.add(new int[] {
							-pVar(j, i).number,
							-pVar(j + 1, k).number
					});
				}
			}
		}
	}
	
	private void pDefinitions(List<int[]> constraints) {
		for (int i = 0; i < colorSize; i++) {
			for (int j = i + 1; j < colorSize; j++) {
				constraints.add(new int[] {
						-pVar(j, i).number,
						tVar(i, j).number
				});
				final int[] options = new int[i + 2];
				for (int k = i - 1; k >= 0; k--) {
					constraints.add(new int[] {
							-pVar(j, i).number,
							-tVar(k, j).number
					});
					options[k] = tVar(k, j).number;
				}
				options[i] = -tVar(i, j).number;
				options[i + 1] = pVar(j, i).number;
				constraints.add(options);
			}
		}
	}
	
	private void tDefinitions(List<int[]> constraints) {
		for (int i = 0; i < colorSize; i++) {
			for (int j = i + 1; j < colorSize; j++) {
				final int[] options = new int[events.size() + 1];
				for (int ei = 0; ei < events.size(); ei++) {
					final String e = events.get(ei);
					constraints.add(new int[] {
							-yVar(i, j, e).number,
							tVar(i, j).number
					});
					options[ei] = yVar(i, j, e).number;
				}
				options[events.size()] = -tVar(i, j).number;
				constraints.add(options);
			}
		}
	}
	
	private void childrenOrderConstraints(List<int[]> constraints) {
		if (events.size() > 2) {
			// m definitions
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize; j++) {
					for (int eventIndex1 = 0; eventIndex1 < events.size(); eventIndex1++) {
						final String e1 = events.get(eventIndex1);
						constraints.add(new int[] {
								-mVar(e1, i, j).number,
								yVar(i, j, e1).number
						});
						final int[] options = new int[eventIndex1 + 2];
						for (int eventIndex2 = eventIndex1 - 1; eventIndex2 >= 0; eventIndex2--) {
							final String e2 = events.get(eventIndex2);
							constraints.add(new int[] {
									-mVar(e1, i, j).number,
									-yVar(i, j, e2).number
							});
							options[eventIndex2] = yVar(i, j, e2).number;
						}
						options[eventIndex1] = -yVar(i, j, e1).number;
						options[eventIndex1 + 1] = mVar(e1, i, j).number;
						constraints.add(options);
					}
				}
			}
			// children constraints
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					for (int k = 0; k < events.size(); k++) {
						for (int n = k + 1; n < events.size(); n++) {
							constraints.add(new int[] {
									-pVar(j, i).number,
									-pVar(j + 1, i).number,
									-mVar(events.get(n), i, j).number,
									-mVar(events.get(k), i, j + 1).number
							});
						}
					}
				}
			}
		} else {
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					constraints.add(new int[] {
							-pVar(j, i).number,
							-pVar(j + 1, i).number,
							yVar(i, j, events.get(0)).number
					});
				}
			}
		}
	}
}

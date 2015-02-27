package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanVariable;
import qbf.reduction.FormulaList;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;

/**
 * (c) Igor Buzhinsky
 */

public abstract class FormulaBuilder {
	protected final int colorSize;
	protected final List<String> events;
	protected final List<String> actions;
	protected final ScenariosTree tree;
	protected final boolean complete;
	protected final List<BooleanVariable> existVars = new ArrayList<>();
	
	public FormulaBuilder(int colorSize, ScenariosTree tree, boolean complete,
			List<String> events, List<String> actions) {
		this.colorSize = colorSize;
		this.events = events;
		this.actions = actions;
		this.tree = tree;
		this.complete = complete;
	}
	
	protected BooleanVariable xVar(int state, int color) {
		return BooleanVariable.byName("x", state, color).get();
	}
	
	protected BooleanVariable yVar(int from, int to, String event) {
		return BooleanVariable.byName("y", from, to, event).get();
	}
	
	protected BooleanVariable zVar(int from, String action, String event) {
		return BooleanVariable.byName("z", from, action, event).get();
	}
	
	private BooleanVariable pVar(int j, int i) {
		return BooleanVariable.byName("p", j, i).get();
	}
	
	private BooleanVariable tVar(int i, int j) {
		return BooleanVariable.byName("t", i, j).get();
	}
	
	private BooleanVariable mVar(String event, int i, int j) {
		return BooleanVariable.byName("m", event, i, j).get();
	}
	
	protected void addColorVars() {
		// color variables x_#node_color
		for (Node node : tree.getNodes()) {
			for (int color = 0; color < colorSize; color++) {
				existVars.add(new BooleanVariable("x", node.getNumber(), color));
			}
		}
	}
	
	protected void addTransitionVars(boolean addActionVars) {
		for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
			for (String e : events) {
				// transition variables y_color_childColor_event_formula
				for (int childColor = 0; childColor < colorSize; childColor++) {
					existVars.add(new BooleanVariable("y", nodeColor, childColor, e));
				}
				if (addActionVars) {
					// action variables z_color_action_event_formula
					for (String action : actions) {
						existVars.add(new BooleanVariable("z", nodeColor, action, e));
					}
				}
			}
		}
	}
	
	/*
	 * Each node has some color (can be derived from the action consistency)
	 */
	private BooleanFormula eachNodeHasColorConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (Node node : tree.getNodes()) {
			List<BooleanFormula> terms = new ArrayList<>();
			for (int color = 0; color < colorSize; color++) {
				terms.add(xVar(node.getNumber(), color));
			}
			constraints.add(BinaryOperation.or(terms));
		}
		return constraints.assemble("scenario constraints: each tree node has a color");
	}
	
	/*
	 * No node can have two colors simultaneously.
	 */
	private BooleanFormula eachNodeHasOnlyColorConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (Node node : tree.getNodes()) {
			for (int color1 = 0; color1 < colorSize; color1++) {
				for (int color2 = 0; color2 < color1; color2++) {
					BooleanVariable v1 = xVar(node.getNumber(), color1);
					BooleanVariable v2 = xVar(node.getNumber(), color2);					
					constraints.add(v1.not().or(v2.not()));
				}
			}
		}
		return constraints.assemble("scenario constraints: each tree node has at most one color");
	}
	
	// z's are consistent with scenarios
	private BooleanFormula actionScenarioConsistencyConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		
		for (Node node : tree.getNodes()) {
			FormulaList options = new FormulaList(BinaryOperations.OR);
			for (int i = 0; i < colorSize; i++) {
				FormulaList zConstraints = new FormulaList(BinaryOperations.AND);
				zConstraints.add(xVar(node.getNumber(), i));
				for (Transition t : node.getTransitions()) {
					List<String> actionSequence = Arrays.asList(t.getActions().getActions());
					for (String action : actions) {
						BooleanFormula f = zVar(i, action, t.getEvent());
						if (!actionSequence.contains(action)) {
							f = f.not();
						}
						zConstraints.add(f);
					}
				}
				options.add(zConstraints.assemble());
			}
			constraints.add(options.assemble());
		}

		return constraints.assemble("additional scenario constraints: z's are consistent with scenarios");
	}
	
	private BooleanFormula consistencyConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		Map<Node, Set<Node>> adjacent = AdjacentCalculator.getAdjacent(tree);
		for (Node node : tree.getNodes()) {
			// removing the non-determinism of hash maps
			adjacent.get(node).stream()
				.sorted((n1, n2) -> n1.getNumber() - n2.getNumber())
				.filter(other -> other.getNumber() < node.getNumber())
				.forEach(other -> {
					for (int color = 0; color < colorSize; color++) {
						BooleanVariable v1 = xVar(node.getNumber(), color);
						BooleanVariable v2 = xVar(other.getNumber(), color);
						constraints.add(v1.and(v2).not());
					}
				});
		}
		return constraints.assemble("scenario constraints: adjacency graph constraints");
	}
	
	private BooleanFormula notMoreThanOneEdgeConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String e : events) {
				for (int i2 = 0; i2 < colorSize; i2++) {
					for (int i3 = 0; i3 < colorSize; i3++) {
						if (i3 != i2) {
							constraints.add(yVar(i1, i2, e).and(yVar(i1, i3, e)).not());
						}
					}
				}
			}
		}
		return constraints.assemble("scenario constraints: at most one edge from a fixed state for a fixed event");
	}

	private BooleanFormula transitionConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (Node node : tree.getNodes()) {
			for (Transition t : node.getTransitions()) {
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					for (int childColor = 0; childColor < colorSize; childColor++) {
						BooleanVariable nodeVar = xVar(node.getNumber(), nodeColor);
						BooleanVariable childVar = xVar(t.getDst().getNumber(), childColor);
						BooleanVariable relationVar = yVar(nodeColor, childColor, t.getEvent());
						constraints.add(BinaryOperation.or(relationVar, nodeVar.not(), childVar.not()));
						constraints.add(BinaryOperation.or(relationVar.not(), nodeVar.not(), childVar));
					}
				}
			}
		}
		return constraints.assemble("scenario constraints: connection between x's and y's");
	}
	
	// induce complete FSMs
	private BooleanFormula eventCompletenessConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String e : events) {
				FormulaList options = new FormulaList(BinaryOperations.OR);
				for (int i2 = 0; i2 < colorSize; i2++) {
					options.add(yVar(i1, i2, e));
				}
				constraints.add(options.assemble());
			}
		}
		return constraints.assemble("induce a complete FSM");
	}
	
	// if there exists z, then it exists for some transition (unnecessary if
	// completeness is enabled)
	private BooleanFormula actionTransitionExistenceConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);

		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String action : actions) {
				for (String e : events) {
					FormulaList options = new FormulaList(BinaryOperations.OR);
					for (int i2 = 0; i2 < colorSize; i2++) {
						options.add(yVar(i1, i2, e));
					}
					constraints.add(zVar(i1, action, e).implies(options.assemble()));
				}
			}
		}

		return constraints
				.assemble("additional scenario constraints: if there exists z, then it exists for some transition");
	}
	
	protected FormulaList scenarioConstraints(boolean includeActionConstrains) {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		// first node has color 0
		constraints.add(xVar(0, 0));
		constraints.add(eachNodeHasOnlyColorConstraints());
		constraints.add(consistencyConstraints());
		constraints.add(notMoreThanOneEdgeConstraints());
		constraints.add(transitionConstraints());
		
		if (complete) {
			constraints.add(eventCompletenessConstraints());
		}
		
		if (includeActionConstrains) {
			 constraints.add(actionScenarioConsistencyConstraints());
             if (!complete) {
            	 constraints.add(actionTransitionExistenceConstraints());
             }
		} else {
			constraints.add(eachNodeHasColorConstraints());
		}
		
		addBFSVars();
		constraints.add(parentConstraints());
		constraints.add(pDefinitions());
		constraints.add(tDefinitions());
		constraints.add(childrenOrderConstraints());
		
		return constraints;
	}
	
	private void addBFSVars() {
		// p_ji, t_ij
		for (int i = 0; i < colorSize; i++) {
			for (int j = i + 1; j < colorSize; j++) {
				existVars.add(new BooleanVariable("p", j, i));
				existVars.add(new BooleanVariable("t", i, j));
			}
		}
		if (events.size() > 2) {
			// m_efij
			for (String e : events) {
				for (int i = 0; i < colorSize; i++) {
					for (int j = i + 1; j < colorSize; j++) {
						existVars.add(new BooleanVariable("m", e, i, j));
					}
				}
			}
		}
	}
	
	private BooleanFormula parentConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int j = 1; j < colorSize; j++) {
			FormulaList options = new FormulaList(BinaryOperations.OR);
			for (int i = 0; i < j; i++) {
				options.add(pVar(j, i));
			}
			constraints.add(options.assemble());
		}
		
		for (int k = 0; k < colorSize; k++) {
			for (int i = k + 1; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					constraints.add(pVar(j, i).implies(pVar(j + 1, k).not()));
				}
			}
		}
		
		return constraints.assemble();
	}
	
	private BooleanFormula pDefinitions() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int i = 0; i < colorSize; i++) {
			for (int j = i + 1; j < colorSize; j++) {
				FormulaList definition = new FormulaList(BinaryOperations.AND);
				definition.add(tVar(i, j));
				for (int k = i - 1; k >=0; k--) {
					definition.add(tVar(k, j).not());
				}
				constraints.add(pVar(j, i).equivalent(definition.assemble()));
			}
		}
		return constraints.assemble();
	}
	
	private BooleanFormula tDefinitions() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int i = 0; i < colorSize; i++) {
			for (int j = i + 1; j < colorSize; j++) {
				FormulaList definition = new FormulaList(BinaryOperations.OR);
				for (String e : events) {
					definition.add(yVar(i, j, e));
				}
				constraints.add(tVar(i, j).equivalent(definition.assemble()));
			}
		}
		return constraints.assemble();
	}
	
	private BooleanFormula childrenOrderConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		if (events.size() > 2) {
			// m definitions
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize; j++) {
					for (int eventIndex1 = 0; eventIndex1 < events.size(); eventIndex1++) {
						String e1 = events.get(eventIndex1);
						FormulaList definition = new FormulaList(BinaryOperations.AND);
						definition.add(yVar(i, j, e1));
						for (int eventIndex2 = eventIndex1 - 1; eventIndex2 >= 0; eventIndex2--) {
							String e2 = events.get(eventIndex2);
							definition.add(yVar(i, j, e2).not());
						}
						constraints.add(mVar(e1, i, j).equivalent(definition.assemble()));
					}
				}
			}
			// children constraints
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					for (int k = 0; k < events.size(); k++) {
						for (int n = k + 1; n < events.size(); n++) {
							constraints.add(
									BinaryOperation.and(
											pVar(j, i), pVar(j + 1, i),
											mVar(events.get(n), i, j)
									).implies(
											mVar(events.get(k), i, j + 1).not()
									)
							);
						}
					}
				}
			}
		} else {
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					constraints.add(
							pVar(j, i).and(pVar(j + 1, i))
							.implies(yVar(i, j, events.get(0)))
					);
				}
			}
		}
		return constraints.assemble();
	}
}

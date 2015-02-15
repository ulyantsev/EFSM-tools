package algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanVariable;
import qbf.reduction.FormulaList;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;
import bool.MyBooleanExpression;

/**
 * (c) Igor Buzhinsky
 */

public abstract class FormulaBuilder {
	protected final int colorSize;
	protected final List<String> events;
	protected final List<String> actions;
	protected final List<EventExpressionPair> efPairs;
	protected final ScenariosTree tree;
	protected final boolean eventCompleteness;
	protected final boolean bfsConstraints;
	protected final List<BooleanVariable> existVars = new ArrayList<>();
	
	public static class EventExpressionPair {
		public final String event;
		public final MyBooleanExpression expression;
		
		public EventExpressionPair(String event, MyBooleanExpression expression) {
			this.event = event;
			this.expression = expression;
		}
		
		@Override
		public String toString() {
			return event + " [" + expression + "]";
		}
		
		public static List<String> getEvents(List<EventExpressionPair> efPairs) {
			return new ArrayList<>(new TreeSet<>(efPairs.stream().map(p -> p.event).collect(Collectors.toList())));
		}
	}
	
	/*public static List<EventExpressionPair> getEventExpressionPairs(ScenariosTree tree) {
		final List<EventExpressionPair> efPairs = new ArrayList<>();
		for (String event : tree.getEvents()) {
			for (MyBooleanExpression f : tree.getPairsEventExpression().get(event)) {
				efPairs.add(new EventExpressionPair(event, f));
			}
		}
		return efPairs;
	}*/
	
	public FormulaBuilder(int colorSize, ScenariosTree tree, boolean eventCompleteness, boolean bfsConstraints,
			List<EventExpressionPair> efPairs, List<String> actions) {
		this.colorSize = colorSize;
		this.events = EventExpressionPair.getEvents(efPairs);
		this.actions = actions;
		this.efPairs = efPairs;
		this.tree = tree;
		this.eventCompleteness = eventCompleteness;
		this.bfsConstraints = bfsConstraints;
	}
	
	protected BooleanVariable xVar(int state, int color) {
		return BooleanVariable.byName("x", state, color).get();
	}
	
	protected BooleanVariable yVar(int from, int to, String event, MyBooleanExpression f) {
		return BooleanVariable.byName("y", from, to, event, f).get();
	}
	
	protected BooleanVariable zVar(int from, String action, String event, MyBooleanExpression f) {
		return BooleanVariable.byName("z", from, action, event, f).get();
	}
	
	private BooleanVariable pVar(int j, int i) {
		return BooleanVariable.byName("p", j, i).get();
	}
	
	private BooleanVariable tVar(int i, int j) {
		return BooleanVariable.byName("t", i, j).get();
	}
	
	private BooleanVariable mVar(String event, MyBooleanExpression f, int i, int j) {
		return BooleanVariable.byName("m", event, f, i, j).get();
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
			for (EventExpressionPair p : efPairs) {
				// transition variables y_color_childColor_event_formula
				for (int childColor = 0; childColor < colorSize; childColor++) {
					existVars.add(new BooleanVariable("y", nodeColor, childColor, p.event, p.expression));
				}
				if (addActionVars) {
					// action variables z_color_action_event_formula
					for (String action : actions) {
						existVars.add(new BooleanVariable("z", nodeColor, action, p.event, p.expression));
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
	
	// if there exists z, then it exists for some transition (unnecessary if completeness is enabled)
	private BooleanFormula actionTransitionExistenceConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String action : actions) {
				for (EventExpressionPair p : efPairs) {
					FormulaList options = new FormulaList(BinaryOperations.OR);
					for (int i2 = 0; i2 < colorSize; i2++) {
						options.add(yVar(i1, i2, p.event, p.expression));
					}
					constraints.add(zVar(i1, action, p.event, p.expression).implies(options.assemble()));
				}
			}
		}
		
		return constraints.assemble("additional scenario constraints: if there exists z, then it exists for some transition");
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
						BooleanFormula f = zVar(i, action, t.getEvent(), t.getExpr());
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
			// removing non-determinism of hash maps
			List<Node> otherNodes = adjacent.get(node).stream()
				.sorted((n1, n2) -> n1.getNumber() - n2.getNumber())
				.collect(Collectors.toList());
			
			for (Node other : otherNodes) {
				if (other.getNumber() < node.getNumber()) {
					for (int color = 0; color < colorSize; color++) {
						BooleanVariable v1 = xVar(node.getNumber(), color);
						BooleanVariable v2 = xVar(other.getNumber(), color);
						constraints.add(v1.not().or(v2.not()));
					}
				}
			}
		}
		return constraints.assemble("scenario constraints: adjacency graph constraints");
	}
	
	private BooleanFormula notMoreThanOneEdgeConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		Set<String> was = new HashSet<>();
		for (Node node : tree.getNodes()) {
			for (Transition t : node.getTransitions()) {
				String key = t.getEvent() + "_" + t.getExpr();
				if (!was.contains(key)) {
					was.add(key);
					for (int parentColor = 0; parentColor < colorSize; parentColor++) {
						for (int color1 = 0; color1 < colorSize; color1++) {
							for (int color2 = 0; color2 < color1; color2++) {
								BooleanVariable v1 = yVar(parentColor, color1, t.getEvent(), t.getExpr());
								BooleanVariable v2 = yVar(parentColor, color2, t.getEvent(), t.getExpr());
								constraints.add(v1.not().or(v2.not()));
							}
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
						BooleanVariable relationVar = yVar(nodeColor, childColor, t.getEvent(), t.getExpr());
						constraints.add(BinaryOperation.or(relationVar, nodeVar.not(), childVar.not()));
						constraints.add(BinaryOperation.or(relationVar.not(), nodeVar.not(), childVar));
					}
				}
			}
		}
		return constraints.assemble("scenario constraints: transition constraints (...)");
	}
	
	// induce complete FSMs
	private BooleanFormula eventCompletenessConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (EventExpressionPair p : efPairs) {
				FormulaList options = new FormulaList(BinaryOperations.OR);
				for (int i2 = 0; i2 < colorSize; i2++) {
					options.add(yVar(i1, i2, p.event, p.expression));
				}
				constraints.add(options.assemble());
			}
		}
		return constraints.assemble("induce a complete FSM");
	}
	
	protected FormulaList scenarioConstraints(boolean includeActionConstrains) {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		// first node has color 0
		constraints.add(xVar(0, 0));
		constraints.add(eachNodeHasOnlyColorConstraints());
		constraints.add(consistencyConstraints());
		constraints.add(notMoreThanOneEdgeConstraints());
		constraints.add(transitionConstraints());
		
		if (eventCompleteness) {
			constraints.add(eventCompletenessConstraints());
		}
		
		if (includeActionConstrains) {
			constraints.add(actionScenarioConsistencyConstraints());
			if (!eventCompleteness) {
				constraints.add(actionTransitionExistenceConstraints());
			}
		} else {
			constraints.add(eachNodeHasColorConstraints());
		}
		
		if (bfsConstraints) {
			addBFSVars();
			constraints.add(parentConstraints());
			constraints.add(pDefinitions());
			constraints.add(tDefinitions());
			constraints.add(childrenOrderConstraints());
		}
		
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
		if (efPairs.size() > 2) {
			// m_efij
			for (EventExpressionPair p : efPairs) {
				for (int i = 0; i < colorSize; i++) {
					for (int j = i + 1; j < colorSize; j++) {
						existVars.add(new BooleanVariable("m", p.event, p.expression, i, j));
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
				for (EventExpressionPair p : efPairs) {
					definition.add(yVar(i, j, p.event, p.expression));
				}
				constraints.add(tVar(i, j).equivalent(definition.assemble()));
			}
		}
		return constraints.assemble();
	}
	
	private BooleanFormula childrenOrderConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		if (efPairs.size() > 2) {
			// m definitions
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize; j++) {
					for (int pairIndex1 = 0; pairIndex1 < efPairs.size(); pairIndex1++) {
						EventExpressionPair p1 = efPairs.get(pairIndex1);
						FormulaList definition = new FormulaList(BinaryOperations.AND);
						definition.add(yVar(i, j, p1.event, p1.expression));
						for (int pairIndex2 = pairIndex1 - 1; pairIndex2 >= 0; pairIndex2--) {
							EventExpressionPair p2 = efPairs.get(pairIndex2);
							definition.add(yVar(i, j, p2.event, p2.expression).not());
						}
						constraints.add(mVar(p1.event, p1.expression, i, j).equivalent(definition.assemble()));
					}
				}
			}
			// children constraints
			for (int i = 0; i < colorSize; i++) {
				for (int j = i + 1; j < colorSize - 1; j++) {
					for (int k = 0; k < efPairs.size(); k++) {
						for (int n = k + 1; n < efPairs.size(); n++) {
							constraints.add(
									BinaryOperation.and(
											pVar(j, i), pVar(j + 1, i),
											mVar(efPairs.get(n).event, efPairs.get(n).expression, i, j)
									).implies(
											mVar(efPairs.get(k).event, efPairs.get(k).expression, i, j + 1).not()
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
							.implies(yVar(i, j, efPairs.get(0).event, efPairs.get(0).expression))
					);
				}
			}
		}
		return constraints.assemble();
	}
}
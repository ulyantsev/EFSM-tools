package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class SatFormulaBuilder {
	private final int colorSize;

	private final List<String> events;
	private final List<String> actions;
	private final Map<String, List<MyBooleanExpression>> pairsEventExpression;
	private final ScenariosTree tree;
	
	private final List<BooleanVariable> vars = new ArrayList<>();
	
	private final boolean eventCompleteness;

	public SatFormulaBuilder(ScenariosTree tree, int colorSize, boolean eventCompleteness) {
		this.colorSize = colorSize;
		this.tree = tree;
		events = Arrays.asList(tree.getEvents());
		actions = tree.getActions();
		pairsEventExpression = tree.getPairsEventExpression();
		this.eventCompleteness = eventCompleteness;
	}

	private BooleanVariable xVar(int state, int color) {
		return BooleanVariable.byName("x", state, color).get();
	}
	
	private BooleanVariable yVar(int from, int to, String event, MyBooleanExpression f) {
		return BooleanVariable.byName("y", from, to, event, f).get();
	}
	
	private void addColorVars() {
		// color variables x_#node_color
		for (Node node : tree.getNodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(new BooleanVariable("x", node.getNumber(), color));
			}
		}
	}
	
	private void addTransitionVars() {
		// transition variables
		// action variables
		for (Node node : tree.getNodes()) {
			for (Transition t : node.getTransitions()) {
				if (!BooleanVariable.byName("y", 0, 0, t.getEvent(), t.getExpr()).isPresent()) { // why (0, 0)?
					for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
						for (int childColor = 0; childColor < colorSize; childColor++) {
							vars.add(new BooleanVariable("y", nodeColor, childColor, t.getEvent(), t.getExpr()));
						}
						for (String action : actions) {
							vars.add(new BooleanVariable("z", nodeColor, action, t.getEvent(), t.getExpr()));
						}
					}
				}
			}
		}
	}
	
	/*
	 * Each node has some color (can be derived from the action consistency)
	 */
	@SuppressWarnings("unused")
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
			for (String event : events) {
				for (MyBooleanExpression f : pairsEventExpression.get(event)) {
					FormulaList options = new FormulaList(BinaryOperations.OR);
					for (int i2 = 0; i2 < colorSize; i2++) {
						options.add(yVar(i1, i2, event, f));
					}
					constraints.add(options.assemble());
				}
			}
		}
		return constraints.assemble("induce a complete FSM");
	}
	
	private FormulaList scenarioConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		// first node has color 0
		constraints.add(xVar(0, 0));
		//constraints.add(eachNodeHasColorConstraints());
		constraints.add(eachNodeHasOnlyColorConstraints());
		constraints.add(consistencyConstraints());
		constraints.add(notMoreThanOneEdgeConstraints());
		constraints.add(transitionConstraints());
		
		// TODO add BFS constraints
		
		if (eventCompleteness) {
			constraints.add(eventCompletenessConstraints());
		}
		
		return constraints;
	}
	
	public BooleanFormula getFormula() {
		addColorVars();
		addTransitionVars();
		return scenarioConstraints().assemble();
	}
}

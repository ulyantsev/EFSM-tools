package algorithms.formula_builders;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scenario.StringActions;
import structures.plant.MooreNode;
import structures.plant.MooreTransition;
import structures.plant.NegativePlantScenarioForest;
import structures.plant.PositivePlantScenarioForest;
import bnf_formulae.BinaryOperation;
import bnf_formulae.BinaryOperations;
import bnf_formulae.BooleanFormula;
import bnf_formulae.BooleanVariable;
import bnf_formulae.FormulaList;

/**
 * (c) Igor Buzhinsky
 */

public class PlantFormulaBuilder {
	protected final int colorSize;
	protected final List<String> events;
	protected final List<String> actions;
	protected final PositivePlantScenarioForest positiveForest;
	protected final NegativePlantScenarioForest negativeForest;
	
	/**
	 * Special forest (actually, tree) for G(...) specifications, which are processed separately
	 */
	protected final NegativePlantScenarioForest globalNegativeForest;
	
	protected final List<BooleanVariable> vars = new ArrayList<>();
	
	public PlantFormulaBuilder(int colorSize, PositivePlantScenarioForest positiveForest,
			NegativePlantScenarioForest negativeForest, NegativePlantScenarioForest globalNegativeForest,
			List<String> events, List<String> actions) {
		this.colorSize = colorSize;
		this.events = events;
		this.actions = actions;
		this.positiveForest = positiveForest;
		this.negativeForest = negativeForest;
		this.globalNegativeForest = globalNegativeForest;
	}
	
	public static BooleanVariable xVar(int node, int color) {
		return BooleanVariable.byName("x", node, color).get();
	}
	
	public static BooleanVariable yVar(int from, int to, String event) {
		return BooleanVariable.byName("y", from, to, event).get();
	}
	
	public static BooleanVariable zVar(int state, String action) {
		return BooleanVariable.byName("z", state, action).get();
	}
	
	public static BooleanVariable xxVar(int node, int color, boolean isGlobal) {
		return BooleanVariable.byName(isGlobal ? "xxg" : "xx", node, color).get();
	}
	
	private void addPositiveVars() {
		for (int color = 0; color < colorSize; color++) {
			// scenario vars
			for (MooreNode node : positiveForest.nodes()) {
				vars.add(BooleanVariable.getOrCreate("x", node.number(), color));
			}
			// transition vars
			for (String e : events) {
				for (int childColor = 0; childColor < colorSize; childColor++) {
					vars.add(BooleanVariable.getOrCreate("y", color, childColor, e));
				}
			}
			// action vars
			for (String action : actions) {
				vars.add(BooleanVariable.getOrCreate("z", color, action));
			}
		}
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
	 * Each node has at least one color
	 */
	private void eachNodeHasColorConstraints(FormulaList constraints) {
		for (MooreNode node : positiveForest.nodes()) {
			final FormulaList terms = new FormulaList(BinaryOperations.OR);
			for (int color = 0; color < colorSize; color++) {
				terms.add(xVar(node.number(), color));
			}
			constraints.add(terms.assemble());
		}
		// return constraints.assemble("scenario constraints: each tree node has a color");
	}

	private void eachNodeHasOnlyColorConstraints(FormulaList constraints) {
		for (MooreNode node : positiveForest.nodes()) {
			for (int color1 = 0; color1 < colorSize; color1++) {
				for (int color2 = 0; color2 < color1; color2++) {
					final BooleanVariable v1 = xVar(node.number(), color1);
					final BooleanVariable v2 = xVar(node.number(), color2);					
					constraints.add(v1.not().or(v2.not()));
				}
			}
		}
		//return constraints.assemble("scenario constraints: each tree node has at most one color");
	}
	
	private void transitionConstraints(FormulaList constraints) {
		for (MooreNode node : positiveForest.nodes()) {
			for (MooreTransition t : node.transitions()) {
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					final BooleanVariable nodeVar = xVar(node.number(), nodeColor);
					for (int childColor = 0; childColor < colorSize; childColor++) {
						final BooleanVariable childVar = xVar(t.dst().number(), childColor);
						final BooleanVariable relationVar = yVar(nodeColor, childColor, t.event());
						constraints.add(BinaryOperation.or(relationVar, nodeVar.not(), childVar.not()));
					}
				}
			}
		}
		//return constraints.assemble("scenario constraints: connection between x's and y's");
	}
	
	private BooleanFormula actionEquality(int state, StringActions a) {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		final List<String> actionSequence = Arrays.asList(a.getActions());
		for (String action : actions) {
			final BooleanVariable var = zVar(state, action);
			constraints.add(actionSequence.contains(action) ? var : var.not());
		}
		return constraints.assemble();
	}
	
	private void scenarioActionConstraints(FormulaList constraints) {
		for (MooreNode node : positiveForest.nodes()) {
			final List<String> actionSequence = Arrays.asList(node.actions().getActions());
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				// actionEquality was inlined to produce CNF constraints
				for (String action : actions) {
					final BooleanVariable var = zVar(nodeColor, action);
					final BooleanFormula term = actionSequence.contains(action) ? var : var.not();
					constraints.add(xVar(node.number(), nodeColor).implies(term));
				}
			}
		}
		//return constraints.assemble("scenario action constraints");
	}
	
	private void eventCompletenessConstraints(FormulaList constraints) {
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String e : events) {
				final FormulaList options = new FormulaList(BinaryOperations.OR);
				for (int i2 = 0; i2 < colorSize; i2++) {
					options.add(yVar(i1, i2, e));
				}
				constraints.add(options.assemble());
			}
		}
		//return constraints.assemble("induce a complete automaton");
	}
	
	private void negativeScenarioBasis(List<BooleanFormula> constraints) {
		for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
			for (MooreNode root : positiveForest.roots()) {
				for (MooreNode negRoot : negativeForest.roots()) {
					if (root.actions().equals(negRoot.actions())) {
						constraints.add(xVar(root.number(), nodeColor)
								.implies(xxVar(negRoot.number(), nodeColor, false)));
					}
				}
			}
		}
		//return constraints.assemble("negative scenario basis");
	}
	
	private void globalNegativeScenarioBasis(List<BooleanFormula> constraints) {
		if (globalNegativeForest.roots().size() > 1) {
			throw new AssertionError();
		}
		for (MooreNode root : globalNegativeForest.roots()) {
			// the global negative root is colored in all colors
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(xxVar(root.number(), nodeColor, true));
			}
		}
		//return constraints.assemble("global negative scenario basis");
	}
	
	private void negativeScenarioPropagation(List<BooleanFormula> constraints, boolean isGlobal) {
		for (MooreNode node : (isGlobal ? globalNegativeForest : negativeForest).nodes()) {
			final BooleanFormula[] xxParent = new BooleanFormula[colorSize];
			for (int color = 0; color < colorSize; color++) {
				xxParent[color] = xxVar(node.number(), color, isGlobal);
			}
			for (MooreTransition edge : node.transitions()) {
				final MooreNode childNode = edge.dst();
				final String event = edge.event();
				final BooleanFormula[] actionEq = new BooleanFormula[colorSize];
				for (int color = 0; color < colorSize; color++) {
					actionEq[color] = actionEquality(color, childNode.actions());
				}
				final BooleanFormula[] xxChild = new BooleanFormula[colorSize];
				for (int color = 0; color < colorSize; color++) {
					xxChild[color] = xxVar(childNode.number(), color, isGlobal);
				}
				for (int color1 = 0; color1 < colorSize; color1++) {
					for (int color2 = 0; color2 < colorSize; color2++) {
						constraints.add(BinaryOperation.and(
								xxParent[color1],
								yVar(color1, color2, event),
								actionEq[color2]).implies(
								xxChild[color2]));
					}
				}
			}
		}
		//return constraints.assemble("negative scenario propagation");
	}
	
	private void negativeScenarioTermination(List<BooleanFormula> constraints, boolean isGlobal) {
		for (MooreNode node : (isGlobal ? globalNegativeForest : negativeForest).terminalNodes()) {
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(xxVar(node.number(), nodeColor, isGlobal).not());
			}
		}
		//return constraints.assemble("negative scenario termination");
	}
	
	public void createVars() {
		addPositiveVars();
		addNegativeVars();
	}
	
	public FormulaList positiveConstraints() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		// first node is always an initial state (but probably there are more)
		constraints.add(xVar(0, 0));
		transitionConstraints(constraints);
		eventCompletenessConstraints(constraints);
		eachNodeHasColorConstraints(constraints);
		eachNodeHasOnlyColorConstraints(constraints);
		scenarioActionConstraints(constraints);
		return constraints;
	}
	
	public List<BooleanFormula> negativeConstraints() {
		final List<BooleanFormula> constraints = new ArrayList<>();
		negativeScenarioBasis(constraints);
		globalNegativeScenarioBasis(constraints);
		for (boolean isGlobal : Arrays.asList(false, true)) {
			negativeScenarioPropagation(constraints, isGlobal);
			negativeScenarioTermination(constraints, isGlobal);
		}
		return constraints;
	}
}

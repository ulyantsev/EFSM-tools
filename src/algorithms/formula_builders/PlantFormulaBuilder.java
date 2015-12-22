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
import structures.plant.NegativePlantScenarioForest.Loop;
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
	
	public static BooleanVariable loopVar(boolean isGlobal, MooreNode node, int sourceColor, int loopNodeColor, Loop loop) {
		return BooleanVariable.byName("loop", isGlobal, loop.index, node.number(), sourceColor, loopNodeColor).get();
	}
	
	protected void addColorVars() {
		for (MooreNode node : positiveForest.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(new BooleanVariable("x", node.number(), color));
			}
		}
	}
	
	protected void addTransitionVars() {
		for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
			for (String e : events) {
				for (int childColor = 0; childColor < colorSize; childColor++) {
					vars.add(new BooleanVariable("y", nodeColor, childColor, e));
				}
			}
			for (String action : actions) {
				vars.add(new BooleanVariable("z", nodeColor, action));
			}
		}
	}
	
	protected void addActionVars() {
		for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
			for (String action : actions) {
				vars.add(new BooleanVariable("z", nodeColor, action));
			}
		}
	}
	
	protected void addNegativeColorVars() {
		for (MooreNode node : negativeForest.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(new BooleanVariable("xx", node.number(), color));
			}
		}
		for (MooreNode node : globalNegativeForest.nodes()) {
			for (int color = 0; color < colorSize; color++) {
				vars.add(new BooleanVariable("xxg", node.number(), color));
			}
		}
	}
	
	/*
	 * Each node has at least one color
	 */
	private BooleanFormula eachNodeHasColorConstraints() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (MooreNode node : positiveForest.nodes()) {
			final FormulaList terms = new FormulaList(BinaryOperations.OR);
			for (int color = 0; color < colorSize; color++) {
				terms.add(xVar(node.number(), color));
			}
			constraints.add(terms.assemble());
		}
		return constraints.assemble("scenario constraints: each tree node has a color");
	}

	private BooleanFormula eachNodeHasOnlyColorConstraints() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (MooreNode node : positiveForest.nodes()) {
			for (int color1 = 0; color1 < colorSize; color1++) {
				for (int color2 = 0; color2 < color1; color2++) {
					final BooleanVariable v1 = xVar(node.number(), color1);
					final BooleanVariable v2 = xVar(node.number(), color2);					
					constraints.add(v1.not().or(v2.not()));
				}
			}
		}
		return constraints.assemble("scenario constraints: each tree node has at most one color");
	}
	
	private BooleanFormula transitionConstraints() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (MooreNode node : positiveForest.nodes()) {
			for (MooreTransition t : node.transitions()) {
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					for (int childColor = 0; childColor < colorSize; childColor++) {
						final BooleanVariable nodeVar = xVar(node.number(), nodeColor);
						final BooleanVariable childVar = xVar(t.dst().number(), childColor);
						final BooleanVariable relationVar = yVar(nodeColor, childColor, t.event());
						constraints.add(BinaryOperation.or(relationVar, nodeVar.not(), childVar.not()));
					}
				}
			}
		}
		return constraints.assemble("scenario constraints: connection between x's and y's");
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
	
	private BooleanFormula scenarioActionConstraints() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (MooreNode node : positiveForest.nodes()) {
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(xVar(node.number(), nodeColor).implies(actionEquality(nodeColor, node.actions())));
			}
		}
		return constraints.assemble("scenario action constraints");
	}
	
	private BooleanFormula eventCompletenessConstraints() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int i1 = 0; i1 < colorSize; i1++) {
			for (String e : events) {
				FormulaList options = new FormulaList(BinaryOperations.OR);
				for (int i2 = 0; i2 < colorSize; i2++) {
					options.add(yVar(i1, i2, e));
				}
				constraints.add(options.assemble());
			}
		}
		return constraints.assemble("induce a complete automaton");
	}
	
	private BooleanFormula negativeScenarioBasis() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
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
		return constraints.assemble("negative scenario basis");
	}
	
	private BooleanFormula globalNegativeScenarioBasis() {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		if (globalNegativeForest.roots().size() > 1) {
			throw new AssertionError();
		}
		for (MooreNode root : globalNegativeForest.roots()) {
			// the global negative root is colored in all colors
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(xxVar(root.number(), nodeColor, true));
			}
		}
		return constraints.assemble("global negative scenario basis");
	}
	
	private BooleanFormula negativeScenarioPropagation(boolean isGlobal) {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (MooreNode node : (isGlobal ? globalNegativeForest : negativeForest).nodes()) {
			for (MooreTransition edge : node.transitions()) {
				final MooreNode childNode = edge.dst();
				final String event = edge.event();
				for (int color1 = 0; color1 < colorSize; color1++) {
					for (int color2 = 0; color2 < colorSize; color2++) {
						constraints.add(BinaryOperation.and(
								xxVar(node.number(), color1, isGlobal),
								yVar(color1, color2, event),
								actionEquality(color2, childNode.actions())).implies(
								xxVar(childNode.number(), color2, isGlobal)));
					}
				}
			}
		}
		return constraints.assemble("negative scenario propagation");
	}
	
	private BooleanFormula negativeScenarioTermination(boolean isGlobal) {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (MooreNode node : (isGlobal ? globalNegativeForest : negativeForest).terminalNodes()) {
			for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
				constraints.add(xxVar(node.number(), nodeColor, isGlobal).not());
			}
		}
		return constraints.assemble("negative scenario termination");
	}
	
	private BooleanFormula negativeScenarioLoopPrevention(boolean isGlobal) {
		final FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (Loop loop : (isGlobal ? globalNegativeForest : negativeForest).loops()) {
			final List<MooreNode> loopNodes = new ArrayList<>();
			loopNodes.add(loop.source);
			loopNodes.addAll(loop.nodes);
			for (int sourceColor = 0; sourceColor < colorSize; sourceColor++) {
				// variable creation
				for (int loopNodeColor = 0; loopNodeColor < colorSize; loopNodeColor++) {
					for (MooreNode node : loopNodes) {
						if (!BooleanVariable.byName("loop", isGlobal, node.number(),
								sourceColor, loopNodeColor, loop).isPresent()) {
							final BooleanVariable loopVar = new BooleanVariable("loop",
									isGlobal, loop.index, node.number(), sourceColor, loopNodeColor);
							vars.add(loopVar);
						}
					}
				}
				
				// loop start
				constraints.add(xxVar(loop.source.number(), sourceColor, isGlobal)
						.implies(loopVar(isGlobal, loop.source, sourceColor, sourceColor, loop)));
				
				// loop propagation
				for (int loopNodeColor = 0; loopNodeColor < colorSize; loopNodeColor++) {
					for (int loopNextNodeColor = 0; loopNextNodeColor < colorSize; loopNextNodeColor++) {
						for (int i = 0; i < loopNodes.size() - 1; i++) {
							constraints.add(BinaryOperation.and(
									loopVar(isGlobal, loopNodes.get(i), sourceColor, loopNodeColor, loop),
									yVar(loopNodeColor, loopNextNodeColor, loop.events.get(i)),
									actionEquality(loopNextNodeColor, loop.nodes.get(i).actions())
									).implies(
									loopVar(isGlobal, loopNodes.get(i + 1), sourceColor, loopNextNodeColor, loop)
							));
						}
					}
				}
				
				// loop end prohibition
				constraints.add(loopVar(isGlobal, loopNodes.get(loopNodes.size() - 1),
						sourceColor, sourceColor, loop).not());
			}
		}
		//System.out.println(constraints);
		return constraints.assemble("negative scenario loop prevention");
	}
	
	public FormulaList scenarioConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		// first node is always an initial state (probably there are more)
		addColorVars();
		addTransitionVars();
		addActionVars();
		constraints.add(xVar(0, 0));
		constraints.add(transitionConstraints());
		constraints.add(eventCompletenessConstraints());
		constraints.add(eachNodeHasColorConstraints());
		constraints.add(eachNodeHasOnlyColorConstraints());
		constraints.add(scenarioActionConstraints());
		
		addNegativeColorVars();
		constraints.add(negativeScenarioBasis());
		constraints.add(globalNegativeScenarioBasis());
		for (boolean isGlobal : Arrays.asList(false, true)) {
			constraints.add(negativeScenarioPropagation(isGlobal));
			constraints.add(negativeScenarioTermination(isGlobal));
			//constraints.add(negativeScenarioLoopPrevention(isGlobal));
		}
		
		return constraints;
	}
}

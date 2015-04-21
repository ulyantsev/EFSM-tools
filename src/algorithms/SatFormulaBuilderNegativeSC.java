package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanVariable;
import qbf.reduction.FalseFormula;
import qbf.reduction.FormulaList;
import structures.NegativeNode;
import structures.NegativeScenariosTree;
import structures.Node;
import structures.ScenariosTree;
import structures.Transition;
import algorithms.AutomatonCompleter.CompletenessType;

public class SatFormulaBuilderNegativeSC extends FormulaBuilder {
	private final NegativeScenariosTree negativeTree;
	
	// the ones present in the scenario tree
    private final Map<NegativeNode, Node> verifiedNodes = new LinkedHashMap<>();
	
	public SatFormulaBuilderNegativeSC(ScenariosTree tree, int colorSize,
			List<String> events, List<String> actions,
			CompletenessType completenessType, NegativeScenariosTree negativeTree) {
		super(colorSize, tree, completenessType, events, actions);
		this.negativeTree = negativeTree;
		findVerifiedNodes(tree.getRoot(), negativeTree.getRoot());
	}
	
	private void findVerifiedNodes(Node positiveNode, NegativeNode negativeNode) {
		// premise: this negativeNode is present in the scenario tree
		if (negativeNode.terminal()) {
			//System.out.println("*************");
			// this means that the scenario tree is inconsistent with LTL properties
			// the solver will return UNSAT
			return;
		}
		verifiedNodes.put(negativeNode, positiveNode);
		for (Transition t : positiveNode.getTransitions()) {
			Node negativeChild = negativeNode.getDst(t.getEvent(), t.getExpr(), t.getActions());
			if (negativeChild != null) {
				findVerifiedNodes(t.getDst(), (NegativeNode) negativeChild);
			}
		}
	}
	
	public static BooleanVariable xxVar(int state, int color) {
		return BooleanVariable.byName("xx", state, color).get();
	}

	private void addNegativeScenarioVars() {
		for (Node node : negativeTree.getNodes()) {
			for (int color = 0; color <= colorSize; color++) {
				existVars.add(new BooleanVariable("xx", node.getNumber(), color));
			}
		}
	}
	
	private BooleanFormula eachNegativeNodeHasOneColorConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (NegativeNode node : negativeTree.getNodes()) {
			final int num = node.getNumber();
			if (node == negativeTree.getRoot()) {
				// the root has color 0
				constraints.add(xxVar(0, 0));
				for (int i = 1; i <= colorSize; i++) {
					constraints.add(xxVar(0, i).not());
				}
			} else if (verifiedNodes.containsKey(node)) {
				// verified nodes are colored according to the coloring of the positive tree
				final Node positiveNode = verifiedNodes.get(node);
				constraints.add(invalid(node).not());
				for (int i = 0; i < colorSize; i++) {
					constraints.add(xxVar(num, i).equivalent(xVar(positiveNode.getNumber(), i)));
				}
			} else if (node.terminal()) {
				// each terminal node is invalid
				constraints.add(invalid(node));
				for (int i = 0; i < colorSize; i++) {
					constraints.add(xxVar(num, i).not());
				}
			} else {
				// 'unknown' nodes
				
				// at least one color
				FormulaList terms = new FormulaList(BinaryOperations.OR);
				for (int color = 0; color <= colorSize; color++) {
					terms.add(xxVar(num, color));
				}
				constraints.add(terms.assemble());
				
				// at most one color
				for (int color1 = 0; color1 <= colorSize; color1++) {
					for (int color2 = 0; color2 < color1; color2++) {
						BooleanVariable v1 = xxVar(num, color1);
						BooleanVariable v2 = xxVar(num, color2);					
						constraints.add(v1.not().or(v2.not()));
					}
				}
			}
		}
		return constraints.assemble();
	}

	private BooleanFormula properTransitionYConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (NegativeNode node : negativeTree.getNodes()) {
			for (Transition t : node.getTransitions()) {
				NegativeNode child = (NegativeNode) t.getDst();
				if (verifiedNodes.containsKey(child)) {
					continue;
					// the positive tree implies these constraints
				}
				for (int nodeColor = 0; nodeColor < colorSize; nodeColor++) {
					BooleanVariable nodeVar = xxVar(node.getNumber(), nodeColor);
					for (int childColor = 0; childColor < colorSize; childColor++) {
						BooleanVariable childVar = xxVar(child.getNumber(), childColor);
						BooleanVariable relationVar = yVar(nodeColor, childColor, t.getEvent());
						constraints.add(BinaryOperation.or(relationVar, nodeVar.not(), childVar.not()));
						constraints.add(BinaryOperation.or(relationVar.not(), nodeVar.not(), childVar, invalid(child)));
					}
				}
			}
		}
		return constraints.assemble();
	}
	
	private BooleanFormula properTransitionZConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		
		for (NegativeNode node : negativeTree.getNodes()) {
			FormulaList options = new FormulaList(BinaryOperations.OR);
			for (int i = 0; i < colorSize; i++) {
				FormulaList zConstraints = new FormulaList(BinaryOperations.AND);
				zConstraints.add(xxVar(node.getNumber(), i));
				for (Transition t : node.getTransitions()) {
					NegativeNode child = (NegativeNode) t.getDst();
					if (verifiedNodes.containsKey(child)) {
						continue;
						// the positive tree implies these constraints
					}
					
					FormulaList innerConstraints = new FormulaList(BinaryOperations.AND);
					List<String> actionSequence = Arrays.asList(t.getActions().getActions());
					for (String action : actions) {
						BooleanFormula f = zVar(i, action, t.getEvent());
						if (!actionSequence.contains(action)) {
							f = f.not();
						}
						innerConstraints.add(f);
					}
					zConstraints.add(invalid(t.getDst()).or(innerConstraints.assemble()));
				}
				options.add(zConstraints.assemble());
			}
			constraints.add(invalid(node).or(options.assemble()));
		}
		
		return constraints.assemble();
	}
	
	private BooleanFormula invalidDefinitionConstraints() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		
		for (NegativeNode parent : negativeTree.getNodes()) {
			for (Transition t : parent.getTransitions()) {
				Node child = t.getDst();
				if (verifiedNodes.containsKey(child)) {
					continue;
					// it is already stated in the exactly-one-color constraints
					// that this node is valid
				}
				
				String event = t.getEvent();
				FormulaList options = new FormulaList(BinaryOperations.OR);
				options.add(invalid(parent));
				for (int colorParent = 0; colorParent < colorSize; colorParent++) {
					// either no transition
					FormulaList innerYConstraints = new FormulaList(BinaryOperations.AND);
					for (int colorChild = 0; colorChild < colorSize; colorChild++) {
						innerYConstraints.add(yVar(colorParent, colorChild, event).not());
					}
					BooleanFormula yFormula = completenessType == CompletenessType.NORMAL ?
							FalseFormula.INSTANCE : innerYConstraints.assemble();
					
					// or actions on the transition are invalid
					FormulaList innerZConstraints = new FormulaList(BinaryOperations.OR);
					for (String action : actions) {
						BooleanFormula v = zVar(colorParent, action, event);
						if (ArrayUtils.contains(t.getActions().getActions(), action)) {
							v = v.not();
						}
						innerZConstraints.add(v);
					}
					
					options.add(xxVar(parent.getNumber(), colorParent)
							.and(innerZConstraints.assemble().or(yFormula)));
				}
				constraints.add(invalid(child).equivalent(options.assemble()));
			}
		}
		
		return constraints.assemble();
	}
	
	private BooleanVariable invalid(Node n) {
		return xxVar(n.getNumber(), colorSize);
	}
	
	public BooleanFormula getFormula() {
		// actions should be included into the model!
		addColorVars();
		addTransitionVars(true);
		addNegativeScenarioVars();
		return scenarioConstraints(true).assemble()
				.and(eachNegativeNodeHasOneColorConstraints())
				.and(properTransitionYConstraints())
				.and(properTransitionZConstraints())
				.and(invalidDefinitionConstraints())
				.and(varPresenceConstraints());
	}
}

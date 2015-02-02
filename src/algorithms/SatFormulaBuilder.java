package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;

import qbf.reduction.BooleanFormula;
import structures.ScenariosTree;

public class SatFormulaBuilder extends FormulaBuilder {
	public SatFormulaBuilder(ScenariosTree tree, int colorSize, boolean eventCompleteness,
			boolean bfsConstraints, List<EventExpressionPair> efPairs, List<String> actions) {
		super(colorSize, tree, eventCompleteness, bfsConstraints, efPairs, actions);
	}

	public BooleanFormula getFormula() {
		addColorVars();
		addTransitionVars(false);
		return scenarioConstraints(false).assemble();
	}
}

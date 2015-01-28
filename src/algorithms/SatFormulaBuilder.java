package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import qbf.reduction.BooleanFormula;
import structures.ScenariosTree;

public class SatFormulaBuilder extends FormulaBuilder {
	public SatFormulaBuilder(ScenariosTree tree, int colorSize, boolean eventCompleteness, boolean bfsConstraints) {
		super(colorSize, tree, eventCompleteness, bfsConstraints);
	}

	public BooleanFormula getFormula() {
		addColorVars();
		addTransitionVars();
		return scenarioConstraints().assemble();
	}
}

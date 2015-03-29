package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;

import qbf.reduction.BooleanFormula;
import structures.ScenariosTree;

public class SatFormulaBuilder extends FormulaBuilder {
	public SatFormulaBuilder(ScenariosTree tree, int colorSize,
			List<String> events, List<String> actions, boolean noDeadEnds) {
		super(colorSize, tree, false, noDeadEnds, events, actions);
	}

	public BooleanFormula getFormula() {
		addColorVars();
		addTransitionVars(false);
		return scenarioConstraints(false).assemble();
	}
}

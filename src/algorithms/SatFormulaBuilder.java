package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;

import algorithms.AutomatonCompleter.CompletenessType;
import qbf.reduction.BooleanFormula;
import structures.ScenariosTree;

public class SatFormulaBuilder extends FormulaBuilder {
	private final boolean includeActionVars;
	
	public SatFormulaBuilder(ScenariosTree tree, int colorSize,
			List<String> events, List<String> actions,
			CompletenessType completenessType, boolean includeActionVars) {
		super(colorSize, tree, completenessType, events, actions);
		this.includeActionVars = includeActionVars;
	}

	public BooleanFormula getFormula() {
		addColorVars();
		addTransitionVars(includeActionVars);
		return scenarioConstraints(includeActionVars).assemble().and(varPresenceConstraints());
	}
}

package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.List;
import java.util.logging.Logger;

import qbf.egorov.ltl.grammar.LtlNode;
import qbf.reduction.BooleanFormula;
import qbf.reduction.QuantifiedBooleanFormula;
import structures.NegativeScenariosTree;
import structures.ScenariosTree;
import algorithms.AutomatonCompleter.CompletenessType;

public class HybridFormulaBuilder extends QbfFormulaBuilder {
	private final SatFormulaBuilderNegativeSC negativeBuilder;
	
	public HybridFormulaBuilder(ScenariosTree tree, int colorSize,
			List<String> events, List<String> actions,
			CompletenessType completenessType, Logger logger, List<LtlNode> formulae, int k,
			NegativeScenariosTree negativeTree, List<BooleanFormula> prohibitedFsms) {
		super(logger, tree, formulae,
				colorSize, k, completenessType, events, actions);
		negativeBuilder = new SatFormulaBuilderNegativeSC(tree, colorSize, events, actions,
				completenessType, negativeTree, prohibitedFsms);
	}

	public QuantifiedBooleanFormula getFormula() {
		addVars();
		existVars.addAll(negativeBuilder.nagativeVars());
		
		BooleanFormula fullExistConstraint = scenarioConstraints(true).assemble()
			.and(negativeBuilder.negativeConstraints())
			.and(varPresenceConstraints());
		BooleanFormula mainQbfConstraint = mainQbfConstraint(true);
		
		return new QuantifiedBooleanFormula(existVars, forallVars,
				fullExistConstraint, mainQbfConstraint);
	}
}

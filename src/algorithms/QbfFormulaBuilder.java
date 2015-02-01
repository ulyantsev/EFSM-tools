package algorithms;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.apache.commons.lang3.tuple.Pair;

import qbf.egorov.ltl.grammar.BinaryOperator;
import qbf.egorov.ltl.grammar.BooleanNode;
import qbf.egorov.ltl.grammar.LtlNode;
import qbf.egorov.ltl.grammar.Predicate;
import qbf.egorov.ltl.grammar.UnaryOperator;
import qbf.reduction.BinaryOperation;
import qbf.reduction.BinaryOperations;
import qbf.reduction.BooleanFormula;
import qbf.reduction.BooleanVariable;
import qbf.reduction.FalseFormula;
import qbf.reduction.FormulaList;
import qbf.reduction.LtlNormalizer;
import qbf.reduction.QuantifiedBooleanFormula;
import qbf.reduction.TrueFormula;
import structures.ScenariosTree;
import bool.MyBooleanExpression;

public class QbfFormulaBuilder extends FormulaBuilder{
	private final Logger logger;
	private final int k; // depth
	private final List<LtlNode> formulae;
	private final List<BooleanVariable> forallVars = new ArrayList<>();
	private final Map<SubtermIdentifier, Pair<BooleanFormula, BooleanVariable>> subterms = new LinkedHashMap<>();	
	private final boolean extractSubterms;

	public QbfFormulaBuilder(Logger logger, ScenariosTree tree, List<LtlNode> formulae, int colorSize, int depth, boolean extractSubterms, boolean eventCompleteness, boolean bfsConstraints) {
		super(colorSize, tree, eventCompleteness, bfsConstraints);
		this.logger = logger;
		this.formulae = formulae;
		this.k = depth;
		this.extractSubterms = extractSubterms;
	}

	private BooleanVariable sigmaVar(int state, int pathIndex) {
		return BooleanVariable.byName("sigma", state, pathIndex).get();
	}

	private BooleanVariable epsVar(String event, MyBooleanExpression f, int pathIndex) {
		return BooleanVariable.byName("eps", event, f, pathIndex).get();
	}
	
	private BooleanVariable zetaVar(String action, int pathIndex) {
		return BooleanVariable.byName("zeta", action, pathIndex).get();
	}
	
	private void addSigmaVars() {
		for (int i = 0; i < colorSize; i++) {
			for (int j = 0; j <= k; j++) {
				forallVars.add(new BooleanVariable("sigma", i, j));
			}
		}
	}
	
	private void addEpsVars() {
		for (EventExpressionPair p : efPairs) {
			for (int j = 0; j <= k; j++) {
				forallVars.add(new BooleanVariable("eps", p.event, p.expression, j));
			}
		}
	}
	
	private void addZetaVars() {
		for (String action : actions) {
			for (int j = 0; j <= k; j++) {
				forallVars.add(new BooleanVariable("zeta", action, j));
			}
		}
	}
	
	private LtlNode formulaToCheck() {
		if (formulae.isEmpty()) {
			return BooleanNode.FALSE;
		}
		
		LtlNode f = formulae.stream().skip(1).reduce(formulae.get(0), (f1, f2) -> LtlNormalizer.and(f1, f2));
		f = LtlNormalizer.toNegationNormalForm(LtlNormalizer.not(f));
		
		return f;
	}
	
	public QuantifiedBooleanFormula getFormula(boolean forFurtherSatReduction) {
		addColorVars(); // exist
		addTransitionVars(); // exist
		
		addSigmaVars(); // forall
		addEpsVars(); // forall
		addZetaVars(); // forall
		
		LtlNode formulaToCheck = formulaToCheck();
		logger.info(formulaToCheck.toString());
		BooleanFormula scenarioConstraints = scenarioConstraints().assemble();
		
		BooleanFormula pathIsCorrect = forFurtherSatReduction
				? cTerm().and(dTerm())
				: BinaryOperation.and(sigmaVar(0, 0), aTerm(), bTerm(), cTerm(), dTerm());
		
		FormulaList cyclicPathFormula = new FormulaList(BinaryOperations.OR);
		for (int l = 0; l <= k; l++) {
			cyclicPathFormula.add(llkTerm(l).and(translateCyclic(formulaToCheck, l, 0)));
		}

		BooleanFormula pathFormula = lkTerm().not()
			.and(translateNonCyclic(formulaToCheck, 0))
			.or(cyclicPathFormula.assemble());
		
		// auxiliary "forall" variables
		FormulaList subtermEquations = new FormulaList(BinaryOperations.AND);
		
		subterms.entrySet().forEach(entry -> {
			BooleanFormula expansion = entry.getValue().getLeft();
			BooleanVariable name = entry.getValue().getRight();
			forallVars.add(name);
			subtermEquations.add(name.equivalent(expansion));
		});
		
		BooleanFormula mainQbfConstraint = BinaryOperation.or(Arrays.asList(
				subtermEquations.assemble().not(),
				pathIsCorrect.not(),
				pathFormula.not()
		),"main QBF constraint");

		return new QuantifiedBooleanFormula(existVars, forallVars, scenarioConstraints, mainQbfConstraint);
	}

	// not more than one state/event in the same place of the path
	private BooleanFormula aTerm() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		
		for (int j = 0; j <= k; j++) {
			for (int i1 = 0; i1 < colorSize; i1++) {
				for (int i2 = i1 + 1; i2 < colorSize; i2++) {
					constraints.add(sigmaVar(i1, j).and(sigmaVar(i2, j)).not());
				}
			}
			for (int i1 = 0; i1 < efPairs.size(); i1++) {
				String e1 = efPairs.get(i1).event;
				MyBooleanExpression f1 = efPairs.get(i1).expression;
				for (int i2 = i1 + 1; i2 < efPairs.size(); i2++) {
					String e2 = efPairs.get(i2).event;
					MyBooleanExpression f2 = efPairs.get(i2).expression;
					constraints.add(epsVar(e1, f1, j).and(epsVar(e2, f2, j)).not());
				}
			}
		}
		return constraints.assemble("(A) not more than one state/event in the same position of the path");
	}
	
	// at least one state/event in each position of the path
	private BooleanFormula bTerm() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int j = 0; j <= k; j++) {
			FormulaList optionsS = new FormulaList(BinaryOperations.OR);
			for (int i = 0; i < colorSize; i++) {
				optionsS.add(sigmaVar(i, j));
			}
			constraints.add(optionsS.assemble());
			
			FormulaList optionsE = new FormulaList(BinaryOperations.OR);
			for (EventExpressionPair p : efPairs) {
				optionsE.add(epsVar(p.event, p.expression, j));
			}
			constraints.add(optionsE.assemble());
		}
		return constraints.assemble("(B) at least one state/event in each position of the path");
	}
	
	// forall-variables are consistent with y's
	private BooleanFormula cTerm() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int j = 0; j < k; j++) {
			for (int i1 = 0; i1 < colorSize; i1++) {
				for (int i2 = 0; i2 < colorSize; i2++) {
					for (EventExpressionPair p : efPairs) {
						constraints.add(BinaryOperation.and(sigmaVar(i1, j), epsVar(p.event, p.expression, j),
								sigmaVar(i2, j + 1)).implies(yVar(i1, i2, p.event, p.expression)));
					}
				}
			}
		}
		if (!eventCompleteness) {
			// additional term for j = k
			for (int i1 = 0; i1 < colorSize; i1++) {
				for (EventExpressionPair p : efPairs) {
					FormulaList options = new FormulaList(BinaryOperations.OR);
					// some state exists to transit to
					for (int i2 = 0; i2 < colorSize; i2++) {
						options.add(yVar(i1, i2, p.event, p.expression));
					}
					constraints.add(sigmaVar(i1, k).and(epsVar(p.event, p.expression, k)).implies(options.assemble()));
				}
			}
		}
		
		return constraints.assemble("(C) path is consistent with y's");
	}
	
	// forall-variables are consistent with z's
	private BooleanFormula dTerm() {
		FormulaList constraints = new FormulaList(BinaryOperations.AND);
		for (int j = 0; j <= k; j++) {
			for (int i1 = 0; i1 < colorSize; i1++) {
				for (String action : actions) {
					for (EventExpressionPair p : efPairs) {
						constraints.add(sigmaVar(i1, j).and(epsVar(p.event, p.expression, j))
							.implies(zVar(i1, action, p.event, p.expression).equivalent(zetaVar(action, j))));
					}
				}
			}
		}
		return constraints.assemble("(D) path is consistent with z's");
	}
	
	// path is a (k, l)-loop
	private BooleanFormula llkTerm(int l) {
		assert l >= 0 && l <= k;
		
		SubtermIdentifier si = new SubtermIdentifier(null, l, -1);
		Pair<BooleanFormula, BooleanVariable> subterm = subterms.get(si);
		if (subterm == null) {
			BooleanVariable result = new BooleanVariable("llk", l);
			FormulaList options = new FormulaList(BinaryOperations.OR);
			for (int i1 = 0; i1 < colorSize; i1++) {
				for (int i2 = 0; i2 < colorSize; i2++) {
					for (EventExpressionPair p : efPairs) {
						options.add(BinaryOperation.and(sigmaVar(i1, k),
							epsVar(p.event, p.expression, k), sigmaVar(i2, l), yVar(i1, i2, p.event, p.expression)));
					}
				}
			}
			BooleanFormula expansion = options.assemble("LLK_" + l);
			storeSubterm(si, Pair.of(expansion, result));
			return extractSubterms ? result : expansion;
		} else {
			return subterm.getRight();
		}
	}
	
	// path is a loop
	private BooleanFormula lkTerm() {
		FormulaList options = new FormulaList(BinaryOperations.OR);
		for (int l = 0; l <= k; l++) {
			options.add(llkTerm(l));
		}
		return options.assemble("LK");
	}

	private BooleanFormula translatePredicate(Predicate p, int index) {
		String arg = p.args().get(0).toString();
		switch (p.getName()) {
		case "wasEvent":
			FormulaList options = new FormulaList(BinaryOperations.OR);
			for (EventExpressionPair pair : efPairs) {
				if (pair.event.equals(arg)) {
					options.add(epsVar(arg, pair.expression, index));
				}
			}
			return options.assemble();
		case "wasAction":
			return zetaVar(arg, index);
		default:
			throw new RuntimeException("Unsupported predicate " + p.getName());
		}
	}
	
	private BooleanFormula translateConstant(LtlNode node) {
		if (node == BooleanNode.TRUE) {
			return TrueFormula.INSTANCE;
		} else if (node == BooleanNode.FALSE) {
			return FalseFormula.INSTANCE;
		} else {
			throw new AssertionError();
		}
	}
	
	private BooleanFormula translateNonCyclic(LtlNode node, int index) {
		assert index >= 0 && index <= k;
		
		if (node instanceof Predicate) {
			return translatePredicate((Predicate) node, index);
		} else if (node instanceof BooleanNode) {
			return translateConstant(node);
		} 
		
		SubtermIdentifier si = new SubtermIdentifier(node, -1, index);
		Pair<BooleanFormula, BooleanVariable> subterm = subterms.get(si);
		if (subterm == null) {
			BooleanFormula expansion;

			if (node instanceof UnaryOperator) {
				UnaryOperator op = (UnaryOperator) node;
				LtlNode a = op.getOperand();
				switch (op.getType()) {
				case GLOBAL:
					expansion = FalseFormula.INSTANCE;
					break;
				case FUTURE:
					FormulaList orList = new FormulaList(BinaryOperations.OR);
					for (int j = index; j <= k; j++) {
						orList.add(translateNonCyclic(a, j));
					}
					expansion = orList.assemble();
					break;
				case NEXT:
					expansion = index == k ? FalseFormula.INSTANCE : translateNonCyclic(a, index + 1);
					break;
				case NEG:
					assert a instanceof BooleanNode || a instanceof Predicate;
					expansion = translateNonCyclic(a, index).not();
					break;
				default:
					throw new RuntimeException("Unknown unary operator " + op);
				}
			} else if (node instanceof BinaryOperator) {
				BinaryOperator op = (BinaryOperator) node;
				LtlNode a = op.getLeftOperand();
				LtlNode b = op.getRightOperand();
				
				Function<Boolean, BooleanFormula> untilRelease = isUntil -> {
					FormulaList orList = new FormulaList(BinaryOperations.OR);
					for (int j = index; j <= k; j++) {
						FormulaList andList = new FormulaList(BinaryOperations.AND);
						andList.add(translateNonCyclic(isUntil ? b : a, j));
						for (int n = index; n <= (isUntil ? j - 1 : j); n++) {
							andList.add(translateNonCyclic(isUntil ? a : b, n));
						}
						orList.add(andList.assemble());
					}
					return orList.assemble();
				};
				
				switch (op.getType()) {
				case OR:
					expansion = translateNonCyclic(a, index).or(translateNonCyclic(b, index));
					break;
				case AND:
					expansion = translateNonCyclic(a, index).and(translateNonCyclic(b, index));
					break;
				case UNTIL:
					expansion = untilRelease.apply(true);
					break;
				case RELEASE:
					expansion = untilRelease.apply(false);
					break;
				default:
					throw new RuntimeException("Unknown binary operator " + op);
				}
			} else {
				throw new AssertionError();
			}
			
			BooleanVariable result = BooleanVariable.newAuxiliaryVariable();
			storeSubterm(si, Pair.of(expansion, result));
			return extractSubterms ? result : expansion;
		} else {
			return subterm.getRight();
		}
	}
	
	private void storeSubterm(SubtermIdentifier si, Pair<BooleanFormula, BooleanVariable> p) {
		if (extractSubterms) {
			subterms.put(si, p);
		}
	}

	private BooleanFormula translateCyclic(LtlNode node, int l, int index) {
		assert l >= 0 && l <= k;
		assert index >= 0 && index <= k;
		
		if (node instanceof Predicate) {
			return translatePredicate((Predicate) node, index);
		} else if (node instanceof BooleanNode) {
			return translateConstant(node);
		} 
		
		SubtermIdentifier si = new SubtermIdentifier(node, l, index);
		Pair<BooleanFormula, BooleanVariable> subterm = subterms.get(si);
		if (subterm == null) {
			BooleanFormula expansion;
			if (node instanceof UnaryOperator) {
				UnaryOperator op = (UnaryOperator) node;
				LtlNode a = op.getOperand();
				
				Function<BinaryOperations, BooleanFormula> globalFuture = operation -> {
					FormulaList list = new FormulaList(operation);
					for (int j = Math.min(index, l); j <= k; j++) {
						list.add(translateCyclic(a, l, j));
					}
					return list.assemble();
				};
				
				switch (op.getType()) {
				case GLOBAL:
					expansion = globalFuture.apply(BinaryOperations.AND);
					break;
				case FUTURE:
					expansion = globalFuture.apply(BinaryOperations.OR);
					break;
				case NEXT:
					expansion = translateCyclic(a, l, index < k ? index + 1 : l);
					break;
				case NEG:
					assert a instanceof BooleanNode || a instanceof Predicate;
					expansion = translateCyclic(a, l, index).not();
					break;
				default:
					throw new RuntimeException("Unknown unary operator " + op);
				}
			} else if (node instanceof BinaryOperator) {
				BinaryOperator op = (BinaryOperator) node;
				LtlNode a = op.getLeftOperand();
				LtlNode b = op.getRightOperand();
				
				BiFunction<BinaryOperations, BinaryOperations, BooleanFormula> untilRelease = (op1, op2) -> {
					FormulaList l1 = new FormulaList(op1);
					for (int j = index; j <= k; j++) {
						FormulaList l2 = new FormulaList(op2);
						l2.add(translateCyclic(b, l, j));
						for (int n = index; n < j; n++) {
							l2.add(translateCyclic(a, l, n));
						}
						l1.add(l2.assemble());
					}
					for (int j = l; j < index; j++) {
						FormulaList l2 = new FormulaList(op2);
						l2.add(translateCyclic(b, l, j));
						for (int n = 0; n <= k; n++) {
							if (n >= index || n >= l && n < j) {
								l2.add(translateCyclic(a, l, n));
							}
						}
						l1.add(l2.assemble());
					}
					return l1.assemble();
				};
				
				switch (op.getType()) {
				case OR:
					expansion = translateCyclic(a, l, index).or(translateCyclic(b, l, index));
					break;
				case AND:
					expansion = translateCyclic(a, l, index).and(translateCyclic(b, l, index));
					break;
				case UNTIL:
					expansion = untilRelease.apply(BinaryOperations.OR, BinaryOperations.AND);
					break;
				case RELEASE:
					expansion = untilRelease.apply(BinaryOperations.AND, BinaryOperations.OR);
					break;
				default:
					throw new RuntimeException("Unknown binary operator " + op);
				}
			} else {
				throw new AssertionError();
			}
			
			BooleanVariable result = BooleanVariable.newAuxiliaryVariable();
			storeSubterm(si, Pair.of(expansion, result));
			return extractSubterms ? result : expansion;
		} else {
			return subterm.getRight();
		}
	}
	
	private static class SubtermIdentifier {
		final LtlNode node;
		final int l;
		final int index;
		
		public SubtermIdentifier(LtlNode node, int l, int index) {
			this.node = node;
			this.l = l;
			this.index = index;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + index;
			result = prime * result + l;
			result = prime * result + ((node == null) ? 0 : node.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				throw new AssertionError();
			if (getClass() != obj.getClass())
				throw new AssertionError();
			SubtermIdentifier other = (SubtermIdentifier) obj;
			return node == other.node && index == other.index && l == other.l;
		}
		
		@Override
		public String toString() {
			return node + " " + l + " " + index;
		}
	}
}

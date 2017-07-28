package formula_builders;

/**
 * (c) Igor Buzhinsky
 */

import algorithms.AutomatonCompleter.CompletenessType;
import bnf_formulae.*;
import structures.mealy.ScenarioTree;
import verification.ltl.LtlNormalizer;
import verification.ltl.grammar.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

public class QbfFormulaBuilder extends FormulaBuilder {
    private final Logger logger;
    private final int k; // depth
    private final List<LtlNode> formulae;
    protected final List<BooleanVariable> forallVars = new ArrayList<>();

    public QbfFormulaBuilder(Logger logger, ScenarioTree tree, List<LtlNode> formulae,
            int colorSize, int depth, CompletenessType completenessType,
            List<String> events, List<String> actions) {
        super(colorSize, tree, completenessType, events, actions);
        BooleanVariable.eraseVariables();
        this.logger = logger;
        this.formulae = formulae;
        this.k = depth;
    }

    private BooleanVariable sigmaVar(int state, int pathIndex) {
        return BooleanVariable.byName("sigma", state, pathIndex).get();
    }

    private BooleanVariable epsVar(String event, int pathIndex) {
        return BooleanVariable.byName("eps", event, pathIndex).get();
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
        for (String e : events) {
            for (int j = 0; j <= k; j++) {
                forallVars.add(new BooleanVariable("eps", e, j));
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
        return formulae.isEmpty() ? BooleanNode.FALSE : LtlNormalizer.toNegationNormalForm(
                LtlNormalizer.not(formulae.stream().skip(1).reduce(formulae.get(0), LtlNormalizer::and)));
    }
    
    private BooleanFormula mainQbfConstraint(boolean forFurtherSatReduction) {
        final LtlNode formulaToCheck = formulaToCheck();
        logger.info(formulaToCheck.toString());
        
        final BooleanFormula pathIsCorrect = forFurtherSatReduction
                ? cTerm().and(dTerm())
                : BinaryOperation.and(sigmaVar(0, 0), aTerm(), bTerm(), cTerm(), dTerm());
        
        final FormulaList cyclicPathFormula = new FormulaList(BinaryOperations.OR);
        for (int l = 0; l <= k; l++) {
            cyclicPathFormula.add(llkTerm(l).and(translateCyclic(formulaToCheck, l, 0)));
        }
        
        final BooleanFormula pathFormula = lkTerm().not()
                .and(translateNonCyclic(formulaToCheck, 0))
                .or(cyclicPathFormula.assemble());
        
        return BinaryOperation.or(Arrays.asList(pathIsCorrect.not(), pathFormula.not()), "main QBF constraint");
    }
    
    private void addVars() {
        addColorVars(); // exist
        addTransitionVars(true); // exist
        addSigmaVars(); // forall
        addEpsVars(); // forall
        addZetaVars(); // forall
    }
    
    public QuantifiedBooleanFormula getFormula(boolean forFurtherSatReduction) {
        addVars();
        BooleanFormula scenarioConstraints = scenarioConstraints(true).assemble();
        BooleanFormula mainQbfConstraint = mainQbfConstraint(forFurtherSatReduction);
        return new QuantifiedBooleanFormula(existVars, forallVars, scenarioConstraints.and(varPresenceConstraints()),
                mainQbfConstraint);
    }

    // not more than one state/event in the same place of the path
    private BooleanFormula aTerm() {
        final FormulaList constraints = new FormulaList(BinaryOperations.AND);
        
        for (int j = 0; j <= k; j++) {
            for (int i1 = 0; i1 < colorSize; i1++) {
                for (int i2 = i1 + 1; i2 < colorSize; i2++) {
                    constraints.add(sigmaVar(i1, j).and(sigmaVar(i2, j)).not());
                }
            }
            for (int i1 = 0; i1 < events.size(); i1++) {
                String e1 = events.get(i1);
                for (int i2 = i1 + 1; i2 < events.size(); i2++) {
                    String e2 = events.get(i2);
                    constraints.add(epsVar(e1, j).and(epsVar(e2, j)).not());
                }
            }
        }
        return constraints.assemble("(A) not more than one state/event in the same position of the path");
    }
    
    // at least one state/event in each position of the path
    private BooleanFormula bTerm() {
        final FormulaList constraints = new FormulaList(BinaryOperations.AND);
        for (int j = 0; j <= k; j++) {
            FormulaList optionsS = new FormulaList(BinaryOperations.OR);
            for (int i = 0; i < colorSize; i++) {
                optionsS.add(sigmaVar(i, j));
            }
            constraints.add(optionsS.assemble());
            
            FormulaList optionsE = new FormulaList(BinaryOperations.OR);
            for (String e : events) {
                optionsE.add(epsVar(e, j));
            }
            constraints.add(optionsE.assemble());
        }
        return constraints.assemble("(B) at least one state/event in each position of the path");
    }
    
    // forall-variables are consistent with y's
    private BooleanFormula cTerm() {
        final FormulaList constraints = new FormulaList(BinaryOperations.AND);
        for (int j = 0; j < k; j++) {
            for (int i1 = 0; i1 < colorSize; i1++) {
                for (int i2 = 0; i2 < colorSize; i2++) {
                    for (String e : events) {
                        constraints.add(BinaryOperation.and(sigmaVar(i1, j),
                                epsVar(e, j),
                                sigmaVar(i2, j + 1)).implies(yVar(i1, i2, e)));
                    }
                }
            }
        }
        if (completenessType != CompletenessType.NORMAL) {
            // additional term for j = k
            for (int i1 = 0; i1 < colorSize; i1++) {
                for (String e : events) {
                    FormulaList options = new FormulaList(BinaryOperations.OR);
                    // some state exists to transit to
                    for (int i2 = 0; i2 < colorSize; i2++) {
                        options.add(yVar(i1, i2, e));
                    }
                    constraints.add(
                            sigmaVar(i1, k).and(epsVar(e, k))
                            .implies(options.assemble())
                    );
                }
            }
        }
        
        return constraints.assemble("(C) path is consistent with y's");
    }
    
    // forall-variables are consistent with z's
    private BooleanFormula dTerm() {
        final FormulaList constraints = new FormulaList(BinaryOperations.AND);
        for (int j = 0; j <= k; j++) {
            for (int i1 = 0; i1 < colorSize; i1++) {
                for (String action : actions) {
                    for (String e : events) {
                        constraints.add(
                            sigmaVar(i1, j).and(epsVar(e, j))
                            .implies(zVar(i1, action, e).equivalent(zetaVar(action, j)))
                        );
                    }
                }
            }
        }
        return constraints.assemble("(D) path is consistent with z's");
    }
    
    // path is a (k, l)-loop
    private BooleanFormula llkTerm(int l) {
        assert l >= 0 && l <= k;

        final FormulaList options = new FormulaList(BinaryOperations.OR);
        for (int i1 = 0; i1 < colorSize; i1++) {
            for (int i2 = 0; i2 < colorSize; i2++) {
                for (String e : events) {
                    options.add(BinaryOperation.and(sigmaVar(i1, k),
                        epsVar(e, k), sigmaVar(i2, l), yVar(i1, i2, e)));
                }
            }
        }
        return options.assemble("LLK_" + l);
    }
    
    // path is a loop
    private BooleanFormula lkTerm() {
        final FormulaList options = new FormulaList(BinaryOperations.OR);
        for (int l = 0; l <= k; l++) {
            options.add(llkTerm(l));
        }
        return options.assemble("LK");
    }

    private BooleanFormula translatePredicate(Predicate p, int index) {
        switch (p.getName()) {
        case "event":
            return epsVar(p.arg(), index);
        case "action": 
            return zetaVar(p.arg(), index);
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
            final BinaryOperator op = (BinaryOperator) node;
            final LtlNode a = op.getLeftOperand();
            final LtlNode b = op.getRightOperand();

            final Function<Boolean, BooleanFormula> untilRelease = isUntil -> {
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
        return expansion;
    }

    private BooleanFormula translateCyclic(LtlNode node, int l, int index) {
        assert l >= 0 && l <= k;
        assert index >= 0 && index <= k;
        
        if (node instanceof Predicate) {
            return translatePredicate((Predicate) node, index);
        } else if (node instanceof BooleanNode) {
            return translateConstant(node);
        } 

        BooleanFormula expansion;
        if (node instanceof UnaryOperator) {
            UnaryOperator op = (UnaryOperator) node;
            LtlNode a = op.getOperand();

            final Function<BinaryOperations, BooleanFormula> globalFuture = operation -> {
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
            final BinaryOperator op = (BinaryOperator) node;
            final LtlNode a = op.getLeftOperand();
            final LtlNode b = op.getRightOperand();

            final BiFunction<BinaryOperations, BinaryOperations, BooleanFormula> untilRelease = (op1, op2) -> {
                final FormulaList l1 = new FormulaList(op1);
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
        return expansion;
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package verification.ltl.buchi.translator;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rwth.i2.ltl2ba4j.LTL2BA4J;
import rwth.i2.ltl2ba4j.formula.IFormula;
import rwth.i2.ltl2ba4j.formula.IFormulaFactory;
import rwth.i2.ltl2ba4j.formula.impl.FormulaFactory;
import rwth.i2.ltl2ba4j.model.IGraphProposition;
import rwth.i2.ltl2ba4j.model.IState;
import rwth.i2.ltl2ba4j.model.ITransition;
import verification.ltl.buchi.BuchiAutomaton;
import verification.ltl.buchi.BuchiNode;
import verification.ltl.buchi.BuchiNodeFactory;
import verification.ltl.buchi.ExpressionMap;
import verification.ltl.buchi.TransitionCondition;
import verification.ltl.grammar.BinaryOperator;
import verification.ltl.grammar.BooleanNode;
import verification.ltl.grammar.IExpression;
import verification.ltl.grammar.INodeVisitor;
import verification.ltl.grammar.LtlNode;
import verification.ltl.grammar.Predicate;
import verification.ltl.grammar.UnaryOperator;

/**
 * Ltl formula translator to Buchi automata.
 * Used ltl2ba4j library to call LTL2BA. Ltl2ba is called using JNI.
 * <br>Visit <a href="http://www.sable.mcgill.ca/~ebodde/rv//ltl2ba4j/">ltl2ba4j site</a> for
 * more information abount this library.
 *
 * @author kegorov
 *         Date: Apr 3, 2009
 */
public class JLtl2baTranslator {
    private final VisitorImpl visitor = new VisitorImpl();
    private IFormulaFactory factory;
    private final BuchiConverter buchiConverter = new BuchiConverter();
    private final ExpressionMap expr = new ExpressionMap();

    public BuchiAutomaton translate(LtlNode root) {
        try {
            IFormula formula = getFormula(root);
            Collection<ITransition> transitions = LTL2BA4J.formulaToBA(formula);
            return buchiConverter.convert(transitions);
        } catch (Exception e) {
            throw new TranslationException(e);
        }
    }

    private IFormula getFormula(LtlNode root) {
        expr.clear();
        factory = new FormulaFactory();
        return root.accept(visitor, null);
    }

    private class BuchiConverter {
        BuchiAutomaton buchi;
        BuchiNodeFactory nodeFactory;

        Map<IState, BuchiNode> bNodes;
        Set<BuchiNode> acceptNodes;

        public BuchiAutomaton convert(Collection<ITransition> transitions) {
            buchi = new BuchiAutomaton();
            nodeFactory = new BuchiNodeFactory();

            bNodes = new HashMap<>();
            acceptNodes = new HashSet<>();

            for (ITransition t : transitions) {
                IState source = t.getSourceState();
                IState target = t.getTargetState();

                BuchiNode bSource = getBuchiNode(source);
                BuchiNode bTarget = getBuchiNode(target);

                bSource.addTransition(getCondition(t), bTarget);
            }
            buchi.setAcceptSet(acceptNodes);
            checkAcceptStates();

            return buchi;
        }

        /**
         * Get buchi node corresponds to IState. If node doesn't exist, create new one.
         * @param state ltl2ba4j state implementation
         * @return buchi node
         */
        private BuchiNode getBuchiNode(IState state) {
            BuchiNode node = bNodes.get(state);
            if (node == null) {
                node = nodeFactory.createBuchiNode();
                bNodes.put(state, node);
                buchi.addNode(node);
                if (state.isInitial()) {
                    buchi.setStartNode(node);
                }
                if (state.isFinal()) {
                    acceptNodes.add(node);
                }
            }
            return node;
        }
        
        private TransitionCondition getCondition(ITransition t) {
            TransitionCondition cond = new TransitionCondition();
            for (IGraphProposition prop: t.getLabels()) {
                String label = prop.getLabel();
                IExpression<Boolean> e = expr.get(label);
                if (e == null) {
                    if (label.equals("<SIGMA>")) {
                        cond.addExpression(BooleanNode.TRUE);
                    } else {
                        throw new TranslationException("ltl2baj4: Unexpected label: " + label);
                    }
                } else {
                    if (prop.isNegated()) {
                        cond.addNegExpression(e);
                    } else {
                        cond.addExpression(e);
                    }
                }
            }
            return cond;
        }

        /**
         * If accept node has not transitions, add transition to it
         */
        private void checkAcceptStates() {
            for (BuchiNode n : acceptNodes) {
                if (n.getTransitions().isEmpty()) {
                    TransitionCondition cond = new TransitionCondition();
                    cond.addExpression(BooleanNode.TRUE);
                    n.addTransition(cond, n);
                }
            }
        }
    }

    private class VisitorImpl implements INodeVisitor<IFormula, Void> {
        public IFormula visitPredicate(Predicate p, Void aVoid) {
            String prop = p.getUniqueName();
            expr.put(prop, p);

            return factory.Proposition(prop);
        }

        public IFormula visitNeg(UnaryOperator op, Void aVoid) {
            return factory.Not(op.getOperand().accept(this, null));
        }

        public IFormula visitFuture(UnaryOperator op, Void aVoid) {
            return factory.F(op.getOperand().accept(this, null));
        }

        public IFormula visitNext(UnaryOperator op, Void aVoid) {
            return factory.X(op.getOperand().accept(this, null));
        }

        public IFormula visitAnd(BinaryOperator op, Void aVoid) {
            return factory.And(op.getLeftOperand().accept(this, null),
                               op.getRightOperand().accept(this, null));
        }

        public IFormula visitOr(BinaryOperator op, Void aVoid) {
            return factory.Or(op.getLeftOperand().accept(this, null),
                              op.getRightOperand().accept(this, null));
        }

        public IFormula visitRelease(BinaryOperator op, Void aVoid) {
            return factory.Release(op.getLeftOperand().accept(this, null),
                                   op.getRightOperand().accept(this, null));
        }

        public IFormula visitUntil(BinaryOperator op, Void aVoid) {
            return factory.Until(op.getLeftOperand().accept(this, null),
                                 op.getRightOperand().accept(this, null));
        }

        public IFormula visitGlobal(UnaryOperator op, Void aVoid) {
            return factory.G(op.getOperand().accept(this, null));
        }

        public IFormula visitBoolean(BooleanNode b, Void aVoid) {
            String str = b.getValue().toString();
            expr.put(b.getValue().toString(), b);
            return factory.Proposition(str);
        }
    }
}

/* 
 * Developed by eVelopers Corporation, 2009
 */
package ru.ifmo.ltl.buchi.translator;

import ru.ifmo.ltl.buchi.*;
import ru.ifmo.ltl.buchi.impl.*;
import ru.ifmo.ltl.grammar.*;
import rwth.i2.ltl2ba4j.formula.IFormula;
import rwth.i2.ltl2ba4j.formula.IFormulaFactory;
import rwth.i2.ltl2ba4j.formula.impl.FormulaFactory;
import rwth.i2.ltl2ba4j.LTL2BA4J;
import rwth.i2.ltl2ba4j.model.ITransition;
import rwth.i2.ltl2ba4j.model.IState;
import rwth.i2.ltl2ba4j.model.IGraphProposition;

import java.util.*;

/**
 * Ltl formula translator to Buchi automata.
 * Used ltl2ba4j library to call LTL2BA. Ltl2ba is called using JNI.
 * <br>Visit <a href="http://www.sable.mcgill.ca/~ebodde/rv//ltl2ba4j/">ltl2ba4j site</a> for
 * more information abount this library.
 *
 * @author kegorov
 *         Date: Apr 3, 2009
 */
public class JLtl2baTranslator implements ITranslator {
    private VisitorImpl visitor = new VisitorImpl();
    private IFormulaFactory factory;
    private BuchiConverter  buchiConverter = new BuchiConverter();
    private ExpressionMap expr = new ExpressionMap();

    public IBuchiAutomata translate(LtlNode root) {
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
        BuchiAutomata buchi;
        IBuchiNodeFactory<BuchiNode> nodeFactory;

        Map<IState, BuchiNode> bNodes;
        Set<BuchiNode> acceptNodes;

        public IBuchiAutomata convert(Collection<ITransition> transitions) {
            buchi = new BuchiAutomata();
            nodeFactory = new BuchiNodeFactory();

            bNodes = new HashMap<IState, BuchiNode>();
            acceptNodes = new HashSet<BuchiNode>();

            for (ITransition t: transitions) {
                IState source = t.getSourceState();
                IState target = t.getTargetState();

                BuchiNode bSource = getBuchiNode(source);
                BuchiNode bTarget = getBuchiNode(target);

                bSource.addTransition(getCondition(t), bTarget);
            }
            buchi.addAcceptSet(acceptNodes);
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
        
        private ITransitionCondition getCondition(ITransition t) {
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
            for (BuchiNode n: acceptNodes) {
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

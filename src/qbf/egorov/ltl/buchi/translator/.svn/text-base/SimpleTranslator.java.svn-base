/**
 * SimpleTranslator.java, 19.03.2008
 */
package ru.ifmo.ltl.buchi.translator;

import ru.ifmo.ltl.buchi.ITranslator;
import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.ltl.buchi.IBuchiNode;
import ru.ifmo.ltl.buchi.ITransitionCondition;
import ru.ifmo.ltl.buchi.impl.BuchiNode;
import ru.ifmo.ltl.buchi.impl.TransitionCondition;
import ru.ifmo.ltl.buchi.impl.BuchiAutomata;
import ru.ifmo.ltl.grammar.*;

import java.util.*;

import org.apache.commons.lang.NotImplementedException;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class SimpleTranslator implements ITranslator {

    private Set<RecordNode> nodes = new HashSet<RecordNode>();;
    private INodeVisitor<Void, RecordNode> visitor = new ExpandVisitor();
    private Map<BinaryOperator, Set<RecordNode>> accept = new LinkedHashMap<BinaryOperator, Set<RecordNode>>();

    public IBuchiAutomata translate(LtlNode root) {
        LtlNode nnfRoot = LtlUtils.getInstance().toNnf(root);

        nodes.clear();
        RecordNode start = new RecordNode();
        RecordNode init = createInitNode();

        start.incoming.add(init);
        start.newForm.add(nnfRoot);
        expand(start);
        return createBuchi(nnfRoot, init, nodes);
    }

    protected RecordNode createInitNode() {
        RecordNode initNode = new RecordNode();
        initNode.incoming = null;
        initNode.newForm = null;
        initNode.nextForm = null;
        initNode.oldForm = null;
        return initNode;
    }

    protected void expand(RecordNode curNode) {
        if (curNode.newForm.isEmpty()) {
            RecordNode r = findEqualsForm(curNode);
            if (r != null) {
                r.incoming.addAll(curNode.incoming);
            } else {
                nodes.add(curNode);
                RecordNode newNode = new RecordNode();
                newNode.incoming.add(curNode);
                newNode.newForm.addAll(curNode.nextForm); //TODO: try newNode.newForm = curNode.nextForm;
                expand(newNode);
            }
        } else {
            Iterator<LtlNode> iter = curNode.newForm.iterator();
            LtlNode form = iter.next();
            iter.remove();
            if (curNode.oldForm.contains(form)) {
                expand(curNode);
            } else {
                form.accept(visitor, curNode);
            }
        }
    }

    protected IBuchiAutomata createBuchi(LtlNode root, RecordNode init, Set<RecordNode> nodes) {
        BuchiAutomata buchi = new BuchiAutomata();
        Map<RecordNode, BuchiNode> map = new HashMap<RecordNode, BuchiNode>();
        int idSeq = 0;

        accept.clear();

        //create Buchi nodes
        map.put(init, new BuchiNode(idSeq++));
        for (RecordNode q: nodes) {
            checkAcceptSetMembership(root, q);
            map.put(q, new BuchiNode(idSeq++));
        }

        buchi.setStartNode(map.get(init));
        buchi.addNodes(map.values());

        //set accept nodes set
        for (Map.Entry<BinaryOperator, Set<RecordNode>> entry: accept.entrySet()) {
            Set<IBuchiNode> acceptSet = new HashSet<IBuchiNode>();
            for (RecordNode q: entry.getValue()) {
                acceptSet.add(map.get(q));
            }
            buchi.addAcceptSet(acceptSet);
        }

        //set buchi nodes transitions
        for (RecordNode q: nodes) {
            IBuchiNode buchiNode = map.get(q);
            ITransitionCondition cond = extractCondition(q.oldForm);
            for (RecordNode p: q.incoming) {
                BuchiNode n = map.get(p);
                n.addTransition(cond, buchiNode);
            }
        }

        return buchi;
    }

    @SuppressWarnings("unchecked")
    protected ITransitionCondition extractCondition(Set<LtlNode> nodes) {
        TransitionCondition cond = new TransitionCondition();
        for (LtlNode node: nodes) {
            if (node instanceof IExpression) {
                cond.addExpression((IExpression<Boolean>) node);
            } else if (node instanceof UnaryOperator) {
                UnaryOperator op = (UnaryOperator) node;
                if (UnaryOperatorType.NEG == op.getType()) {
                    if (op.getOperand() instanceof IExpression) {
                        cond.addNegExpression((IExpression<Boolean>) op.getOperand());
                    } else {
                        throw new RuntimeException("BuchiNode isn't in negative normal form!");
                    }
                }
            }
        }
        return cond;
    }

    protected void checkAcceptSetMembership(LtlNode root, RecordNode node) {
        if (root instanceof UnaryOperator) {
            UnaryOperator op = (UnaryOperator) root;
            checkAcceptSetMembership(op.getOperand(), node);
        } else if (root instanceof BinaryOperator) {
            BinaryOperator op = (BinaryOperator) root;
            if (BinaryOperatorType.UNTIL == op.getType()) {
                if (node.oldForm.contains(op.getRightOperand()) || !node.oldForm.contains(op)) {
                    putToAccept(op, node);
                }
            }
            checkAcceptSetMembership(op.getLeftOperand(), node);
            checkAcceptSetMembership(op.getRightOperand(), node);
        }
    }

    private void putToAccept(BinaryOperator op, RecordNode node) {
        if (BinaryOperatorType.UNTIL != op.getType()) {
            throw new IllegalArgumentException("Operator should be UNTIL");
        }
        Set<RecordNode> nodes = accept.get(op);
        if (nodes == null) {
            nodes = new HashSet<RecordNode>();
            accept.put(op, nodes);
        }
        nodes.add(node);
    }

    protected RecordNode findEqualsForm(RecordNode q) {
        for (RecordNode r: nodes) {
            if ((r.oldForm.size() == q.oldForm.size())
                    && (r.nextForm.size() == q.nextForm.size())
                    && r.oldForm.containsAll(q.oldForm)
                    && r.nextForm.containsAll(q.nextForm)) {
                return r;
            }
        }
        return null;
    }

    protected class ExpandVisitor implements INodeVisitor<Void, RecordNode> {

        public Void visitBoolean(BooleanNode b, RecordNode node) {
            if (BooleanNode.TRUE.equals(b)) {
                RecordNode q = createChild(node);
                q.oldForm.add(b);
                expand(q);
            }
            return null;
        }

        public Void visitPredicate(Predicate p, RecordNode node) {
            UnaryOperator op = new UnaryOperator(UnaryOperatorType.NEG, p);
            if (!node.oldForm.contains(op)) {
                RecordNode q = createChild(node);
                q.oldForm.add(p);
                expand(q);
            }
            return null;
        }

        public Void visitNeg(UnaryOperator op, RecordNode node) {
            if (UnaryOperatorType.NEG != op.getType()) {
                throw new IllegalArgumentException();
            }
            if (!(op.getOperand() instanceof Predicate)) {
                throw new IllegalFormulaException("Formula isn't in negative normal form");
            }
            if (!node.oldForm.contains(op.getOperand())) {
                RecordNode q = createChild(node);
                q.oldForm.add(op);
                expand(q);
            }
            return null;
        }

        public Void visitUntil(BinaryOperator op, RecordNode node) {
            if (BinaryOperatorType.UNTIL != op.getType()) {
                throw new IllegalArgumentException();
            }
            RecordNode q1 = createChild(node);
            q1.oldForm.add(op);
            q1.newForm.add(op.getLeftOperand());
            q1.nextForm.add(op);

            RecordNode q2 = createChild(node);
            q2.oldForm.add(op);
            q2.newForm.add(op.getRightOperand());

            expand(q1);
            expand(q2);
            return null;
        }

        public Void visitRelease(BinaryOperator op, RecordNode node) {
            if (BinaryOperatorType.RELEASE != op.getType()) {
                throw new IllegalArgumentException();
            }
            RecordNode q1 = createChild(node);
            q1.oldForm.add(op);
            q1.newForm.add(op.getLeftOperand());
            q1.newForm.add(op.getRightOperand());

            RecordNode q2 = createChild(node);
            q2.oldForm.add(op);
            q2.newForm.add(op.getRightOperand());
            q2.nextForm.add(op);

            expand(q1);
            expand(q2);
            return null;
        }

        public Void visitOr(BinaryOperator op, RecordNode node) {
            if (BinaryOperatorType.OR != op.getType()) {
                throw new IllegalArgumentException();
            }
            RecordNode q1 = createChild(node);
            q1.oldForm.add(op);
            q1.newForm.add(op.getLeftOperand());

            RecordNode q2 = createChild(node);
            q2.oldForm.add(op);
            q2.newForm.add(op.getRightOperand());

            expand(q1);
            expand(q2);
            return null;
        }

        public Void visitAnd(BinaryOperator op, RecordNode node) {
            if (BinaryOperatorType.AND != op.getType()) {
                throw new IllegalArgumentException();
            }
            RecordNode q = createChild(node);
            q.oldForm.add(op);
            q.newForm.add(op.getLeftOperand());
            q.newForm.add(op.getRightOperand());
            expand(q);
            return null;
        }

        public Void visitNext(UnaryOperator op, RecordNode node) {
            if (UnaryOperatorType.NEXT != op.getType()) {
                throw new IllegalArgumentException();
            }
            RecordNode q = createChild(node);
            q.oldForm.add(op);
            q.nextForm.add(op.getOperand());
            expand(q);
            return null;
        }

        public Void visitFuture(UnaryOperator op, RecordNode node) {
            throw new NotImplementedException();
        }

        public Void visitGlobal(UnaryOperator op, RecordNode recordNode) {
            throw new NotImplementedException();
        }

        private RecordNode createChild(RecordNode node) {
            RecordNode child = new RecordNode();
            child.incoming.addAll(node.incoming);
            child.oldForm.addAll(node.oldForm);
            child.newForm.addAll(node.newForm);
            child.nextForm.addAll(node.nextForm);
            return child;
        }
    }
}

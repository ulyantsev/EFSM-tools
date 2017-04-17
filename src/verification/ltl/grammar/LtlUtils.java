/**
 * ${NAME}.java, 22.03.2008
 */
package verification.ltl.grammar;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class LtlUtils {
    private static LtlUtils instance;

    public synchronized static LtlUtils getInstance() {
        if (instance == null) {
            instance = new LtlUtils();
        }
        return instance;
    }

    INodeVisitor<LtlNode, Void> visitor;

    private LtlUtils() {
        visitor = new NegationVisitor();
    }

    public LtlNode neg(LtlNode root) {
        return new UnaryOperator(UnaryOperatorType.NEG, root);
    }

    private class NegationVisitor implements INodeVisitor<LtlNode, Void> {
        public LtlNode visitPredicate(Predicate p, Void aVoid) {
            throw new AssertionError();
        }

        public LtlNode visitNeg(UnaryOperator op, Void aVoid) {
            if (UnaryOperatorType.NEG != op.getType()) {
                throw new IllegalArgumentException();
            }
            return op.getOperand();
        }

        public LtlNode visitFuture(UnaryOperator op, Void aVoid) {
            throw new AssertionError();
        }

        public LtlNode visitNext(UnaryOperator op, Void aVoid) {
            if (UnaryOperatorType.NEXT != op.getType()) {
                throw new IllegalArgumentException();
            }
            op.setOperand(new UnaryOperator(UnaryOperatorType.NEG, op.getOperand()));
            return op;
        }

        public LtlNode visitAnd(BinaryOperator op, Void aVoid) {
            if (BinaryOperatorType.AND != op.getType()) {
                throw new IllegalArgumentException();
            }

            return visitBinaryOperator(op, BinaryOperatorType.OR);
        }

        public LtlNode visitOr(BinaryOperator op, Void aVoid) {
            if (BinaryOperatorType.OR != op.getType()) {
                throw new IllegalArgumentException();
            }

            return visitBinaryOperator(op, BinaryOperatorType.AND);
        }

        public LtlNode visitRelease(BinaryOperator op, Void aVoid) {
            if (BinaryOperatorType.RELEASE != op.getType()) {
                throw new IllegalArgumentException();
            }

            return visitBinaryOperator(op, BinaryOperatorType.UNTIL);
        }

        public LtlNode visitUntil(BinaryOperator op, Void aVoid) {
            if (BinaryOperatorType.UNTIL != op.getType()) {
                throw new IllegalArgumentException();
            }

            return visitBinaryOperator(op, BinaryOperatorType.RELEASE);
        }

        public LtlNode visitGlobal(UnaryOperator op, Void aVoid) {
            throw new AssertionError();
        }

        public LtlNode visitBoolean(BooleanNode b, Void aVoid) {
            return BooleanNode.TRUE.equals(b) ? BooleanNode.FALSE : BooleanNode.TRUE;
        }

        protected LtlNode visitBinaryOperator(BinaryOperator op, BinaryOperatorType newType) {
            BinaryOperator newOp = new BinaryOperator(newType);
            newOp.setLeftOperand(new UnaryOperator(UnaryOperatorType.NEG, op.getLeftOperand()));
            newOp.setRightOperand(new UnaryOperator(UnaryOperatorType.NEG, op.getRightOperand()));
            return newOp;
        }
    }

    // expands things like event(A0, A1, A2) to (event(A0) || event(A1) || event(A2))
    public static String expandEventList(String input) {
        final Pattern p = Pattern.compile("event\\(((\\w+, *)+\\w+)\\)");
        final StringBuilder sb = new StringBuilder();
        final Matcher m = p.matcher(input);
        int lastPos = 0;
        while (m.find()) {
            final String events = m.group(1);
            sb.append(input.substring(lastPos, m.start()));
            final List<String> tokens = Arrays.stream(events.split(", *"))
                    .map(s -> "event(" + s + ")").collect(Collectors.toList());
            sb.append("(" + String.join(" || ", tokens) + ")");
            lastPos = m.end();
        }
        sb.append(input.substring(lastPos, input.length()));
        return sb.toString();
    }
}

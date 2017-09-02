package bnf_formulae;

/**
 * (c) Igor Buzhinsky
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BinaryOperation extends BooleanFormula {
    private final List<BooleanFormula> children;
    public final BinaryOperations type;
    private final String comment;

    private BinaryOperation(BooleanFormula left, BooleanFormula right, BinaryOperations type) {
        this(Arrays.asList(left, right), type, null);
    }
    
    private BinaryOperation(List<BooleanFormula> children, BinaryOperations type) {
        this(children, type, null);
    }
    
    private BinaryOperation(List<BooleanFormula> children, BinaryOperations type, String comment) {
        for (BooleanFormula f : children) {
            assert f != null;
        }
        if (type == BinaryOperations.EQ || type == BinaryOperations.IMPLIES) {
            assert children.size() == 2;
        }
        this.children = new ArrayList<>(children);
        this.type = type;
        this.comment = comment;
    }
    
    public static BooleanFormula and(List<BooleanFormula> elements) {
        return and(elements, null);
    }
    
    public static BooleanFormula or(List<BooleanFormula> elements) {
        return or(elements, null);
    }
    
    public static BooleanFormula and(List<BooleanFormula> elements, String comment) {
        return new BinaryOperation(elements, BinaryOperations.AND, comment);
    }
    
    public static BooleanFormula or(List<BooleanFormula> elements, String comment) {
        return new BinaryOperation(elements, BinaryOperations.OR, comment);
    }
    
    public static BooleanFormula and(BooleanFormula... elements) {
        return new BinaryOperation(Arrays.asList(elements), BinaryOperations.AND);
    }
    
    public static BooleanFormula or(BooleanFormula... elements) {
        return new BinaryOperation(Arrays.asList(elements), BinaryOperations.OR);
    }
    
    public static BooleanFormula implies(BooleanFormula left, BooleanFormula right) {
        return new BinaryOperation(left, right, BinaryOperations.IMPLIES);
    }
    
    public static BooleanFormula equivalent(BooleanFormula left, BooleanFormula right) {
        return new BinaryOperation(left, right, BinaryOperations.EQ);
    }
    
    @Override
    public String toLimbooleString() {
        assert !children.isEmpty();
        if (children.size() == 1) {
            return children.get(0).toLimbooleString();
        }
        
        final List<String> strChildren = children.stream()
                .map(BooleanFormula::toLimbooleString)
                .collect(Collectors.toList());
        
        // limboole does not like '->' without the left space
        return "(" + String.join((type == BinaryOperations.IMPLIES ? " " : "")
                + type, strChildren) + ")";
    }
    
    private String commentStringStart() {
        return comment == null ? "" : ("\n/* " + comment + " */\n");
    }
    
    private String commentStringEnd() {
        return comment == null ? "" : ("\n/* " + "end of block" + " */\n");
    }
    
    private String comment(String content) {
        return commentStringStart() + content + commentStringEnd();
    }
    
    @Override
    public String toString() {
        if (children.isEmpty()) {
            switch (type) {
            case AND:
                return comment(BooleanFormula.TRUE.toString());
            case OR:
                return comment(BooleanFormula.FALSE.toString());
            default:
                throw new AssertionError();
            }
        } else if (children.size() == 1) {
            return comment(children.get(0).toString());
        }
        
        final List<String> strChildren = children.stream().map(Object::toString).collect(Collectors.toList());
        return "(" + comment(String.join(" " + type + " ", strChildren)) + ")";
    }

    @Override
    public BooleanFormula multipleSubstitute(Map<BooleanVariable, BooleanFormula> replacement) {
        List<BooleanFormula> newChildren = children.stream().map(c ->
            c.multipleSubstitute(replacement)).collect(Collectors.toList());
        return new BinaryOperation(newChildren, type);
    }
    
    @Override
    public BooleanFormula simplify() {
        final List<BooleanFormula> childrenSimpl = children.stream()
                .map(BooleanFormula::simplify).collect(Collectors.toList());
        BooleanFormula left;
        BooleanFormula right;
        switch (type) {
        case AND:
            if (childrenSimpl.contains(BooleanFormula.FALSE)) {
                return BooleanFormula.FALSE;
            }
            childrenSimpl.removeIf(elem -> elem == BooleanFormula.TRUE);
            return childrenSimpl.isEmpty() ? BooleanFormula.TRUE
                : childrenSimpl.size() == 1 ? childrenSimpl.get(0) : and(childrenSimpl);
        case OR:
            if (childrenSimpl.contains(BooleanFormula.TRUE)) {
                return BooleanFormula.TRUE;
            }
            childrenSimpl.removeIf(elem -> elem == BooleanFormula.FALSE);
            return childrenSimpl.isEmpty() ? BooleanFormula.FALSE
                    : childrenSimpl.size() == 1 ? childrenSimpl.get(0) : or(childrenSimpl);
        case EQ:
            left = childrenSimpl.get(0);
            right = childrenSimpl.get(1);
            if (left == right) { // both TRUE or both FALSE
                return BooleanFormula.TRUE;
            } else if (left == BooleanFormula.TRUE && right == BooleanFormula.FALSE) {
                return BooleanFormula.FALSE;
            } else if (left == BooleanFormula.FALSE && right == BooleanFormula.TRUE) {
                return BooleanFormula.FALSE;
            } else if (left == BooleanFormula.TRUE) {
                return right;
            } else if (right == BooleanFormula.TRUE) {
                return left;
            } else if (left == BooleanFormula.FALSE) {
                return right.not();
            } else if (right == BooleanFormula.FALSE) {
                return left.not();
            }
            return new BinaryOperation(childrenSimpl, BinaryOperations.EQ);
        case IMPLIES:
            left = childrenSimpl.get(0);
            right = childrenSimpl.get(1);
            if (left == BooleanFormula.FALSE || right == BooleanFormula.TRUE) {
                return BooleanFormula.TRUE;
            } else if (left == BooleanFormula.TRUE && right == BooleanFormula.FALSE) {
                return BooleanFormula.FALSE;
            } else if (left == BooleanFormula.TRUE) {
                return right;
            } else if (right == BooleanFormula.FALSE) {
                return left.not();
            }
            return new BinaryOperation(childrenSimpl, BinaryOperations.IMPLIES);
        }
        throw new AssertionError();
    }
}

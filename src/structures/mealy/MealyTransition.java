package structures.mealy;

import scenario.StringActions;
import bool.MyBooleanExpression;

public class MealyTransition {
    private final String event;
    private final MyBooleanExpression expr;
    private final StringActions actions;
    private final MealyNode src;
    private final MealyNode dst;

    public MealyTransition(MealyNode src, MealyNode dst, String event, MyBooleanExpression expr,
                           StringActions actions) {
        this.src = src;
        this.dst = dst;
        this.event = event;
        this.expr = expr;
        this.actions = actions;
    }

    public String event() {
        return event;
    }

    public MealyNode src() {
        return src;
    }

    public MealyNode dst() {
        return dst;
    }

    public StringActions actions() {
        return actions;
    }

    public MyBooleanExpression expr() {
        return expr;
    }
    
    @Override
    public String toString() {
        return src.number() + " -> " + dst.number()
                + "  " + event + " [" + expr.toString() + "] " + actions.toString();
    }
}

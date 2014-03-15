package structures;

import actions.StringActions;
import bool.MyBooleanExpression;

public class Transition {
	public static boolean isCopatibility(Transition first, Transition second) {
		if (first.getEvent().equals(second.getEvent())) {
			if (first.getExpr() == second.getExpr()) {
				if (!first.getActions().equals(second.getActions())) {
					return false;
				}
			} else if (first.getExpr().hasSolutionWith(second.getExpr())) {
				return false;
			}
		}
		return true;
	}

	private String event;

	private MyBooleanExpression expr;

	private StringActions actions;

	private Node src;

	private Node dst;

	public Transition(Node src, Node dst, String event, MyBooleanExpression expr, StringActions actions) {
		this.src = src;
		this.dst = dst;
		this.event = event;
		this.expr = expr;
		this.actions = actions;
	}

	public String getEvent() {
		return event;
	}

	public Node getSrc() {
		return src;
	}

	public Node getDst() {
		return dst;
	}

	public StringActions getActions() {
		return actions;
	}

	public MyBooleanExpression getExpr() {
		return expr;
	}
	
	public String toString() {
		String res = src.getNumber() + " -> " + dst.getNumber();
		res += "  " + event + " [" + expr.toString() + "] " + actions.toString();
		return res;
	}
}

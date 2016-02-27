/**
 * DfsTransition.java, 12.04.2008
 */
package verification.verifier;

import verification.statemachine.StateTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionTransition {
    public final IntersectionNode target;
    public final StateTransition transition;

    public IntersectionTransition(StateTransition transition, IntersectionNode target) {
        if (target == null) {
            throw new IllegalArgumentException("Target can't be null");
        }
        this.target = target;
        this.transition = transition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final IntersectionTransition that = (IntersectionTransition) o;

        if (!target.equals(that.target)) return false;
        if (transition != null ? !transition.equals(that.transition) : that.transition != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = target.hashCode();
        result = 31 * result + (transition != null ? transition.hashCode() : 0);
        return result;
    }
    
    @Override
    public String toString() {
    	return "[" + (transition == null ? "NULL" : transition.event
    			+ "" + transition.getActions()) + "]";
    }
}

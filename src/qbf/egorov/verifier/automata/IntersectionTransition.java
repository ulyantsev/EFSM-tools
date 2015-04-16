/**
 * DfsTransition.java, 12.04.2008
 */
package qbf.egorov.verifier.automata;

import qbf.egorov.statemachine.IState;
import qbf.egorov.statemachine.IStateTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionTransition<S extends IState> {
    private IntersectionNode<S> target;
    private IStateTransition transition;

    public IntersectionTransition(IStateTransition trans, IntersectionNode<S> target) {
        if (target == null) {
            throw new IllegalArgumentException("Target can't be null");
        }
        this.target = target;
        this.transition = trans;
    }

    public IntersectionNode<S> getTarget() {
        return target;
    }

    public IStateTransition getTransition() {
        return transition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntersectionTransition<?> that = (IntersectionTransition<?>) o;

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
    	return "["
    			+ (transition == null ? "NULL" : transition.getEvent()
    			+ "" + transition.getActions()) + "]";
    }
}

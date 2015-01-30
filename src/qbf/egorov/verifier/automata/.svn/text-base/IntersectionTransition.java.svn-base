/**
 * DfsTransition.java, 12.04.2008
 */
package ru.ifmo.verifier.automata;

import ru.ifmo.automata.statemachine.IStateTransition;
import ru.ifmo.automata.statemachine.IState;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionTransition<S extends IState> implements IIntersectionTransition<S> {

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

        IntersectionTransition that = (IntersectionTransition) o;

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
}

/**
 * ITransitionCondition.java, 16.03.2008
 */
package qbf.egorov.ltl.buchi;

import qbf.egorov.ltl.grammar.IExpression;
import qbf.egorov.ltl.grammar.Predicate;

import java.util.Set;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface ITransitionCondition {

    @Deprecated
    public Set<IExpression<Boolean>> getExpressions();

    @Deprecated
    public Set<IExpression<Boolean>> getNegExpressions();

    public boolean getValue();
}

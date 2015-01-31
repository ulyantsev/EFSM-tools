/**
 * ITransitionCondition.java, 16.03.2008
 */
package qbf.egorov.ltl.buchi;

import java.util.Set;

import qbf.egorov.ltl.grammar.IExpression;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface ITransitionCondition {
    public Set<IExpression<Boolean>> getExpressions();
    public Set<IExpression<Boolean>> getNegExpressions();
    public boolean getValue();
}

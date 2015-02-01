/**
 * IVerifier.java, 06.04.2008
 */
package qbf.egorov.verifier;

import java.util.List;

import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;
import qbf.egorov.verifier.automata.IIntersectionTransition;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IVerifier<S extends IState> {
    List<IIntersectionTransition> verify(IBuchiAutomata buchi,
                                         IPredicateFactory<S> predicates,
                                         IDfsListener... listeners);
}

/**
 * IVerifier.java, 06.04.2008
 */
package ru.ifmo.verifier;

import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.ltl.LtlParseException;
import ru.ifmo.ltl.grammar.predicate.IPredicateFactory;
import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.verifier.automata.IIntersectionTransition;

import java.util.List;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public interface IVerifier<S extends IState> {

    List<IIntersectionTransition> verify(IBuchiAutomata buchi,
                                         IPredicateFactory<S> predicates,
                                         IDfsListener... listeners);

    List<IIntersectionTransition> verify(String ltlFormula,
                                         IPredicateFactory<S> predicates,
                                         IDfsListener... listeners) throws LtlParseException;
}

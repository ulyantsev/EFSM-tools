package ru.ifmo.verifier.automata;

import ru.ifmo.automata.statemachine.IState;
import ru.ifmo.ltl.buchi.IBuchiNode;
import ru.ifmo.ltl.buchi.IBuchiAutomata;
import ru.ifmo.ltl.grammar.predicate.IPredicateFactory;

public interface IIntersectionAutomata<S extends IState> {

    IBuchiAutomata getBuchiAutomata();

    IPredicateFactory<S> getPredicates();

    IntersectionNode<S> getNode(S state, IBuchiNode node, int acceptSet);
}

package qbf.egorov.verifier.automata;

import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.IBuchiNode;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;

public interface IIntersectionAutomata<S extends IState> {
    IBuchiAutomata getBuchiAutomata();
    IPredicateFactory<S> getPredicates();
    IntersectionNode<S> getNode(S state, IBuchiNode node, int acceptSet);
}

/**
 * IntersectionAutomata.java, 12.04.2008
 */
package qbf.egorov.verifier.automata;

import qbf.egorov.ltl.buchi.BuchiAutomata;
import qbf.egorov.ltl.buchi.BuchiNode;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionAutomata<S extends IState> {
    private final IPredicateFactory<S> predicates;
    private final BuchiAutomata buchiAutomata;
    private final Map<String, IntersectionNode<S>> nodeMap = new HashMap<>();

    public IntersectionAutomata(IPredicateFactory<S> predicates, BuchiAutomata buchi) {
        this.predicates = predicates;
        buchiAutomata = buchi;
    }

    public BuchiAutomata getBuchiAutomata() {
        return buchiAutomata;
    }

    public IntersectionNode<S> getNode(S state, BuchiNode node, int acceptSet) {
        String key = getUniqueKey(state, node, acceptSet);

        IntersectionNode<S> res = nodeMap.get(key);
        if (res == null) {
            res = new IntersectionNode<>(this, state, node, acceptSet);
            nodeMap.put(key, res);
        }
        return res;
    }

    public IPredicateFactory<S> getPredicates() {
        return predicates;
    }

    protected String getUniqueKey(IState state, BuchiNode node, int acceptSet) {
        return state.getUniqueName() + "_" + node.getID() + "_" + acceptSet;
    }
}

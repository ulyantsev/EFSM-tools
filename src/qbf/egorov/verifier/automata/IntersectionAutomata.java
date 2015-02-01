/**
 * IntersectionAutomata.java, 12.04.2008
 */
package qbf.egorov.verifier.automata;

import qbf.egorov.ltl.buchi.IBuchiAutomata;
import qbf.egorov.ltl.buchi.IBuchiNode;
import qbf.egorov.ltl.grammar.predicate.IPredicateFactory;
import qbf.egorov.statemachine.IState;

import java.util.*;

/**
 * TODO: add comment
 *
 * @author Kirill Egorov
 */
public class IntersectionAutomata<S extends IState> implements IIntersectionAutomata<S> {
    private IPredicateFactory<S> predicates;
    private IBuchiAutomata buchiAutomata;
    private Map<String, IntersectionNode<S>> nodeMap = new HashMap<>();

    public IntersectionAutomata(IPredicateFactory<S> predicates, IBuchiAutomata buchi) {
        if (buchi == null || predicates == null) {
            throw new IllegalArgumentException();
        }
        this.predicates = predicates;
        this.buchiAutomata = buchi;
    }

    public IBuchiAutomata getBuchiAutomata() {
        return buchiAutomata;
    }

    public IntersectionNode<S> getNode(S state, IBuchiNode node, int acceptSet) {
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

    protected String getUniqueKey(IState state, IBuchiNode node, int acceptSet) {
        return state.getUniqueName() + "_" + node.getID() + "_" + acceptSet;
    }
}
